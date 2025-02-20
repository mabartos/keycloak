import {
    AlertVariant,
    Button,
    ButtonVariant,
    Dropdown,
    DropdownItem,
    DropdownList,
    MenuToggle,
    ToolbarItem
} from "@patternfly/react-core";
import {useTranslation} from "react-i18next";
import {Action, KeycloakDataTable, ListEmptyState, useAlerts} from "@keycloak/keycloak-ui-shared";
import {emptyFormatter} from "../../util";
import {useAdminClient} from "../../admin-client";
import {useState} from "react";
import {EllipsisVIcon} from "@patternfly/react-icons";
import {useConfirmDialog} from "../../components/confirm-dialog/ConfirmDialog";

export type EventType = {
    id: string;
};

type EventsTypeTableProps = {
    ariaLabelKey?: string;
    eventTypes: string[];
    addTypes?: () => void;
    onSelect?: (value: EventType[]) => void;
    onDelete?: (value: EventType) => void;
};

export function EventsTypeTable({
                                    ariaLabelKey = "userEventsRegistered",
                                    eventTypes,
                                    addTypes,
                                    onSelect,
                                    onDelete,
                                }: EventsTypeTableProps) {
    const {t} = useTranslation();
    const [selectedEvents, setSelectedEvents] = useState<
        EventType[]
    >([]);
    const data = eventTypes.map((type) => ({
        id: type,
        name: t(`eventTypes.${type}.name`),
        description: t(`eventTypes.${type}.description`),
    }));
    const {adminClient} = useAdminClient();
    const {addAlert, addError} = useAlerts();
    const [kebabOpen, setKebabOpen] = useState(false);

    const refresh = () => {
        setSelectedEvents([]);
    };

    const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
        titleKey: t("deleteEventsType", {
            count: selectedEvents.length,
        }),
        messageKey: "deleteConfirmEventType",
        continueButtonLabel: "delete",
        continueButtonVariant: ButtonVariant.danger,
        onConfirm: async () => {
            const clientScopes = await adminClient.clientScopes.find();
            const clientScopeLength = Object.keys(clientScopes).length;
            if (clientScopeLength - selectedEvents.length > 0) {
                try {
                    for (const event of selectedEvents) {
                        try {
                            //await onDelete(event);
                        } catch (error: any) {
                            console.warn(
                                "could not remove scope",
                                error.response?.data?.errorMessage || error,
                            );
                        }
                        await adminClient.clientScopes.del({id: event.id!});
                    }
                    addAlert(t("deletedSuccessClientScope"), AlertVariant.success);
                    refresh();
                } catch (error) {
                    addError("deleteErrorClientScope", error);
                }
            } else {
                addAlert(t("notAllowedToDeleteAllClientScopes"), AlertVariant.danger);
            }
        },
    });

    return (
        <>
            <DeleteConfirm/>
            <KeycloakDataTable
                ariaLabelKey={ariaLabelKey}
                searchPlaceholderKey="searchEventType"
                loader={data}
                onSelect={(eventTypes) => setSelectedEvents([...eventTypes])}
                canSelectAll
                isPaginated
                toolbarItem={
                    <>
                        {addTypes && (
                            <ToolbarItem>
                                <Button id="addTypes" onClick={addTypes} data-testid="addTypes">
                                    {t("addSavedTypes")}
                                </Button>
                            </ToolbarItem>
                        )}

                        <ToolbarItem>
                            <Dropdown
                                shouldFocusToggleOnSelect
                                onOpenChange={(isOpen) => setKebabOpen(isOpen)}
                                toggle={(ref) => (
                                    <MenuToggle
                                        data-testid="kebab"
                                        aria-label="Kebab toggle"
                                        ref={ref}
                                        onClick={() => setKebabOpen(!kebabOpen)}
                                        variant="plain"
                                    >
                                        <EllipsisVIcon/>
                                    </MenuToggle>
                                )}
                                isOpen={kebabOpen}
                            >
                                <DropdownList>
                                    <DropdownItem
                                        data-testid="delete"
                                        isDisabled={selectedEvents.length === 0}
                                        onClick={() => {
                                            toggleDeleteDialog();
                                            setKebabOpen(false);
                                        }}
                                    >
                                        {t("delete")}
                                    </DropdownItem>
                                </DropdownList>
                            </Dropdown>
                        </ToolbarItem>
                    </>
                }
                actions={
                    !onDelete
                        ? []
                        : [
                            {
                                title: t("remove"),
                                onRowClick: onDelete,
                            } as Action<EventType>,
                        ]
                }
                columns={[
                    {
                        name: "name",
                        displayKey: "eventType",
                    },
                    {
                        name: "description",
                        cellFormatters: [emptyFormatter()],
                    },
                ]}
                emptyState={
                    <ListEmptyState
                        message={t("emptyEvents")}
                        instructions={t("emptyEventsInstructions")}
                    />
                }
            />
        </>
    );
}
