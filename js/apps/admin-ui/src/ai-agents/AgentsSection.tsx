import type { AgentDirectoryRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/agentDirectoryRepresentation";
import {
  KeycloakSpinner,
  ListEmptyState,
  label,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Badge,
  Button,
  ButtonVariant,
  Card,
  CardBody,
  CardFooter,
  CardTitle,
  Flex,
  Gallery,
  Label,
  PageSection,
  SearchInput,
  Tab,
  Tabs,
  TabTitleText,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  ArrowRightIcon,
  ProjectDiagramIcon,
  RobotIcon,
  ServerIcon,
} from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useSearchParams } from "react-router-dom";
import { PageNotFoundSection } from "../PageNotFoundSection";
import { useAdminClient } from "../admin-client";
import { toClient } from "../clients/routes/Client";
import { ConfirmDialogModal } from "../components/confirm-dialog/ConfirmDialog";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";
import { clientResourcePrefix } from "./ConnectionEndpoint";
import { modeLabels } from "./ConnectionModeCards";
import { DirectoryEditor, type Editor, titles } from "./DirectoryEditor";
import "./agents.css";

type Collection = keyof AgentDirectoryRepresentation;
const tabs = {
  agents: "agentCallers",
  servers: "mcpServers",
  connections: "agentConnections",
} as const;
const icons = {
  connections: ProjectDiagramIcon,
  agents: RobotIcon,
  servers: ServerIcon,
};

export default function AgentsSection() {
  const enabled = useIsFeatureEnabled();
  const { realm } = useRealm();
  return enabled(Feature.AiAgents) ? (
    <DirectoryView key={realm} />
  ) : (
    <PageNotFoundSection />
  );
}

