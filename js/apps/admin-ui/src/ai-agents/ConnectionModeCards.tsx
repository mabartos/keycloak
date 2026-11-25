import type { ConnectionMode } from "@keycloak/keycloak-admin-client/lib/defs/agentDirectoryRepresentation";
import {
  Button,
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  FormGroup,
  Grid,
  GridItem,
  Icon,
  Tooltip,
} from "@patternfly/react-core";
import {
  BuildingIcon,
  ExchangeAltIcon,
  ExclamationTriangleIcon,
  SignInAltIcon,
} from "@patternfly/react-icons";
import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useRealm } from "../context/realm-context/RealmContext";
import useIsFeatureEnabled, { Feature } from "../utils/useIsFeatureEnabled";

export const modeLabels = {
  interactive: "agentInteractive",
  delegated: "agentDelegated",
  enterprise: "agentEnterprise",
} as const;

const modes = {
  interactive: { description: "agentInteractiveHelp", icon: SignInAltIcon },
  delegated: { description: "agentDelegatedHelp", icon: ExchangeAltIcon },
  enterprise: { description: "agentEnterpriseHelp", icon: BuildingIcon },
} as const;

export function ConnectionModeCards({
  value,
  onChange,
  editing,
  clientTarget,
  externalIssuer,
}: {
  value: ConnectionMode;
  onChange: (mode: ConnectionMode) => void;
  editing: boolean;
  clientTarget: boolean;
  externalIssuer: boolean;
}) {
  const { t } = useTranslation();
  const enabled = useIsFeatureEnabled();
  const { realmRepresentation } = useRealm();
  useEffect(() => {
    // PatternFly dismisses the tooltip during document capture, before the modal handles Escape.
    const dismissWarning = (event: KeyboardEvent) => {
      if (
        event.key === "Escape" &&
        document.querySelector(".kc-mode-tooltip")
      ) {
        event.preventDefault();
      }
    };
    window.addEventListener("keydown", dismissWarning, true);
    return () => window.removeEventListener("keydown", dismissWarning, true);
  }, []);
  const externalOnly = externalIssuer ? "agentModeExternalOnly" : undefined;
  const unavailable = {
    interactive: externalOnly,
    delegated:
      externalOnly ??
      (!enabled(Feature.TokenExchangeDelegation)
        ? "agentModeEnableDelegation"
        : !enabled(Feature.AdminFineGrainedAuthzV2) ||
            !realmRepresentation.adminPermissionsEnabled
          ? "agentModeEnableRealmPermissions"
          : undefined),
    enterprise: clientTarget
      ? "agentModeNeedsRegisteredResource"
      : !enabled(Feature.IdentityAssertionJwt)
        ? "agentModeEnableEnterprise"
        : undefined,
  } as const;

  return (
    <FormGroup
      label={t("agentAccessMode")}
      fieldId="connection-mode"
      role="radiogroup"
    >
      <Grid hasGutter>
        {(Object.keys(modeLabels) as ConnectionMode[]).map((mode) => {
          const titleId = `mode-${mode}-title`;
          const reason =
            unavailable[mode] ?? (editing ? "agentModeReadOnly" : undefined);
          const { description, icon: ModeIcon } = modes[mode];
          return (
            <GridItem key={mode} span={12} md={4}>
              <Card
                isSelectable
                isCompact
                isFullHeight
                isSelected={value === mode}
                isDisabled={!!reason}
              >
                <CardHeader
                  className="kc-mode-header"
                  selectableActions={{
                    variant: "single",
                    name: "connection-mode",
                    selectableActionId: `mode-${mode}`,
                    selectableActionAriaLabelledby: titleId,
                    onChange: () => onChange(mode),
                  }}
                >
                  {reason && (
                    <Tooltip content={t(reason)} className="kc-mode-tooltip">
                      <Button
                        variant="plain"
                        className="kc-mode-warning"
                        aria-label={t("agentModeUnavailable", {
                          mode: t(modeLabels[mode]),
                        })}
                      >
                        <Icon status="warning">
                          <ExclamationTriangleIcon />
                        </Icon>
                      </Button>
                    </Tooltip>
                  )}
                  <ModeIcon className="kc-mode-icon" aria-hidden />
                </CardHeader>
                <CardTitle id={titleId} className="pf-v5-u-text-align-center">
                  {t(modeLabels[mode])}
                </CardTitle>
                <CardBody className="pf-v5-u-text-align-center">
                  {t(description)}
                </CardBody>
              </Card>
            </GridItem>
          );
        })}
      </Grid>
    </FormGroup>
  );
}
