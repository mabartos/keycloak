import type { AgentDirectoryRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/agentDirectoryRepresentation";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  KeycloakDataTable,
  label,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  FormGroup,
  Modal,
  ModalVariant,
  Tab,
  Tabs,
  TabTitleText,
} from "@patternfly/react-core";
import {
  RobotIcon,
  SearchIcon,
  ServerIcon,
  WindowMaximizeIcon,
} from "@patternfly/react-icons";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { ClientSelectTable } from "../components/client/ClientSelectModal";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";

export const clientResourcePrefix = "urn:client:";
export type EndpointSide = "caller" | "target";

const endpointIcons = {
  agentKindAgent: RobotIcon,
  agentKindCimd: RobotIcon,
  agentKindServer: ServerIcon,
  agentKindClient: WindowMaximizeIcon,
};

type EndpointProps = {
  side: EndpointSide;
  directory: AgentDirectoryRepresentation;
  value: string;
};

export function ConnectionEndpoint({
  side,
  directory,
  value,
  client,
  disabled,
  onBrowse,
}: EndpointProps & {
  client?: ClientRepresentation;
  disabled: boolean;
  onBrowse: () => void;
}) {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { hasAccess } = useAccess();
  const caller = side === "caller";
  const realmTarget = !caller && value.startsWith(clientResourcePrefix);
  const identifier = realmTarget
    ? value.slice(clientResourcePrefix.length)
    : value;
  const agent = caller
    ? directory.agents.find((a) => a.clientId === value)
    : undefined;
  const server = directory.servers.find((s) =>
    caller ? s.clientId === value : s.resource === value,
  );
  const canQuery = hasAccess("query-clients");
  const [resolved, setResolved] = useState<ClientRepresentation>();
  const lookupId =
    canQuery && !client && !agent && (caller || realmTarget) ? identifier : "";
  useFetch(
    async () => {
      if (!lookupId) return;
      // The client ID remains available if the display name cannot be read.
      const clients = await adminClient.clients
        .find({ realm, clientId: lookupId })
        .catch(() => []);
      return clients.find((c) => c.clientId === lookupId);
    },
    setResolved,
    [realm, lookupId],
  );
  const realmClient =
    client ?? (resolved?.clientId === identifier ? resolved : undefined);
  const name =
    !caller && server
      ? server.name
      : label(t, agent?.name ?? realmClient?.name) || identifier;
  const kind = agent
    ? agent.cimd
      ? "agentKindCimd"
      : "agentKindAgent"
    : server
      ? "agentKindServer"
      : "agentKindClient";
  const Icon = value ? endpointIcons[kind] : SearchIcon;
  const browse = t(caller ? "agentBrowseCaller" : "agentBrowseDestination");
  const available =
    canQuery ||
    (caller ? directory.agents.length > 0 : directory.servers.length > 0);
  const content = (
    <>
      <span className="kc-endpoint-icon">
        <Icon aria-hidden />
      </span>
      <span className="kc-endpoint-name">
        {name ||
          (available
            ? browse
            : t(caller ? "agentEmptyAgents" : "agentEmptyServers"))}
      </span>
      {value && (
        <small id={side + "-kind"} className="kc-endpoint-kind">
          {t(kind)}
          {name !== identifier && <> · {identifier}</>}
        </small>
      )}
    </>
  );
  return (
    <FormGroup
      fieldId={side}
      label={t(caller ? "agentCaller" : "agentDestination")}
      isRequired
    >
      {disabled || !available ? (
        <div id={side} className="kc-endpoint-choice">
          {content}
        </div>
      ) : (
        <Button
          id={side}
          variant="link"
          className="kc-endpoint-choice"
          aria-haspopup="dialog"
          aria-label={
            name ? t("agentChangeEndpoint", { action: browse, name }) : browse
          }
          aria-describedby={
            value ? side + "-kind " + side + "-help" : side + "-help"
          }
          onClick={onBrowse}
        >
          {content}
        </Button>
      )}
      <small id={side + "-help"} className="kc-endpoint-help">
        {realmTarget
          ? t("agentClientAudienceHelp", {
              resource: value,
              client: identifier,
            })
          : t(caller ? "agentCallerHelp" : "agentDestinationHelp")}
      </small>
    </FormGroup>
  );
}

export function ConnectionEndpointPicker({
  side,
  value,
  directory,
  onClose,
  onSelect,
}: EndpointProps & {
  onClose: () => void;
  onSelect: (value: string, client?: ClientRepresentation) => void;
}) {
  const { t } = useTranslation();
  const { hasAccess } = useAccess();
  const caller = side === "caller";
  const directoryTitle = caller ? "agentCallers" : "mcpServers";
  const rows = useMemo(
    () =>
      caller
        ? directory.agents.map((a) => ({
            id: a.clientId,
            name: label(t, a.name),
          }))
        : directory.servers.map((s) => ({ id: s.resource, name: s.name })),
    [caller, directory, t],
  );
  const canQuery = hasAccess("query-clients");
  const [tab, setTab] = useState(
    !canQuery ||
      (rows.length > 0 && (!value || rows.some((r) => r.id === value)))
      ? "directory"
      : "clients",
  );
  const [selection, setSelection] = useState<{
    value: string;
    client?: ClientRepresentation;
  }>();
  const directoryTable = (
    <KeycloakDataTable
      ariaLabelKey={directoryTitle}
      searchPlaceholderKey="search"
      isRadio
      canSelectAll={false}
      loader={rows}
      columns={[
        { name: "name" },
        { name: "id", displayKey: caller ? "clientId" : "agentResourceUrl" },
      ]}
      onSelect={(items) => {
        const row = items.at(0);
        setSelection(row ? { value: row.id } : undefined);
      }}
    />
  );
  const clientTable = (
    <ClientSelectTable
      isRadio
      isRowDisabled={(c) => !c.enabled || c.protocol !== "openid-connect"}
      onSelect={(clients) => {
        const client = clients.at(0);
        setSelection(
          client?.clientId
            ? {
                value: caller
                  ? client.clientId
                  : clientResourcePrefix + client.clientId,
                client,
              }
            : undefined,
        );
      }}
    />
  );
  return (
    <Modal
      isOpen
      variant={ModalVariant.large}
      title={t(caller ? "agentBrowseCaller" : "agentBrowseDestination")}
      onClose={onClose}
      actions={[
        <Button
          key="select"
          isDisabled={!selection}
          onClick={() =>
            selection && onSelect(selection.value, selection.client)
          }
        >
          {t("select")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onClose}>
          {t("cancel")}
        </Button>,
      ]}
    >
      {canQuery && rows.length > 0 ? (
        <Tabs
          activeKey={tab}
          aria-label={t(caller ? "agentCaller" : "agentDestination")}
          onSelect={(_, key) => {
            if (tab !== key) {
              setSelection(undefined);
              setTab(String(key));
            }
          }}
        >
          <Tab
            eventKey="directory"
            title={<TabTitleText>{t(directoryTitle)}</TabTitleText>}
          >
            {tab === "directory" && directoryTable}
          </Tab>
          <Tab
            eventKey="clients"
            title={<TabTitleText>{t("agentRealmClients")}</TabTitleText>}
          >
            {tab === "clients" && clientTable}
          </Tab>
        </Tabs>
      ) : canQuery ? (
        clientTable
      ) : (
        directoryTable
      )}
    </Modal>
  );
}
