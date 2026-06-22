package org.keycloak.events.jpa.analytics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.jpa.AuthStatsDailyEntity;

public class AnalyticsEventListenerProvider implements EventListenerProvider {

    private static final String NONE_SENTINEL = "__none__";

    private static final Set<EventType> TRACKED_EVENTS = EnumSet.of(
            EventType.LOGIN,
            EventType.LOGIN_ERROR,
            EventType.REGISTER,
            EventType.REGISTER_ERROR,
            EventType.UPDATE_CREDENTIAL,
            EventType.UPDATE_CREDENTIAL_ERROR,
            EventType.REMOVE_CREDENTIAL,
            EventType.REMOVE_CREDENTIAL_ERROR
    );

    private final EntityManager em;

    public AnalyticsEventListenerProvider(EntityManager em) {
        this.em = em;
    }

    @Override
    public void onEvent(Event event) {
        EventType eventType = event.getType();
        if (eventType == null || !TRACKED_EVENTS.contains(eventType)) {
            return;
        }

        String realmId = event.getRealmId();
        if (realmId == null) {
            return;
        }

        Map<String, String> details = event.getDetails();
        String credentialType = NONE_SENTINEL;
        if (details != null) {
            String ct = details.get(Details.CREDENTIAL_TYPE);
            if (ct != null && !ct.isEmpty()) {
                credentialType = ct;
            }
        }

        Date today = Date.valueOf(LocalDate.now(ZoneOffset.UTC));
        String eventTypeName = eventType.name();

        TypedQuery<AuthStatsDailyEntity> query = em.createQuery(
                "SELECT e FROM AuthStatsDailyEntity e WHERE e.statDate = :statDate AND e.realmId = :realmId " +
                        "AND e.eventType = :eventType AND e.credentialType = :credentialType",
                AuthStatsDailyEntity.class);
        query.setParameter("statDate", today);
        query.setParameter("realmId", realmId);
        query.setParameter("eventType", eventTypeName);
        query.setParameter("credentialType", credentialType);

        List<AuthStatsDailyEntity> results = query.getResultList();
        if (results.isEmpty()) {
            AuthStatsDailyEntity entity = new AuthStatsDailyEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setStatDate(today);
            entity.setRealmId(realmId);
            entity.setEventType(eventTypeName);
            entity.setCredentialType(credentialType);
            entity.setEventCount(1);
            em.persist(entity);
        } else {
            AuthStatsDailyEntity entity = results.get(0);
            entity.setEventCount(entity.getEventCount() + 1);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Admin events are not tracked in analytics for now
    }

    @Override
    public void close() {
        // no-op
    }
}
