package org.keycloak.events.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.sql.Date;

@Entity
@Table(name = "MFA_ENROLLMENT_DAILY", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"STAT_DATE", "REALM_ID", "CREDENTIAL_TYPE"})
})
public class MfaEnrollmentDailyEntity {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "STAT_DATE", nullable = false)
    private Date statDate;

    @Column(name = "REALM_ID", length = 255, nullable = false)
    private String realmId;

    @Column(name = "CREDENTIAL_TYPE", length = 255, nullable = false)
    private String credentialType;

    @Column(name = "ENROLLMENT_COUNT", nullable = false)
    private long enrollmentCount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getStatDate() {
        return statDate;
    }

    public void setStatDate(Date statDate) {
        this.statDate = statDate;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public long getEnrollmentCount() {
        return enrollmentCount;
    }

    public void setEnrollmentCount(long enrollmentCount) {
        this.enrollmentCount = enrollmentCount;
    }
}
