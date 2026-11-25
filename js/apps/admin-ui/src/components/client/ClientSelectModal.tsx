import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { ClientQuery } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { Button, Modal, ModalVariant } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { translationFormatter } from "../../utils/translationFormatter";

type ClientSelectModalProps = {
  onClose: () => void;
  onSelect: (clients: ClientRepresentation[]) => void;
  isRadio?: boolean;
  isRowDisabled?: (client: ClientRepresentation) => boolean;
};

export const ClientSelectTable = ({
  onSelect,
  isRadio = false,
  isRowDisabled,
}: Omit<ClientSelectModalProps, "onClose">) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();

  const loader = async (
    first?: number,
    max?: number,
    search?: string,
  ): Promise<ClientRepresentation[]> => {
    const params: ClientQuery = {
      first: first!,
      max: max!,
    };

    if (search) {
      params.clientId = search;
      params.search = true;
    }

    return adminClient.clients.find(params);
  };

  return (
    <KeycloakDataTable
      onSelect={onSelect}
      searchPlaceholderKey="searchForClient"
      isPaginated
      canSelectAll={!isRadio}
      isRadio={isRadio}
      isRowDisabled={isRowDisabled}
      loader={loader}
      ariaLabelKey="clientList"
      columns={[
        { name: "clientId" },
        { name: "name", cellFormatters: [translationFormatter(t)] },
      ]}
    />
  );
};

export const ClientSelectModal = ({
  onClose,
  onSelect,
  ...tableProps
}: ClientSelectModalProps) => {
  const { t } = useTranslation();
  const [selectedClients, setSelectedClients] = useState<
    ClientRepresentation[]
  >([]);

  return (
    <Modal
      data-testid="select-client-modal"
      variant={ModalVariant.large}
      title={t("selectClient")}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid="confirm"
          key="confirm"
          isDisabled={selectedClients.length === 0}
          variant="primary"
          onClick={() => {
            onSelect(selectedClients);
            onClose();
          }}
        >
          {t("add")}
        </Button>,
        <Button
          data-testid="cancel"
          key="cancel"
          variant="link"
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <ClientSelectTable {...tableProps} onSelect={setSelectedClients} />
    </Modal>
  );
};
