import {
  Button,
  Card,
  CardBody,
  CardTitle,
  EmptyState,
  EmptyStateBody,
  EmptyStateHeader,
  Grid,
  GridItem,
  PageSection,
  Title,
  ToggleGroup,
  ToggleGroupItem,
} from "@patternfly/react-core";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toRealmSettings } from "../../realm-settings/routes/RealmSettings";
import { AuthTrendsCard } from "./AuthTrendsCard";
import { EnrollmentCard } from "./EnrollmentCard";
import { PasswordResetsCard } from "./PasswordResetsCard";
import { useUserStats } from "./useAnalytics";

import "./analytics-dashboard.css";

type TimeRange = "1d" | "1w" | "1m" | "1y";

function computeDateRange(range: TimeRange): { from: string; to: string } {
  const now = new Date();
  const to = now.toISOString().slice(0, 10);

  const offsets: Record<TimeRange, number> = {
    "1d": 1,
    "1w": 7,
    "1m": 30,
    "1y": 365,
  };

  const from = new Date(now.getTime() - offsets[range] * 24 * 60 * 60 * 1000)
    .toISOString()
    .slice(0, 10);

  return { from, to };
}

type StatVariant = "default" | "success" | "warning" | "danger";

export const SummaryStatCard = ({
  value,
  label,
  variant = "default",
}: {
  value: string | number;
  label: string;
  variant?: StatVariant;
}) => {
  const variantClass =
    variant !== "default"
      ? ` keycloak-analytics__stat-card--${variant}`
      : "";

  return (
    <Card
      isFullHeight
      isCompact
      className={`keycloak-analytics__stat-card${variantClass}`}
    >
      <CardBody>
        <span className="keycloak-analytics__stat-value">
          {typeof value === "number" ? value.toLocaleString() : value}
        </span>
        <span className="keycloak-analytics__stat-label">{label}</span>
      </CardBody>
    </Card>
  );
};

const SummaryRow = () => {
  const { t } = useTranslation();
  const stats = useUserStats();

  return (
    <Grid hasGutter>
      <GridItem lg={3} md={6} sm={12}>
        <SummaryStatCard
          value={stats?.total ?? "—"}
          label={t("analyticsTotalUsers")}
        />
      </GridItem>
      <GridItem lg={3} md={6} sm={12}>
        <SummaryStatCard
          value={stats?.active ?? "—"}
          label={t("analyticsActiveUsers")}
          variant="success"
        />
      </GridItem>
      <GridItem lg={3} md={6} sm={12}>
        <SummaryStatCard
          value={stats?.inactive ?? "—"}
          label={t("analyticsInactiveUsers")}
          variant="warning"
        />
      </GridItem>
      <GridItem lg={3} md={6} sm={12}>
        <SummaryStatCard
          value={stats?.clients ?? "—"}
          label={t("analyticsTotalClients")}
        />
      </GridItem>
    </Grid>
  );
};

const SetupOverlay = () => {
  const { t } = useTranslation();
  const { realm } = useRealm();

  return (
    <div className="keycloak-analytics__overlay">
      <EmptyState variant="lg">
        <EmptyStateHeader
          titleText={t("analyticsSetupTitle")}
          headingLevel="h2"
        />
        <EmptyStateBody>{t("analyticsSetupDescription")}</EmptyStateBody>
        <Button
          variant="primary"
          component={(props) => (
            <Link
              {...props}
              to={toRealmSettings({ realm, tab: "events" }).pathname!}
            />
          )}
        >
          {t("analyticsSetupAction")}
        </Button>
      </EmptyState>
    </div>
  );
};

export const AnalyticsDashboard = () => {
  const { t } = useTranslation();
  const { realmRepresentation } = useRealm();
  const isListenerEnabled =
    realmRepresentation.eventsListeners?.includes("analytics") ?? false;

  const [timeRange, setTimeRange] = useState<TimeRange>("1d");
  const { from, to } = useMemo(() => computeDateRange(timeRange), [timeRange]);

  const timeRangeSelector = (
    <ToggleGroup aria-label={t("analyticsTimeRange")}>
      {(["1d", "1w", "1m", "1y"] as const).map((range) => (
        <ToggleGroupItem
          key={range}
          text={t(`analyticsRange_${range}`)}
          buttonId={`time-range-${range}`}
          isSelected={timeRange === range}
          onChange={() => setTimeRange(range)}
        />
      ))}
    </ToggleGroup>
  );

  const charts = (
    <Grid hasGutter>
      <GridItem lg={6} sm={12}>
        <AuthTrendsCard from={from} to={to} />
      </GridItem>
      <GridItem lg={6} sm={12}>
        <EnrollmentCard />
      </GridItem>
      <GridItem lg={6} sm={12}>
        <PasswordResetsCard from={from} to={to} />
      </GridItem>
    </Grid>
  );

  return (
    <>
      <PageSection className="keycloak-analytics__summary-section">
        <SummaryRow />
      </PageSection>
      <PageSection>
        <div className="keycloak-analytics__header">
          <Title headingLevel="h2" size="lg">
            {t("analyticsChartsTitle")}
          </Title>
          {timeRangeSelector}
        </div>
        {!isListenerEnabled ? (
          <div className="keycloak-analytics__charts-wrapper">
            <div className="keycloak-analytics__content--blurred">
              {charts}
            </div>
            <SetupOverlay />
          </div>
        ) : (
          charts
        )}
      </PageSection>
    </>
  );
};
