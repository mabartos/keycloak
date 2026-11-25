import type {
  AgentConnection,
  AgentDirectoryRepresentation,
  AgentRegistration,
  AgentServer,
  ConnectionMode,
} from "@keycloak/keycloak-admin-client/lib/defs/agentDirectoryRepresentation";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { label, useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  Alert,
  AlertVariant,
  Button,
  Checkbox,
  Form,
  FormGroup,
  FormSelect,
  FormSelectOption,
  Modal,
  ModalVariant,
  TextInput,
} from "@patternfly/react-core";
import { LongArrowAltRightIcon } from "@patternfly/react-icons";
import { FormEvent, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { ClientSelectModal } from "../components/client/ClientSelectModal";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import {
  clientResourcePrefix,
  ConnectionEndpoint,
  ConnectionEndpointPicker,
  type EndpointSide,
} from "./ConnectionEndpoint";
import { ConnectionModeCards } from "./ConnectionModeCards";
import { ScopesSelect } from "./ScopesSelect";
import { discoverServer } from "./discovery";

export type Editor =
  | {
      kind: "agents";
      entry?: AgentRegistration;
    }
  | { kind: "servers"; entry?: AgentServer }
  | {
      kind: "connections";
      entry?: AgentConnection;
      caller?: string;
      resource?: string;
    };

export const titles = {
  agents: "registerAgent",
  servers: "registerMcpServer",
  connections: "allowAgentConnection",
} as const;

export function DirectoryEditor({
  editor,
  directory,
  onClose,
  onSaved,
}: {
  editor: Editor;
  directory: AgentDirectoryRepresentation;
  onClose: () => void;
  onSaved: () => void;
}) {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { hasAccess } = useAccess();
  const enabled = useIsFeatureEnabled();
  const canManageDirectory = hasAccess("manage-realm");
  const agent = editor.kind === "agents" ? editor.entry : undefined;
  const server = editor.kind === "servers" ? editor.entry : undefined;
  const connection = editor.kind === "connections" ? editor.entry : undefined;
  const [name, setName] = useState(agent?.name ?? server?.name ?? "");
  const [clientId, setClientId] = useState(
    agent?.clientId ?? server?.clientId ?? "",
  );
  const [registration, setRegistration] = useState(
    agent?.cimd
      ? "cimd"
      : agent || !hasAccess("manage-clients")
        ? "local"
        : "new",
  );
  const cimd = editor.kind === "agents" && registration === "cimd";
  const newClient = editor.kind === "agents" && registration === "new";
  const [redirectUri, setRedirectUri] = useState("");
  const [loopback, setLoopback] = useState(agent?.loopback ?? false);
  const [external, setExternal] = useState(!!server?.issuer);
  const [issuer, setIssuer] = useState(server?.issuer ?? "");
  const [issuers, setIssuers] = useState<string[]>([]);
  const [resource, setResource] = useState(
    server?.resource ??
      connection?.resource ??
      (editor.kind === "connections" ? editor.resource : "") ??
      "",
  );
  const [caller, setCaller] = useState(
    connection?.caller ??
      (editor.kind === "connections" ? editor.caller : "") ??
      "",
  );
  const [mode, setMode] = useState<ConnectionMode>(
    connection?.mode ?? "interactive",
  );
  const [scopes, setScopes] = useState(connection?.scopes ?? []);
  const [advertisedScopes, setAdvertisedScopes] = useState(
    server?.supportedScopes ?? [],
  );
  const [discoveredScopes, setDiscoveredScopes] = useState<string[]>([]);
  const [role, setRole] = useState(connection?.requiredRealmRole ?? "");
  const [downstream, setDownstream] = useState(
    connection?.downstreamClientId ?? "",
  );
  const [chooseClient, setChooseClient] = useState<
    "registration" | EndpointSide
  >();
  const [realmClients, setRealmClients] = useState<
    Partial<Record<EndpointSide, ClientRepresentation>>
  >({});
  const returnFocus = useRef<EndpointSide>();
  const [busy, setBusy] = useState(false);
  const [discovering, setDiscovering] = useState(false);
  const [metadataStatus, setMetadataStatus] = useState<"success" | "warning">();
  const discovery = useRef<AbortController>();
  useEffect(() => () => discovery.current?.abort(), []);
  const target = directory.servers.find((s) => s.resource === resource);
  const realmTarget = resource.startsWith(clientResourcePrefix);
  const actualMode = target?.issuer
    ? "enterprise"
    : realmTarget && mode === "enterprise"
      ? "interactive"
      : mode;
  const editing = !!editor.entry;

  const callerUuid =
    directory.agents.find((a) => a.clientId === caller)?.clientUuid ??
    realmClients.caller?.id;
  const [loadingDefaults, setLoadingDefaults] = useState(false);
  const includeDefaults = async () => {
    setLoadingDefaults(true);
    try {
      const id =
        callerUuid ??
        (await adminClient.clients.find({ realm, clientId: caller }))[0]?.id;
      if (!id) return;
      const defaults = await adminClient.clients.listDefaultClientScopes({
        realm,
        id,
      });
      const included = defaults
        .filter((s) => s.attributes?.["include.in.token.scope"] !== "false")
        .flatMap((s) => (s.name ? [s.name] : []));
      setScopes((current) => [...new Set([...current, ...included])]);
    } catch (error) {
      addError("agentDefaultScopeError", error);
    } finally {
      setLoadingDefaults(false);
    }
  };

  const loadMetadata = async () => {
    discovery.current?.abort();
    const controller = new AbortController();
    discovery.current = controller;
    setDiscovering(true);
    setMetadataStatus(undefined);
    try {
      const metadata = await discoverServer(resource, controller.signal);
      if (controller.signal.aborted) return;
      if (!name) setName(metadata.name);
      setIssuers(metadata.issuers);
      setDiscoveredScopes(metadata.scopes);
      setAdvertisedScopes((current) => [
        ...new Set([...current, ...metadata.scopes]),
      ]);
      setMetadataStatus("success");
    } catch {
      if (!controller.signal.aborted) setMetadataStatus("warning");
    } finally {
      if (!controller.signal.aborted) setDiscovering(false);
    }
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    try {
      if (newClient) {
        await adminClient.clients.create({
          realm,
          clientId: clientId.trim(),
          protocol: "openid-connect",
          publicClient: true,
          standardFlowEnabled: true,
          implicitFlowEnabled: false,
          directAccessGrantsEnabled: false,
          serviceAccountsEnabled: false,
          redirectUris: [redirectUri.trim()],
          attributes: {
            "ai.agent": "true",
            "pkce.code.challenge.method": "S256",
          },
        });
      } else if (editor.kind === "agents") {
        await adminClient.aiAgents.saveAgent({
          realm,
          name: cimd ? name : clientId,
          clientId,
          cimd,
          loopback: cimd && loopback,
        });
      } else if (editor.kind === "servers") {
        await adminClient.aiAgents.saveServer({
          realm,
          name,
          resource,
          clientId: (!external && clientId) || undefined,
          issuer: external ? issuer : undefined,
          supportedScopes: advertisedScopes,
        });
      } else {
        await adminClient.aiAgents.saveConnection({
          realm,
          caller,
          resource,
          mode: actualMode,
          scopes,
          requiredRealmRole: role || undefined,
          downstreamClientId: target?.issuer ? downstream : undefined,
        });
      }
      addAlert(
        t(newClient ? "createClientSuccess" : "agentDirectorySaved"),
        AlertVariant.success,
      );
      onSaved();
    } catch (error) {
      addError(
        newClient ? "createClientError" : "agentDirectorySaveError",
        error,
      );
    } finally {
      setBusy(false);
    }
  };

  const textField = (
    id: string,
    label: string,
    value: string,
    change: (v: string) => void,
    options: {
      required?: boolean;
      disabled?: boolean;
      url?: boolean;
      hint?: string;
      pattern?: string;
    } = {},
  ) => (
    <FormGroup fieldId={id} label={t(label)} isRequired={options.required}>
      <TextInput
        id={id}
        name={id}
        value={value}
        type={options.url ? "url" : "text"}
        pattern={options.pattern}
        onChange={(_, v) => change(v)}
        isRequired={options.required}
        isDisabled={options.disabled}
        aria-describedby={options.hint ? id + "-help" : undefined}
      />
      {options.hint && <small id={id + "-help"}>{t(options.hint)}</small>}
    </FormGroup>
  );
  const clientField = (
    <>
      {textField(
        "agent-client-id",
        cimd ? "agentCimdUrl" : "clientId",
        clientId,
        setClientId,
        {
          required: editor.kind === "agents",
          disabled: editing,
          url: cimd,
          hint: cimd ? "agentCimdHelp" : undefined,
        },
      )}
      {!newClient && !cimd && !editing && hasAccess("query-clients") && (
        <Button
          variant="link"
          isInline
          onClick={() => setChooseClient("registration")}
        >
          {t("selectClient")}
        </Button>
      )}
    </>
  );
  const endpointField = (side: EndpointSide) => (
    <ConnectionEndpoint
      side={side}
      directory={directory}
      value={side === "caller" ? caller : resource}
      client={realmClients[side]}
      disabled={editing || loadingDefaults}
      onBrowse={() => {
        returnFocus.current = side;
        setChooseClient(side);
      }}
    />
  );
  if (chooseClient === "caller" || chooseClient === "target") {
    return (
      <ConnectionEndpointPicker
        side={chooseClient}
        value={chooseClient === "caller" ? caller : resource}
        directory={directory}
        onClose={() => setChooseClient(undefined)}
        onSelect={(value, client) => {
          if (chooseClient === "caller") setCaller(value);
          else setResource(value);
          setRealmClients((current) => ({
            ...current,
            [chooseClient]: client,
          }));
          setChooseClient(undefined);
        }}
      />
    );
  }
  if (chooseClient === "registration") {
    return (
      <ClientSelectModal
        isRadio
        isRowDisabled={(c) => !c.enabled || c.protocol !== "openid-connect"}
        onClose={() => setChooseClient(undefined)}
        onSelect={([c]) => {
          if (!c.clientId) return;
          setClientId(c.clientId);
          if (!name) setName((c.name && label(t, c.name)) || c.clientId);
        }}
      />
    );
  }
  return (
    <Modal
      isOpen
      elementToFocus={returnFocus.current && "#" + returnFocus.current}
      variant={
        editor.kind === "connections" ? ModalVariant.large : ModalVariant.medium
      }
      title={t(editing ? "editAgentEntry" : titles[editor.kind])}
      onClose={busy ? undefined : onClose}
      onEscapePress={(event) => {
        if (!busy && !event.defaultPrevented) onClose();
      }}
      actions={[
        <Button
          key="save"
          type="submit"
          form="agent-directory-form"
          isLoading={busy}
          isDisabled={
            busy ||
            discovering ||
            loadingDefaults ||
            (newClient && (!clientId.trim() || !redirectUri.trim())) ||
            (editor.kind === "connections" && (!caller || !resource))
          }
        >
          {t(newClient ? "createClient" : "save")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onClose} isDisabled={busy}>
          {t("cancel")}
        </Button>,
      ]}
    >
      <Form id="agent-directory-form" onSubmit={submit}>
        {editor.kind === "servers" &&
          textField("agent-name", "name", name, setName, { required: true })}
        {editor.kind === "agents" && (
          <>
            <FormGroup
              label={t("agentRegistrationMethod")}
              fieldId="registration"
            >
              <FormSelect
                id="registration"
                value={registration}
                isDisabled={editing || !canManageDirectory || busy}
                onChange={(_, v) => {
                  setRegistration(v);
                  setClientId("");
                  setName("");
                  setRedirectUri("");
                }}
              >
                {hasAccess("manage-clients") && (
                  <FormSelectOption
                    value="new"
                    label={t("agentCreateOidcClient")}
                  />
                )}
                {canManageDirectory && (
                  <FormSelectOption value="local" label={t("existingClient")} />
                )}
                {canManageDirectory && (
                  <FormSelectOption
                    value="cimd"
                    label={t("agentCimdDocument")}
                  />
                )}
              </FormSelect>
              <small>
                {t(
                  newClient
                    ? "agentCreateOidcHelp"
                    : cimd
                      ? "agentCimdRegistrationHelp"
                      : "agentExistingClientHelp",
                )}
              </small>
              {cimd && !enabled(Feature.Cimd) && (
                <p>
                  <small>{t("agentCimdEnableForSignIn")}</small>
                </p>
              )}
            </FormGroup>
            {cimd &&
              textField("agent-name", "name", name, setName, {
                required: true,
              })}
            {clientField}
            {newClient &&
              textField(
                "agent-redirect-uri",
                "redirectURI",
                redirectUri,
                setRedirectUri,
                {
                  required: true,
                  url: true,
                  pattern: "[^*#]+",
                  hint: "agentRedirectUriHelp",
                },
              )}
            {cimd && (
              <Checkbox
                id="loopback"
                label={t("agentLoopback")}
                isChecked={loopback}
                onChange={(_, v) => setLoopback(v)}
              />
            )}
          </>
        )}
        {editor.kind === "servers" && (
          <>
            {textField(
              "resource",
              "agentResourceUrl",
              resource,
              (v) => {
                discovery.current?.abort();
                setDiscovering(false);
                setMetadataStatus(undefined);
                setIssuers([]);
                setDiscoveredScopes([]);
                setResource(v);
              },
              { required: true, disabled: editing, url: true },
            )}
            {!editing && (
              <Button
                variant="secondary"
                isLoading={discovering}
                isDisabled={!resource || discovering}
                onClick={loadMetadata}
              >
                {t("discoverMcpMetadata")}
              </Button>
            )}
            {metadataStatus && (
              <Alert
                variant={metadataStatus}
                isInline
                title={t(
                  metadataStatus === "success"
                    ? "agentMetadataLoaded"
                    : "agentMetadataUnavailable",
                )}
              />
            )}
            <FormGroup
              label={t("agentAuthorizationServer")}
              fieldId="authority"
            >
              <FormSelect
                id="authority"
                value={external ? "external" : "local"}
                isDisabled={editing}
                onChange={(_, v) => setExternal(v === "external")}
              >
                <FormSelectOption value="local" label={t("agentThisRealm")} />
                <FormSelectOption
                  value="external"
                  isDisabled={!enabled(Feature.IdentityAssertionJwt)}
                  label={t("agentExternalIssuer")}
                />
              </FormSelect>
            </FormGroup>
            {external ? (
              <>
                {textField("issuer", "agentIssuerUrl", issuer, setIssuer, {
                  required: true,
                  disabled: editing,
                  url: true,
                  hint: "agentIssuerHelp",
                })}
                {!editing &&
                  issuers.map((v) => (
                    <Button
                      key={v}
                      variant="link"
                      isInline
                      onClick={() => setIssuer(v)}
                    >
                      {v}
                    </Button>
                  ))}
              </>
            ) : (
              clientField
            )}
            <ScopesSelect
              field="advertised"
              value={advertisedScopes}
              suggestions={discoveredScopes}
              onChange={setAdvertisedScopes}
            />
          </>
        )}
        {editor.kind === "connections" && (
          <>
            <div className="kc-connection-endpoints">
              {endpointField("caller")}
              <LongArrowAltRightIcon
                className="kc-connection-direction"
                aria-hidden
              />
              {endpointField("target")}
            </div>
            <ConnectionModeCards
              value={actualMode}
              onChange={setMode}
              editing={editing}
              clientTarget={realmTarget}
              externalIssuer={!!target?.issuer}
            />
            {target?.issuer &&
              textField(
                "downstream",
                "agentDownstreamId",
                downstream,
                setDownstream,
                { required: true },
              )}
            <ScopesSelect
              field="allowed"
              value={scopes}
              suggestions={target?.supportedScopes ?? []}
              onChange={setScopes}
            />
            {caller &&
              (callerUuid ||
                !directory.agents.some(
                  (a) => a.cimd && a.clientId === caller,
                )) &&
              hasAccess("view-clients") && (
                <Button
                  variant="link"
                  isInline
                  isLoading={loadingDefaults}
                  isDisabled={loadingDefaults}
                  onClick={includeDefaults}
                >
                  {t("agentIncludeDefaultScopes")}
                </Button>
              )}
            {textField("required-role", "agentRequiredRole", role, setRole, {
              hint: "agentRequiredRoleHelp",
            })}
          </>
        )}
      </Form>
    </Modal>
  );
}
