export type ConnectionMode = "interactive" | "delegated" | "enterprise";

export interface AgentRegistration {
  id: string;
  name: string;
  clientId: string;
  clientUuid?: string;
  cimd: boolean;
  loopback: boolean;
}

export interface AgentServer {
  id: string;
  name: string;
  resource: string;
  clientId?: string;
  clientUuid?: string;
  issuer?: string;
  supportedScopes: string[];
}

export interface AgentConnection {
  id: string;
  caller: string;
  resource: string;
  mode: ConnectionMode;
  scopes: string[];
  requiredRealmRole?: string;
  downstreamClientId?: string;
}

export interface AgentDirectoryRepresentation {
  agents: AgentRegistration[];
  servers: AgentServer[];
  connections: AgentConnection[];
}
