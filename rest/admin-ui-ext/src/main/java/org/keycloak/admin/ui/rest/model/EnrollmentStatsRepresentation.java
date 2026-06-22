package org.keycloak.admin.ui.rest.model;

public class EnrollmentStatsRepresentation {

    private String credentialType;
    private long count;

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
