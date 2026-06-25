import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { Alert, FormGroup } from "@patternfly/react-core";
import { useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { FormFields } from "../ClientDetails";
import { convertAttributeNameToForm } from "../../util";

const ROTATION_CONFIG_PREFIX = "attributes.client.secret.rotation.config.";

type SecretRotationConfigProps = {
  client: ClientRepresentation;
};

export const SecretRotationConfig = ({ client }: SecretRotationConfigProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { control } = useFormContext<FormFields>();
  const [rotationSource, setRotationSource] = useState<string | null>(null);
  const serverInfo = useServerInfo();

  const executorProperties = serverInfo.componentTypes?.[
    "org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider"
  ]?.find((type) => type.id === "secret-rotation")?.properties;

  useFetch(
    () => adminClient.clients.getSecretRotationConfig({ id: client.id! }),
    (config) => setRotationSource(config.source),
    [],
  );

  const rotationEnabled = useWatch({
    control,
    name: convertAttributeNameToForm<FormFields>(
      `${ROTATION_CONFIG_PREFIX}enabled`,
    ),
  });

  if (rotationSource === null) return null;

  if (rotationSource === "policy") {
    return (
      <FormGroup fieldId="secret-rotation-policy-info">
        <Alert
          variant="info"
          isInline
          title={t("secretRotationManagedByPolicy")}
        />
      </FormGroup>
    );
  }

  const convertToName = (name: string) =>
    convertAttributeNameToForm<FormFields>(`${ROTATION_CONFIG_PREFIX}${name}`);

  return (
    <>
      <DefaultSwitchControl
        name={convertAttributeNameToForm<FormFields>(
          `${ROTATION_CONFIG_PREFIX}enabled`,
        )}
        label={t("enableSecretRotation")}
        labelIcon={t("enableSecretRotationHelp")}
        stringify
      />
      {rotationEnabled === "true" && executorProperties && (
        <DynamicComponents
          properties={executorProperties}
          convertToName={convertToName}
          stringify
        />
      )}
    </>
  );
};
