import {
  AlertVariant,
  PageSection,
  useWizardContext,
  Wizard,
  WizardFooter,
  WizardStep,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormValuesToObject } from "../../util";
import { FormFields } from "../ClientDetails";
import { toClient } from "../routes/Client";
import { toClients } from "../routes/Clients";
import { CapabilityConfig } from "./CapabilityConfig";
import { GeneralSettings } from "./GeneralSettings";
import { LoginSettings } from "./LoginSettings";
import { ClientTypeSelector } from "./ClientTypeSelector";
import { useState, useEffect } from "react";
import { convertAttributeNameToForm } from "../../util";

const NewClientFooter = (newClientForm: any) => {
  const { t } = useTranslation();
  const { trigger } = newClientForm;
  const { activeStep, goToNextStep, goToPrevStep, close } = useWizardContext();

  const forward = async (onNext: () => void) => {
    if (!(await trigger())) {
      return;
    }
    onNext?.();
  };

  return (
    <WizardFooter
      activeStep={activeStep}
      onNext={() => forward(goToNextStep)}
      onBack={goToPrevStep}
      onClose={close}
      isBackDisabled={activeStep.index === 1}
      backButtonText={t("back")}
      nextButtonText={t("next")}
      cancelButtonText={t("cancel")}
    />
  );
};

export default function NewClientForm() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { realm } = useRealm();
  const navigate = useNavigate();
  const [saving, setSaving] = useState<boolean>(false);

  const { addAlert, addError } = useAlerts();
  const form = useForm<FormFields>({
    defaultValues: {
      protocol: "openid-connect",
      clientType: "custom",
      clientId: "",
      name: "",
      description: "",
      publicClient: true,
      authorizationServicesEnabled: false,
      serviceAccountsEnabled: false,
      implicitFlowEnabled: false,
      directAccessGrantsEnabled: false,
      standardFlowEnabled: true,
      frontchannelLogout: true,
      attributes: {
        saml_idp_initiated_sso_url_name: "",
        [convertAttributeNameToForm("attributes.pkce.code.challenge.method")]: "",
      },
    },
  });
  const { getValues, watch, setValue } = form;
  const protocol = watch("protocol");
  const clientType = watch("clientType");

  // Apply defaults based on client type
  useEffect(() => {
    if (!clientType || clientType === "custom") return;

    const pkceField = convertAttributeNameToForm<FormFields>(
      "attributes.pkce.code.challenge.method",
    );

    switch (clientType) {
      case "api":
        // Backend service - confidential client
        setValue("publicClient", false);
        setValue("standardFlowEnabled", false);
        setValue("serviceAccountsEnabled", true);
        setValue(pkceField, "");
        break;
      case "web-app":
        // Traditional web application - confidential client with standard flow
        setValue("publicClient", false);
        setValue("standardFlowEnabled", true);
        setValue("serviceAccountsEnabled", false);
        setValue(pkceField, "");
        break;
      case "spa":
        // Single page application - public client with PKCE
        setValue("publicClient", true);
        setValue("standardFlowEnabled", true);
        setValue("serviceAccountsEnabled", false);
        setValue(pkceField, "S256");
        break;
      case "native":
        // Mobile/desktop app - public client with PKCE
        setValue("publicClient", true);
        setValue("standardFlowEnabled", true);
        setValue("directAccessGrantsEnabled", true);
        setValue("serviceAccountsEnabled", false);
        setValue(pkceField, "S256");
        break;
    }
  }, [clientType, setValue]);

  const save = async () => {
    if (saving) return;
    setSaving(true);
    const formValues = getValues();
    const { clientType, ...client } = convertFormValuesToObject(formValues);
    try {
      const newClient = await adminClient.clients.create({
        ...client,
        clientId: client.clientId?.trim(),
      });
      addAlert(t("createClientSuccess"), AlertVariant.success);
      navigate(toClient({ realm, clientId: newClient.id, tab: "settings" }));
    } catch (error) {
      addError("createClientError", error);
    } finally {
      setSaving(false);
    }
  };

  const title = t("createClient");
  return (
    <>
      <ViewHeader titleKey="createClient" subKey="clientsExplain" />
      <PageSection variant="light">
        <FormProvider {...form}>
          <Wizard
            onClose={() => navigate(toClients({ realm }))}
            navAriaLabel={`${title} steps`}
            onSave={save}
            isProgressive
            footer={<NewClientFooter {...form} />}
          >
            <WizardStep
              name={t("clientTypeStep")}
              id="clientType"
              key="clientType"
            >
              <ClientTypeSelector />
            </WizardStep>
            <WizardStep
              name={t("generalSettings")}
              id="generalSettings"
              key="generalSettings"
            >
              <GeneralSettings />
            </WizardStep>
            <WizardStep
              name={t("capabilityConfig")}
              id="capabilityConfig"
              key="capabilityConfig"
              isHidden={protocol === "saml"}
            >
              <CapabilityConfig protocol={protocol} />
            </WizardStep>
            <WizardStep
              name={t("loginSettings")}
              id="loginSettings"
              key="loginSettings"
              footer={{
                backButtonText: t("back"),
                nextButtonText: t("save"),
                cancelButtonText: t("cancel"),
              }}
            >
              <FormAccess isHorizontal role="manage-clients">
                <LoginSettings protocol={protocol} />
              </FormAccess>
            </WizardStep>
          </Wizard>
        </FormProvider>
      </PageSection>
    </>
  );
}
