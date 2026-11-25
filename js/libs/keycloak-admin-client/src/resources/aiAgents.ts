import type { KeycloakAdminClient } from "../client.js";
import type {
  AgentConnection,
  AgentDirectoryRepresentation,
  AgentRegistration,
  AgentServer,
} from "../defs/agentDirectoryRepresentation.js";
import Resource from "./resource.js";

export class AiAgents extends Resource<{ realm?: string }> {
  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/ai-agents",
      getUrlParams: () => ({ realm: client.realmName }),
      getBaseUrl: () => client.baseUrl,
    });
  }

  list = this.makeRequest<{}, AgentDirectoryRepresentation>({ method: "GET" });
  saveAgent = this.makeRequest<
    Pick<AgentRegistration, "clientId"> &
      Partial<Pick<AgentRegistration, "name" | "cimd" | "loopback">>,
    AgentRegistration
  >({ method: "PUT", path: "/agents" });
  saveServer = this.makeRequest<
    Omit<AgentServer, "id" | "clientUuid">,
    AgentServer
  >({ method: "PUT", path: "/servers" });
  saveConnection = this.makeRequest<
    Omit<AgentConnection, "id">,
    AgentConnection
  >({ method: "PUT", path: "/connections" });
  remove = this.makeRequest<
    { collection: keyof AgentDirectoryRepresentation; id: string },
    void
  >({
    method: "DELETE",
    path: "/{collection}/{id}",
    urlParamKeys: ["collection", "id"],
  });
}
