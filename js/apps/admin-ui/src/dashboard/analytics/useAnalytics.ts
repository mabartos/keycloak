import { useState } from "react";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";

export type UserStats = {
  active: number;
  inactive: number;
  locked: number;
  total: number;
  clients: number;
};

export type AuthStatsEntry = {
  date: string;
  eventType: string;
  credentialType: string;
  count: number;
};

export type EnrollmentStatsEntry = {
  credentialType: string;
  count: number;
};

export function useUserStats() {
  const { adminClient } = useAdminClient();
  const [stats, setStats] = useState<UserStats>();

  useFetch(
    () => fetchAdminUI<UserStats>(adminClient, "ui-ext/analytics/user-stats"),
    (data) => setStats(data),
    [],
  );

  return stats;
}

export function useAuthStats(from: string, to: string) {
  const { adminClient } = useAdminClient();
  const [stats, setStats] = useState<AuthStatsEntry[]>();

  useFetch(
    () =>
      fetchAdminUI<AuthStatsEntry[]>(
        adminClient,
        "ui-ext/analytics/auth-stats",
        { from, to },
      ),
    (data) => setStats(data),
    [from, to],
  );

  return stats;
}

export function useEnrollmentStats() {
  const { adminClient } = useAdminClient();
  const [stats, setStats] = useState<EnrollmentStatsEntry[]>();

  useFetch(
    () =>
      fetchAdminUI<EnrollmentStatsEntry[]>(
        adminClient,
        "ui-ext/analytics/enrollment-stats",
      ),
    (data) => setStats(data),
    [],
  );

  return stats;
}

