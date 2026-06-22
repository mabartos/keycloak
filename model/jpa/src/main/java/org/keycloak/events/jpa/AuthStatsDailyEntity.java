package org.keycloak.events.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.sql.Date;

@Entity
@Table(name = "AUTH_STATS_DAILY", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"STAT_DATE", "REALM_ID", "EVENT_TYPE", "CREDENTIAL_TYPE"})
})
public class AuthStatsDailyEntity {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "STAT_DATE", nullable = false)
    private Date statDate;

    @Column(name = "REALM_ID", length = 255, nullable = false)
    private String realmId;

    @Column(name = "EVENT_TYPE", length = 255, nullable = false)
    private String eventType;

    @Column(name = "CREDENTIAL_TYPE", length = 255, nullable = false)
    private String credentialType;

    @Column(name = "EVENT_COUNT", nullable = false)
    private long eventCount;

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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public long getEventCount() {
        return eventCount;
    }

    public void setEventCount(long eventCount) {
        this.eventCount = eventCount;
    }
}
