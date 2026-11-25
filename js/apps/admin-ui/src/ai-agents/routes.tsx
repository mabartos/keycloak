import { lazy } from "react";
import type { AppRouteObject } from "../routes";

const Agents = lazy(() => import("./AgentsSection"));

export const agentsRoute: AppRouteObject = {
  path: "/:realm/ai-agents",
  element: <Agents />,
  handle: { access: "view-realm", breadcrumb: (t) => t("aiAgents") },
};
