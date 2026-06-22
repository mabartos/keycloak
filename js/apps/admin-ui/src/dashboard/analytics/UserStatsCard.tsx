import {
  Card,
  CardBody,
  CardTitle,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import { ChartDonut } from "@patternfly/react-charts";
import { useTranslation } from "react-i18next";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useUserStats } from "./useAnalytics";

export const UserStatsCard = () => {
  const { t } = useTranslation();
  const stats = useUserStats();

  if (!stats) {
    return (
      <Card isFullHeight>
        <CardTitle>{t("analyticsUserStats")}</CardTitle>
        <CardBody>
          <KeycloakSpinner />
        </CardBody>
      </Card>
    );
  }

  if (stats.total === 0) {
    return (
      <Card isFullHeight>
        <CardTitle>{t("analyticsUserStats")}</CardTitle>
        <CardBody>{t("analyticsNoData")}</CardBody>
      </Card>
    );
  }

  return (
    <Card isFullHeight>
      <CardTitle>{t("analyticsUserStats")}</CardTitle>
      <CardBody>
        <TextContent>
          <Text component={TextVariants.small}>
            {t("analyticsUserStatsDescription")}
          </Text>
        </TextContent>
        <div className="keycloak-analytics__chart-container--donut">
          <ChartDonut
            constrainToVisibleArea
            data={[
              { x: t("analyticsActiveUsers"), y: stats.active },
              { x: t("analyticsInactiveUsers"), y: stats.inactive },
              { x: t("analyticsLockedUsers"), y: stats.locked },
            ]}
            colorScale={["#3E8635", "#F0AB00", "#C9190B"]}
            title={String(stats.total)}
            subTitle={t("analyticsTotal")}
            height={230}
            width={350}
            labels={({ datum }) => `${datum.x}: ${datum.y}`}
          />
        </div>
      </CardBody>
    </Card>
  );
};
