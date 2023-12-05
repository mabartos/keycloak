package org.keycloak.config;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class Option<T> {

    private final Class<T> type;
    private final String key;
    private final OptionCategory category;
    private final BooleanSupplier enabled;
    private final boolean hidden;
    private final boolean buildTime;
    private final String description;
    private final Optional<T> defaultValue;
    private final Supplier<List<String>> expectedValues;

    public Option(Class<T> type, String key, OptionCategory category, BooleanSupplier enabled, boolean hidden, boolean buildTime, String description, Optional<T> defaultValue, Supplier<List<String>> expectedValues) {
        this.type = type;
        this.key = key;
        this.category = category;
        this.enabled = enabled;
        this.hidden = hidden;
        this.buildTime = buildTime;
        this.description = getDescriptionByCategorySupportLevel(description);
        this.defaultValue = defaultValue;
        this.expectedValues = expectedValues;
    }

    public Option(Class<T> type, String key, OptionCategory category, boolean enabled, boolean hidden, boolean buildTime, String description, Optional<T> defaultValue, Supplier<List<String>> expectedValues) {
        this(type, key, category, () -> enabled, hidden, buildTime, description, defaultValue, expectedValues);
    }

    public Class<T> getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled.getAsBoolean();
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isBuildTime() {
        return buildTime;
    }

    public String getKey() {
        return key;
    }

    public OptionCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Optional<T> getDefaultValue() {
        return defaultValue;
    }

    public List<String> getExpectedValues() {
        return expectedValues.get();
    }

    public Option<T> withRuntimeSpecificDefault(T defaultValue) {
        return new Option<>(
                this.type,
                this.key,
                this.category,
                this.enabled,
                this.hidden,
                this.buildTime,
                this.description,
                Optional.ofNullable(defaultValue),
                this.expectedValues
        );
    }

    private String getDescriptionByCategorySupportLevel(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }

        description = switch (this.getCategory().getSupportLevel()) {
            case PREVIEW -> "Preview: " + description;
            case EXPERIMENTAL -> "Experimental: " + description;
            default -> description;
        };

        return description;
    }
}
