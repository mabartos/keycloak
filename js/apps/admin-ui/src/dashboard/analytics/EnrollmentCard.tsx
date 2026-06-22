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
import { useEnrollmentStats } from "./useAnalytics";

const CREDENTIAL_LABELS: Record<string, string> = {
  password: "Password",
  otp: "OTP",
  webauthn: "WebAuthn",
  "webauthn-passwordless": "Passkey",
  "recovery-authn-codes": "Recovery Codes",
};

export const EnrollmentCard = () => {
  const { t } = useTranslation();
  const stats = useEnrollmentStats();

  const chartData = useMemo(() => {
    if (!stats) return [];
    return stats
      .filter((entry) => entry.credentialType !== "password")
      .map((entry) => ({
        x: CREDENTIAL_LABELS[entry.credentialType] || entry.credentialType,
        y: entry.count,
      }))
      .sort((a, b) => b.y - a.y);
  }, [stats]);

  if (!stats) {
    return (
      <Card isFullHeight>
        <CardTitle>{t("analyticsEnrollment")}</CardTitle>
        <CardBody>
          <KeycloakSpinner />
        </CardBody>
      </Card>
    );
  }

  if (chartData.length === 0) {
    return (
      <Card isFullHeight>
        <CardTitle>{t("analyticsEnrollment")}</CardTitle>
        <CardBody>{t("analyticsNoData")}</CardBody>
      </Card>
    );
  }

  return (
    <Card isFullHeight>
      <CardTitle>{t("analyticsEnrollment")}</CardTitle>
      <CardBody>
        <TextContent>
          <Text component={TextVariants.small}>
            {t("analyticsEnrollmentDescription")}
          </Text>
        </TextContent>
        <div className="keycloak-analytics__chart-container--small">
          <Chart
            height={250}
            horizontal
            padding={{ bottom: 30, left: 150, right: 30, top: 20 }}
            containerComponent={
              <ChartVoronoiContainer
                labels={({ datum }) => `${datum.x}: ${datum.y}`}
                labelComponent={<ChartTooltip constrainToVisibleArea />}
              />
            }
          >
            <ChartAxis />
            <ChartAxis
              dependentAxis
              tickFormat={(t) => (Number.isInteger(t) ? t : "")}
            />
            <ChartBar
              data={chartData}
              style={{ data: { fill: "#0066CC" } }}
            />
          </Chart>
        </div>
      </CardBody>
    </Card>
  );
};
