import {
  KeycloakSelect,
  SelectVariant,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";

export function ScopesSelect({
  field,
  value,
  suggestions,
  onChange,
}: {
  field: "advertised" | "allowed";
  value: string[];
  suggestions: string[];
  onChange: (value: string[]) => void;
}) {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { hasSomeAccess } = useAccess();
  const canList = hasSomeAccess(
    "query-clients",
    "view-clients",
    "manage-clients",
  );
  const { addError } = useAlerts();
  const [realmScopes, setRealmScopes] = useState<string[]>([]);
  const [open, setOpen] = useState(false);
  const [filter, setFilter] = useState("");
  useFetch(
    async () => {
      if (!canList) return [];
      try {
        const scopes = await adminClient.clientScopes.find({ realm });
        return scopes.flatMap((scope) =>
          scope.protocol === "openid-connect" && scope.name ? [scope.name] : [],
        );
      } catch (error) {
        addError("agentRealmScopeSuggestionsError", error);
        return [];
      }
    },
    setRealmScopes,
    [adminClient, realm, canList],
  );

  const options = [
    ...new Set([
      ...(field === "allowed" ? ["openid"] : []),
      ...suggestions,
      ...value,
      ...realmScopes,
    ]),
  ].sort();
  const query = filter.trim();
  const matching = options.filter((scope) =>
    scope.toLowerCase().includes(query.toLowerCase()),
  );
  // OAuth scope-token characters, RFC 6749 section 3.3.
  const canAdd =
    /^[\x21\x23-\x5b\x5d-\x7e]{1,128}$/.test(query) && !options.includes(query);

  const id = field + "-scopes";
  const label = t(
    field === "advertised" ? "agentSupportedScopes" : "agentAllowedScopes",
  );
  const help = t(
    field === "advertised"
      ? "agentSupportedScopesHelp"
      : "agentAllowedScopesHelp",
  );
  return (
    <FormGroup
      fieldId={id}
      label={label}
      onKeyDownCapture={(event) => {
        if (open && event.key === "Escape") event.preventDefault();
      }}
    >
      <KeycloakSelect
        toggleId={id}
        variant={SelectVariant.typeaheadMulti}
        typeAheadAriaLabel={label}
        placeholderText={t("agentChooseScope")}
        maxHeight="16rem"
        direction="up"
        isOpen={open}
        onToggle={setOpen}
        onFilter={(text) => {
          setFilter(text);
          if (text) setOpen(true);
        }}
        selections={value}
        onSelect={(item) => {
          if (typeof item !== "string" || !item) return;
          onChange(
            value.includes(item)
              ? value.filter((scope) => scope !== item)
              : [...value, item],
          );
        }}
      >
        {matching.map((scope) => (
          <SelectOption
            key={scope}
            value={scope}
            isSelected={value.includes(scope)}
          >
            {scope}
          </SelectOption>
        ))}
        {canAdd && (
          <SelectOption value={query}>
            {t("agentAddScope", { scope: query })}
          </SelectOption>
        )}
      </KeycloakSelect>
      <small>{help}</small>
    </FormGroup>
  );
}
