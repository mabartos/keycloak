import {
  Card,
  CardBody,
  CardTitle,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import {
  Chart,
  ChartAxis,
  ChartBar,
  ChartTooltip,
  ChartVoronoiContainer,
} from "@patternfly/react-charts";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useAuthStats } from "./useAnalytics";
import type { AuthStatsEntry } from "./useAnalytics";

type DailyResets = {
  date: string;
  count: number;
};

function aggregatePasswordResets(entries: AuthStatsEntry[]): DailyResets[] {
  const map = new Map<string, number>();

  for (const entry of entries) {
    if (
      entry.eventType === "UPDATE_CREDENTIAL" &&
      entry.credentialType === "password"
    ) {
      map.set(entry.date, (map.get(entry.date) ?? 0) + entry.count);
    }
  }

  return Array.from(map.entries())
    .map(([date, count]) => ({ date, count }))
    .sort((a, b) => a.date.localeCompare(b.date));
}

type PasswordResetsCardProps = {
  from: string;
  to: string;
};

export const PasswordResetsCard = ({ from, to }: PasswordResetsCardProps) => {
  const { t } = useTranslation();
  const stats = useAuthStats(from, to);

  const chartData = useMemo(
    () =>
      stats
        ? aggregatePasswordResets(stats).map((d) => ({ x: d.date, y: d.count }))
        : [],
    [stats],
  );

  const total = useMemo(
    () => chartData.reduce((sum, d) => sum + d.y, 0),
    [chartData],
  );

  if (!stats) {
    return (
      <Card isFullHeight>
        <CardTitle>{t("analyticsPasswordResets")}</CardTitle>
        <CardBody>
          <KeycloakSpinner />
        </CardBody>
      </Card>
    );
  }

  if (chartData.length === 0) {
    return (
      <Card isFullHeight>
        <CardTitle>{t("analyticsPasswordResets")}</CardTitle>
        <CardBody>{t("analyticsNoData")}</CardBody>
      </Card>
    );
  }

  return (
    <Card isFullHeight>
      <CardTitle>
        {t("analyticsPasswordResets")} — {total.toLocaleString()}
      </CardTitle>
      <CardBody>
        <TextContent>
          <Text component={TextVariants.small}>
            {t("analyticsPasswordResetsDescription")}
          </Text>
        </TextContent>
        <div className="keycloak-analytics__chart-container--small">
          <Chart
            height={250}
            padding={{ bottom: 50, left: 60, right: 30, top: 20 }}
            containerComponent={
              <ChartVoronoiContainer
                labels={({ datum }) => `${datum.x}: ${datum.y}`}
                labelComponent={<ChartTooltip constrainToVisibleArea />}
              />
            }
          >
            <ChartAxis
              fixLabelOverlap
              tickFormat={(d) => (typeof d === "string" ? d.slice(5) : d)}
            />
            <ChartAxis
              dependentAxis
              tickFormat={(t) => (Number.isInteger(t) ? t : "")}
            />
            <ChartBar
              data={chartData}
              style={{ data: { fill: "#F0AB00" } }}
            />
          </Chart>
        </div>
      </CardBody>
    </Card>
  );
};
