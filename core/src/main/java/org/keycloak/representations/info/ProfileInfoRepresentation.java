/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.representations.info;

import org.keycloak.common.Profile;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ProfileInfoRepresentation {

    private String name;
    private List<String> disabledFeatures;
    @Deprecated
    private List<String> previewFeatures;
    @Deprecated
    private List<String> experimentalFeatures;

    private Map<String, String> allFeatures;

    public static ProfileInfoRepresentation create() {
        ProfileInfoRepresentation info = new ProfileInfoRepresentation();

        info.name = Profile.getName();

        info.disabledFeatures = names(Profile.getDisabledFeatures());
        info.previewFeatures = names(Profile.getPreviewFeatures());
        info.experimentalFeatures = names(Profile.getExperimentalFeatures());

        info.allFeatures = gatherAllFeatures(info);

        return info;
    }

    private static Map<String, String> gatherAllFeatures(ProfileInfoRepresentation info) {
        Map<String, String> features = new HashMap<>();

        info.previewFeatures.forEach(f -> features.put(f, Profile.Type.PREVIEW.name()));
        info.experimentalFeatures.forEach(f -> features.put(f, Profile.Type.EXPERIMENTAL.name()));

        return features;
    }

    public String getName() {
        return name;
    }

    public List<String> getDefaultFeatures() {
        return defaultFeatures;
    }

    public List<String> getDisabledFeatures() {
        return disabledFeatures;
    }

    public List<String> getPreviewFeatures() {
        return previewFeatures;
    }

    public List<String> getExperimentalFeatures() {
        return experimentalFeatures;
    }

    public List<String> getDeprecatedFeatures() {
        return deprecatedFeatures;
    }

    public Map<String, String> getAllFeatures() {
        return allFeatures;
    }

    private static List<String> names(Set<Profile.Feature> featureSet) {
        List<String> l = new LinkedList<>();
        for (Profile.Feature f : featureSet) {
            l.add(f.name());
        }
        return l;
    }

}
