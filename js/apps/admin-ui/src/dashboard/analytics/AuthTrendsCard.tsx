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
  ChartGroup,
  ChartTooltip,
  ChartVoronoiContainer,
} from "@patternfly/react-charts";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useAuthStats } from "./useAnalytics";
import type { AuthStatsEntry } from "./useAnalytics";

type DailyAggregate = {
  date: string;
  logins: number;
  loginErrors: number;
  registrations: number;
};

function aggregateByDate(entries: AuthStatsEntry[]): DailyAggregate[] {
  const map = new Map<string, DailyAggregate>();

  for (const entry of entries) {
    let agg = map.get(entry.date);
    if (!agg) {
      agg = { date: entry.date, logins: 0, loginErrors: 0, registrations: 0 };
      map.set(entry.date, agg);
    }

    switch (entry.eventType) {
      case "LOGIN":
        agg.logins += entry.count;
        break;
      case "LOGIN_ERROR":
        agg.loginErrors += entry.count;
        break;
      case "REGISTER":
      case "REGISTER_ERROR":
        agg.registrations += entry.count;
        break;
    }
  }

  return Array.from(map.values()).sort((a, b) =>
    a.date.localeCompare(b.date),
  );
}

type AuthTrendsCardProps = {
  from: string;
  to: string;
};

export const AuthTrendsCard = ({ from, to }: AuthTrendsCardProps) => {
  const { t } = useTranslation();
  const stats = useAuthStats(from, to);

  const aggregated = useMemo(
    () => (stats ? aggregateByDate(stats) : []),
    [stats],
  );

  const loginData = useMemo(
    () => aggregated.map((d) => ({ x: d.date, y: d.logins })),
    [aggregated],
  );
  const errorData = useMemo(
    () => aggregated.map((d) => ({ x: d.date, y: d.loginErrors })),
    [aggregated],
  );
  const registerData = useMemo(
    () => aggregated.map((d) => ({ x: d.date, y: d.registrations })),
    [aggregated],
  );

  return (
    <Card isFullHeight>
      <CardTitle>{t("analyticsAuthTrends")}</CardTitle>
      <CardBody>
        <TextContent>
          <Text component={TextVariants.small}>
            {t("analyticsAuthTrendsDescription")}
          </Text>
        </TextContent>
        {!stats ? (
          <KeycloakSpinner />
        ) : aggregated.length === 0 ? (
          <>{t("analyticsNoData")}</>
        ) : (
          <div className="keycloak-analytics__chart-container">
            <Chart
              height={350}
              padding={{ bottom: 100, left: 60, right: 30, top: 20 }}
              containerComponent={
                <ChartVoronoiContainer
                  labels={({ datum }) => `${datum.x}: ${datum.y}`}
                  labelComponent={<ChartTooltip constrainToVisibleArea />}
                />
              }
              legendData={[
                { name: t("analyticsLoginSuccess"), symbol: { fill: "#3E8635" } },
                { name: t("analyticsLoginFailure"), symbol: { fill: "#C9190B" } },
                { name: t("analyticsRegistrations"), symbol: { fill: "#0066CC" } },
              ]}
              legendOrientation="horizontal"
              legendPosition="bottom"
            >
              <ChartAxis fixLabelOverlap />
              <ChartAxis
                dependentAxis
                tickFormat={(t) => (Number.isInteger(t) ? t : "")}
              />
              <ChartGroup offset={11}>
                <ChartBar
                  data={loginData}
                  style={{ data: { fill: "#3E8635" } }}
                />
                <ChartBar
                  data={errorData}
                  style={{ data: { fill: "#C9190B" } }}
                />
                <ChartBar
                  data={registerData}
                  style={{ data: { fill: "#0066CC" } }}
                />
              </ChartGroup>
            </Chart>
          </div>
        )}
      </CardBody>
    </Card>
  );
};
