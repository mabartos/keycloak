package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.Option;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.keycloak.config.Option.WILDCARD_PLACEHOLDER_PATTERN;
import static org.keycloak.quarkus.runtime.cli.Picocli.ARG_PREFIX;

public class WildcardPropertyMapper<T> extends PropertyMapper<T> {
    private static final Function<Character, String> MATCHER_REPLACEMENT = (delimiter) -> String.format("([\\\\\\\\a-zA-Z0-9%c]+)", delimiter);
    private final Matcher fromWildcardMatcher;
    private final Pattern fromWildcardPattern;
    private final Pattern envVarNameWildcardPattern;
    private Matcher toWildcardMatcher;
    private Pattern toWildcardPattern;

    private final Character allowedWildcardDelimiter;
    private final BiFunction<String, Set<String>, Set<String>> wildcardKeysTransformer;
    private final ValueMapper wildcardMapFrom;

    public WildcardPropertyMapper(Option<T> option, String to, BooleanSupplier enabled, String enabledWhen,
            BiFunction<String, ConfigSourceInterceptorContext, String> mapper,
            String mapFrom, BiFunction<String, ConfigSourceInterceptorContext, String> parentMapper,
            String paramLabel, boolean mask, BiConsumer<PropertyMapper<T>, ConfigValue> validator,
            String description, BooleanSupplier required, String requiredWhen,
   Matcher fromWildcardMatcher, BiFunction<String, Set<String>, Set<String>> wildcardKeysTransformer, ValueMapper wildcardMapFrom, Character allowedWildcardDelimiter)
    {
        super(option, to, enabled, enabledWhen, mapper, mapFrom, parentMapper, paramLabel, mask, validator, description, required, requiredWhen, null);
        this.wildcardMapFrom = wildcardMapFrom;
        this.fromWildcardMatcher = fromWildcardMatcher;
        this.allowedWildcardDelimiter = allowedWildcardDelimiter != null ? allowedWildcardDelimiter : '.';
        // Includes handling for both "--" prefix for CLI options and "kc." prefix
        this.fromWildcardPattern = Pattern.compile("(?:" + ARG_PREFIX + "|kc\\.)" + fromWildcardMatcher.replaceFirst(MATCHER_REPLACEMENT.apply(allowedWildcardDelimiter)));

        // Not using toEnvVarFormat because it would process the whole string incl the <...> wildcard.
        Matcher envVarMatcher = WILDCARD_PLACEHOLDER_PATTERN.matcher(option.getKey().toUpperCase().replace("-", "_"));
        this.envVarNameWildcardPattern = Pattern.compile("KC_" + envVarMatcher.replaceFirst("([_A-Z0-9]+)"));

        if (to != null) {
            toWildcardMatcher = WILDCARD_PLACEHOLDER_PATTERN.matcher(to);
            if (!toWildcardMatcher.find()) {
                throw new IllegalArgumentException("Attempted to map a wildcard option to a non-wildcard option");
            }

            this.toWildcardPattern = Pattern.compile(toWildcardMatcher.replaceFirst(MATCHER_REPLACEMENT.apply(allowedWildcardDelimiter)));
        }

        this.wildcardKeysTransformer = wildcardKeysTransformer;
        //this.mapFrom = mapFrom != null ? getMappedKey(mapFrom).map(this::getFrom).orElse(null) : null;
    }

    @Override
    public boolean hasWildcard() {
        return true;
    }

    String getTo(String wildcardKey) {
        return toWildcardMatcher != null ? toWildcardMatcher.replaceFirst(wildcardKey) : null;
    }

    String getFrom(String wildcardKey) {
        return MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + fromWildcardMatcher.replaceFirst(wildcardKey);
    }

    public Stream<String> getToFromWildcardTransformer(String value) {
        if (wildcardKeysTransformer == null) {
            return Stream.empty();
        }
        return wildcardKeysTransformer.apply(value, new HashSet<String>()).stream().map(toWildcardMatcher::replaceFirst);
    }

    /**
     * Returns a mapped key for the given option name if a relevant mapping is available, or empty otherwise.
     * Currently, it only attempts to extract the wildcard key from the given option name.
     * E.g. for the option "log-level-<category>" and the option name "log-level-io.quarkus",
     * the wildcard value would be "io.quarkus".
     */
    Optional<String> getMappedKey(String originalKey) {
        Matcher matcher = fromWildcardPattern.matcher(originalKey);
        if (matcher.matches()) {
            return Optional.of(matcher).map(m -> m.group(1));
        }

        if (toWildcardPattern != null) {
            matcher = toWildcardPattern.matcher(originalKey);
            if (matcher.matches()) {
                return Optional.of(matcher).map(m -> m.group(1));
            }
        }

        matcher = envVarNameWildcardPattern.matcher(originalKey);
        if (matcher.matches()) {
            // we opiniotatedly convert env var names to CLI format with the allowed wildcard delimiter (f.e. dots or minus sings)
            return Optional.of(matcher).map(m -> m.group(1).toLowerCase().replace("_", allowedWildcardDelimiter.toString()));
        }

        return Optional.empty();
    }

    /**
     * Checks if the given option name matches the wildcard pattern of this option.
     * E.g. check if "log-level-io.quarkus" matches the wildcard pattern "log-level-<category>".
     */
    public boolean matchesWildcardOptionName(String name) {
        return getMappedKey(name).isPresent();
    }

    private PropertyMapper<?> forWildcardValue(final String wildcardValue) {
        String to = getTo(wildcardValue);
        String from = getFrom(wildcardValue);
        return new PropertyMapper<T>(this, from, to, wildcardMapFrom == null ? null : (v, context) -> wildcardMapFrom.map(wildcardValue, v, context));
    }

    @Override
    public PropertyMapper<?> forKey(String key) {
        return getMappedKey(key).map(this::forWildcardValue).orElseThrow();
    }

}
