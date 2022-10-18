/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.operator.crds.v2alpha1.deployment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UnsupportedSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TransactionsSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpec;

import javax.validation.constraints.NotNull;
import java.util.List;

public class KeycloakSpec {

    @JsonPropertyDescription("Number of Keycloak instances in HA mode. Default is 1.")
    private int instances = 1;

    @JsonPropertyDescription("Custom Keycloak image to be used.")
    private String image;

    @JsonPropertyDescription("Secret(s) that might be used when pulling an image from a private container image registry or repository.")
    private List<LocalObjectReference> imagePullSecrets;

    @JsonPropertyDescription("Configuration of the Keycloak server.\n" +
            "expressed as a keys (reference: https://www.keycloak.org/server/all-config) and values that can be either direct values or references to secrets.")
    private List<ValueOrSecret> serverConfiguration; // can't use Set due to a bug in Sundrio https://github.com/sundrio/sundrio/issues/316

    @JsonProperty("http")
    @JsonPropertyDescription("In this section you can configure Keycloak features related to HTTP and HTTPS")
    private HttpSpec httpSpec;

    @JsonPropertyDescription("Disable the default ingress.")
    private boolean disableDefaultIngress;

    @JsonPropertyDescription(
            "In this section you can configure podTemplate advanced features, not production-ready, and not supported settings.\n" +
                    "Use at your own risk and open an issue with your use-case if you don't find an alternative way.")
    private UnsupportedSpec unsupported;

    @JsonProperty("features")
    @JsonPropertyDescription("In this section you can configure Keycloak features, which should be enabled/disabled.")
    private FeatureSpec featureSpec;

    @JsonProperty("transaction")
    @JsonPropertyDescription("In this section you can find all properties related to the settings of transaction behavior.")
    private TransactionsSpec transactionsSpec;

    @JsonProperty("hostname")
    @JsonPropertyDescription("In this section you can configure Keycloak hostname and related properties.")
    private HostnameSpec hostnameSpec;

    public HttpSpec getHttpSpec() {
        return httpSpec;
    }

    public void setHttpSpec(HttpSpec httpSpec) {
        this.httpSpec = httpSpec;
    }

    public void setDisableDefaultIngress(boolean value) {
        this.disableDefaultIngress = value;
    }

    public boolean isDisableDefaultIngress() {
        return this.disableDefaultIngress;
    }

    public UnsupportedSpec getUnsupported() {
        return unsupported;
    }

    public void setUnsupported(UnsupportedSpec unsupported) {
        this.unsupported = unsupported;
    }

    public FeatureSpec getFeatureSpec() {
        return featureSpec;
    }

    public void setFeatureSpec(FeatureSpec featureSpec) {
        this.featureSpec = featureSpec;
    }

    public TransactionsSpec getTransactionsSpec() {
        return transactionsSpec;
    }

    public void setTransactionsSpec(TransactionsSpec transactionsSpec) {
        this.transactionsSpec = transactionsSpec;
    }

    public HostnameSpec getHostnameSpec() {
        return hostnameSpec;
    }

    public void setHostnameSpec(HostnameSpec hostnameSpec) {
        this.hostnameSpec = hostnameSpec;
    }

    public int getInstances() {
        return instances;
    }

    public void setInstances(int instances) {
        this.instances = instances;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<LocalObjectReference> getImagePullSecrets() {
        return this.imagePullSecrets;
    }

    public void setImagePullSecrets(List<LocalObjectReference> imagePullSecrets) {
        this.imagePullSecrets = imagePullSecrets;
    }

    public List<ValueOrSecret> getServerConfiguration() {
        return serverConfiguration;
    }

    public void setServerConfiguration(List<ValueOrSecret> serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
    }
}