function DirectoryView() {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { hasAccess } = useAccess();
  const { addAlert, addError } = useAlerts();
  const canEdit = hasAccess("manage-realm");
  const [params, setParams] = useSearchParams();
  const selectedTab = params.get("tab");
  const tab: Collection =
    selectedTab === "servers" || selectedTab === "connections"
      ? selectedTab
      : "agents";
  const canAdd = canEdit || (tab === "agents" && hasAccess("manage-clients"));
  const [directory, setDirectory] = useState<AgentDirectoryRepresentation>();
  const [revision, setRevision] = useState(0);
  const [search, setSearch] = useState("");
  const [editor, setEditor] = useState<Editor>();
  const [removal, setRemoval] = useState<{
    id: string;
    collection: Collection;
    name: string;
  }>();
  const [deleting, setDeleting] = useState(false);
  useFetch(() => adminClient.aiAgents.list({ realm }), setDirectory, [
    adminClient,
    realm,
    revision,
  ]);
  if (!directory) return <KeycloakSpinner />;
  const caller = params.get("caller");
  const target = params.get("resource");
  const show = (collection: Collection, filter?: Record<string, string>) => {
    setSearch("");
    setParams({ tab: collection, ...filter });
  };
  const match = (...values: (string | undefined)[]) =>
    values.join(" ").toLowerCase().includes(search.trim().toLowerCase());
  const agentName = (clientId: string) =>
    label(
      t,
      directory.agents.find((a) => a.clientId === clientId)?.name ?? clientId,
    );
  const serverName = (resource: string) =>
    directory.servers.find((s) => s.resource === resource)?.name ??
    (resource.startsWith(clientResourcePrefix)
      ? resource.slice(clientResourcePrefix.length)
      : resource);
  const actions = (e: Editor, id: string, name: string) =>
    canEdit && (
      <>
        {(e.kind !== "agents" || e.entry?.cimd) && (
          <Button
            variant="link"
            isInline
            onClick={() => setEditor(e)}
            aria-label={t("agentEditNamed", { name })}
          >
            {t("edit")}
          </Button>
        )}
        <Button
          variant="link"
          isInline
          isDanger
          onClick={() => setRemoval({ collection: e.kind, id, name })}
          aria-label={t("agentRemoveNamed", { name })}
        >
          {t("remove")}
        </Button>
      </>
    );
  const clientLink = (uuid: string | undefined) =>
    uuid &&
    hasAccess("query-clients") && (
      <Link to={toClient({ realm, clientId: uuid, tab: "settings" })}>
        {t("clientSettings")}
      </Link>
    );
  const add = () => {
    setEditor(
      tab === "connections"
        ? {
            kind: tab,
            caller: caller ?? undefined,
            resource: target ?? undefined,
          }
        : { kind: tab },
    );
  };
  const filtered = Boolean(
    search || (tab === "connections" && (caller || target)),
  );
  const showToolbar = directory[tab].length > 0 || filtered;
  const cards =
    tab === "connections"
      ? directory.connections
          .filter(
            (c) =>
              (!caller || caller === c.caller) &&
              (!target || target === c.resource) &&
              match(
                c.caller,
                agentName(c.caller),
                c.resource,
                serverName(c.resource),
                c.requiredRealmRole,
                ...c.scopes,
              ),
          )
          .map((c) => {
            const name = agentName(c.caller) + " → " + serverName(c.resource);
            return (
              <Card key={c.id} isCompact>
                <CardTitle>
                  <span className="kc-agent-flow">
                    <span>{agentName(c.caller)}</span>
                    <ArrowRightIcon aria-label={t("agentCalls")} />
                    <span>{serverName(c.resource)}</span>
                  </span>
                </CardTitle>
                <CardBody>
                  <Label
                    color={
                      c.mode === "delegated"
                        ? "purple"
                        : c.mode === "enterprise"
                          ? "cyan"
                          : "blue"
                    }
                  >
                    {t(modeLabels[c.mode])}
                  </Label>
                  <dl className="kc-agent-details">
                    <dt>
                      {t(
                        c.resource.startsWith(clientResourcePrefix)
                          ? "clientId"
                          : "agentResourceUrl",
                      )}
                    </dt>
                    <dd>
                      {c.resource.startsWith(clientResourcePrefix)
                        ? c.resource.slice(clientResourcePrefix.length)
                        : c.resource}
                    </dd>
                    <dt>{t("agentAllowedScopes")}</dt>
                    <dd>{c.scopes.join(" ") || t("agentNoScopes")}</dd>
                    {c.requiredRealmRole && (
                      <>
                        <dt>{t("agentRequiredRole")}</dt>
                        <dd>{c.requiredRealmRole}</dd>
                      </>
                    )}
                  </dl>
                </CardBody>
                {canEdit && (
                  <CardFooter>
                    <Flex>
                      {actions({ kind: "connections", entry: c }, c.id, name)}
                    </Flex>
                  </CardFooter>
                )}
              </Card>
            );
          })
      : tab === "agents"
        ? directory.agents
            .filter((a) => match(label(t, a.name), a.clientId))
            .map((a) => (
              <Card key={a.id} isCompact>
                <CardTitle>
                  <RobotIcon /> {label(t, a.name)}
                </CardTitle>
                <CardBody>
                  <Label>
                    {t(a.cimd ? "agentCimdDocument" : "existingClient")}
                  </Label>
                  <dl className="kc-agent-details">
                    <dt>{t(a.cimd ? "agentCimdUrl" : "clientId")}</dt>
                    <dd>{a.clientId}</dd>
                  </dl>
                  <Button
                    variant="link"
                    isInline
                    onClick={() => show("connections", { caller: a.clientId })}
                  >
                    {t("agentOutgoingCount", {
                      count: directory.connections.filter(
                        (c) => c.caller === a.clientId,
                      ).length,
                    })}
                  </Button>
                </CardBody>
                <CardFooter>
                  <Flex>
                    {canEdit && (
                      <Button
                        variant="link"
                        isInline
                        onClick={() =>
                          setEditor({ kind: "connections", caller: a.clientId })
                        }
                      >
                        {t("agentConnectResource")}
                      </Button>
                    )}
                    {clientLink(a.clientUuid)}
                    {actions(
                      { kind: "agents", entry: a },
                      a.id,
                      label(t, a.name),
                    )}
                  </Flex>
                </CardFooter>
              </Card>
            ))
        : directory.servers
            .filter((s) => match(s.name, s.resource, s.clientId, s.issuer))
            .map((s) => (
              <Card key={s.id} isCompact>
                <CardTitle>
                  <ServerIcon /> {s.name}
                </CardTitle>
                <CardBody>
                  <Label>
                    {t(s.issuer ? "agentExternalIssuer" : "agentThisRealm")}
                  </Label>
                  <dl className="kc-agent-details">
                    <dt>{t("agentResourceUrl")}</dt>
                    <dd>{s.resource}</dd>
                    {s.issuer && (
                      <>
                        <dt>{t("agentIssuerUrl")}</dt>
                        <dd>{s.issuer}</dd>
                      </>
                    )}
                    {s.supportedScopes.length > 0 && (
                      <>
                        <dt>{t("agentSupportedScopes")}</dt>
                        <dd>{s.supportedScopes.join(" ")}</dd>
                      </>
                    )}
                  </dl>
                  <Flex>
                    <Button
                      variant="link"
                      isInline
                      onClick={() =>
                        show("connections", { resource: s.resource })
                      }
                    >
                      {t("agentIncomingCount", {
                        count: directory.connections.filter(
                          (c) => c.resource === s.resource,
                        ).length,
                      })}
                    </Button>
                    {s.clientId && (
                      <Button
                        variant="link"
                        isInline
                        onClick={() =>
                          show("connections", { caller: s.clientId! })
                        }
                      >
                        {t("agentOutgoingCount", {
                          count: directory.connections.filter(
                            (c) => c.caller === s.clientId,
                          ).length,
                        })}
                      </Button>
                    )}
                  </Flex>
                </CardBody>
                <CardFooter>
                  <Flex>
                    {canEdit && (
                      <Button
                        variant="link"
                        isInline
                        onClick={() =>
                          setEditor({
                            kind: "connections",
                            resource: s.resource,
                          })
                        }
                      >
                        {t("agentAddCaller")}
                      </Button>
                    )}
                    {clientLink(s.clientUuid)}
                    {actions({ kind: "servers", entry: s }, s.id, s.name)}
                  </Flex>
                </CardFooter>
              </Card>
            ));
  return (
    <>
      <ViewHeader titleKey="aiAgents" subKey="aiAgentsIntro" />
      <Tabs
        activeKey={tab}
        onSelect={(_, key) => show(key as Collection)}
        aria-label={t("aiAgents")}
      >
        {(Object.keys(tabs) as Collection[]).map((key) => (
          <Tab
            key={key}
            eventKey={key}
            title={
              <TabTitleText>
                {t(tabs[key])} <Badge isRead>{directory[key].length}</Badge>
              </TabTitleText>
            }
          />
        ))}
      </Tabs>
      <PageSection isFilled={false}>
        {showToolbar && (
          <Toolbar>
            <ToolbarContent>
              <ToolbarItem>
                <SearchInput
                  aria-label={t("agentDirectorySearch")}
                  placeholder={t("search")}
                  value={search}
                  onChange={(_, v) => setSearch(v)}
                  onClear={() => setSearch("")}
                />
              </ToolbarItem>
              {tab === "connections" && (caller || target) && (
                <ToolbarItem>
                  <Label onClose={() => show(tab)}>
                    {caller ? agentName(caller) : serverName(target!)}
                  </Label>
                </ToolbarItem>
              )}
              {canAdd && (
                <ToolbarItem>
                  <Button onClick={add}>{t(titles[tab])}</Button>
                </ToolbarItem>
              )}
            </ToolbarContent>
          </Toolbar>
        )}
        {cards.length ? (
          <Gallery hasGutter className="kc-agent-gallery">
            {cards}
          </Gallery>
        ) : (
          <ListEmptyState
            icon={icons[tab]}
            isSearchVariant={filtered}
            message={t(
              filtered
                ? "agentNoMatches"
                : tab === "connections"
                  ? "agentEmptyConnections"
                  : tab === "agents"
                    ? "agentEmptyAgents"
                    : "agentEmptyServers",
            )}
            instructions={t(
              filtered
                ? "agentAdjustFilter"
                : tab === "connections"
                  ? "agentEmptyConnectionsHelp"
                  : tab === "agents"
                    ? "agentEmptyAgentsHelp"
                    : "agentEmptyServersHelp",
            )}
            primaryActionText={
              !showToolbar && canAdd ? t(titles[tab]) : undefined
            }
            onPrimaryAction={add}
            secondaryActions={
              filtered
                ? [
                    {
                      text: t("clearAllFilters"),
                      type: ButtonVariant.link,
                      onClick: () => show(tab),
                    },
                  ]
                : undefined
            }
          />
        )}
      </PageSection>
      {editor && (
        <DirectoryEditor
          editor={editor}
          directory={directory}
          onClose={() => setEditor(undefined)}
          onSaved={() => {
            setEditor(undefined);
            setRevision((r) => r + 1);
            if (editor.kind === "agents" && !editor.entry) {
              setSearch("");
            }
            if (editor.kind === "connections" && tab !== "connections") {
              show(
                "connections",
                editor.caller
                  ? { caller: editor.caller }
                  : editor.resource
                    ? { resource: editor.resource }
                    : undefined,
              );
            }
          }}
        />
      )}
      {removal && (
        <ConfirmDialogModal
          open
          titleKey={t("agentRemoveNamed", { name: removal.name })}
          messageKey={
            removal.collection === "connections"
              ? "agentRemoveConnectionHelp"
              : removal.collection === "agents" &&
                  directory.agents.some(
                    (a) =>
                      a.id === removal.id && a.clientUuid === a.id && !a.cimd,
                  )
                ? "agentUnmarkClientHelp"
                : "agentRemoveEntryHelp"
          }
          continueButtonLabel="remove"
          continueButtonVariant={ButtonVariant.danger}
          confirmButtonDisabled={deleting}
          toggleDialog={() => setRemoval(undefined)}
          onConfirm={async () => {
            setDeleting(true);
            try {
              await adminClient.aiAgents.remove({
                realm,
                collection: removal.collection,
                id: removal.id,
              });
              setRevision((r) => r + 1);
              addAlert(t("agentDirectoryRemoved"), AlertVariant.success);
            } catch (error) {
              addError("agentDirectoryRemoveError", error);
            } finally {
              setDeleting(false);
            }
          }}
        />
      )}
    </>
  );
}
