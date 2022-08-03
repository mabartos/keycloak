package org.keycloak.testsuite.serverinfo;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.info.ProfileInfoRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FeaturesStateTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

    }

    @Test
    public void defaultFeatures() {
        testFeatures(ProfileInfoRepresentation::getDefaultFeatures, Profile.Type.DEFAULT);
    }

    @Test
    public void previewFeatures() {
        testFeatures(ProfileInfoRepresentation::getPreviewFeatures, Profile.Type.PREVIEW);
    }

    @Test
    public void experimentalFeatures() {
        testFeatures(ProfileInfoRepresentation::getExperimentalFeatures, Profile.Type.EXPERIMENTAL);
    }

    @Test
    public void disabledFeatures() {
        final ProfileInfoRepresentation profileInfo = adminClient.serverInfo().getInfo().getProfileInfo();
        assertThat(profileInfo, notNullValue());

        final Set<String> actual = new HashSet<>(profileInfo.getDisabledFeatures());
        final Set<String> expected = Profile.getDisabledFeatures().stream().map(Enum::name).collect(Collectors.toSet());
        assertThat(actual, is(expected));
        System.err.println(actual);
    }

    @Test
    public void deprecatedFeatures() {
        testFeatures(ProfileInfoRepresentation::getDeprecatedFeatures, Profile.Type.DEPRECATED);
    }

    public void testFeatures(Function<ProfileInfoRepresentation, List<String>> getFeatures, Profile.Type type) {
        final ProfileInfoRepresentation profileInfo = adminClient.serverInfo().getInfo().getProfileInfo();
        assertThat(profileInfo, notNullValue());

        final Set<String> actual = new HashSet<>(getFeatures.apply(profileInfo));
        final Set<String> expected = Arrays.stream(Profile.Feature.values())
                .filter(f -> type.equals(f.getTypeProject()))
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(actual, is(expected));
    }
}
