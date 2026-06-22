package org.keycloak.admin.ui.rest;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.ui.rest.model.AuthStatsRepresentation;
import org.keycloak.admin.ui.rest.model.EnrollmentStatsRepresentation;
import org.keycloak.admin.ui.rest.model.UserStatsRepresentation;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.jpa.AuthStatsDailyEntity;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;

public class AnalyticsResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public AnalyticsResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }

    @GET
    @Path("/user-stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get user status breakdown", description = "Returns counts of active, disabled, and total users")
    public UserStatsRepresentation getUserStats() {
        auth.realm().requireViewRealm();

        int total = session.users().getUsersCount(realm, false);
        int disabled = session.users().getUsersCount(realm, Map.of(UserModel.ENABLED, "false"));

        Long clientsCount = realm.getClientsCount();

        UserStatsRepresentation stats = new UserStatsRepresentation();
        stats.setTotal(total);
        stats.setActive(total - disabled);
        stats.setInactive(disabled);
        stats.setLocked(0);
        stats.setClients(clientsCount != null ? clientsCount : 0);
        return stats;
    }

    @GET
    @Path("/auth-stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get authentication statistics", description = "Returns daily authentication event counts from rollup tables")
    public List<AuthStatsRepresentation> getAuthStats(
            @QueryParam("from") String from,
            @QueryParam("to") String to) {
        auth.realm().requireViewEvents();

        LocalDate fromDate = parseDate(from, LocalDate.now().minusDays(30));
        LocalDate toDate = parseDate(to, LocalDate.now());

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        TypedQuery<AuthStatsDailyEntity> query = em.createQuery(
                "SELECT e FROM AuthStatsDailyEntity e WHERE e.realmId = :realmId " +
                        "AND e.statDate BETWEEN :fromDate AND :toDate ORDER BY e.statDate",
                AuthStatsDailyEntity.class);
        query.setParameter("realmId", realm.getId());
        query.setParameter("fromDate", Date.valueOf(fromDate));
        query.setParameter("toDate", Date.valueOf(toDate));

        return query.getResultStream()
                .map(AnalyticsResource::toAuthStatsRepresentation)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/enrollment-stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get MFA enrollment statistics", description = "Returns current credential enrollment counts by type")
    public List<EnrollmentStatsRepresentation> getEnrollmentStats() {
        auth.realm().requireViewEvents();

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        List<Object[]> results = em.createQuery(
                        "SELECT c.type, COUNT(c) FROM CredentialEntity c WHERE c.user.realmId = :realmId GROUP BY c.type",
                        Object[].class)
                .setParameter("realmId", realm.getId())
                .getResultList();

        return results.stream()
                .map(row -> {
                    EnrollmentStatsRepresentation rep = new EnrollmentStatsRepresentation();
                    rep.setCredentialType((String) row[0]);
                    rep.setCount((Long) row[1]);
                    return rep;
                })
                .collect(Collectors.toList());
    }

    private static AuthStatsRepresentation toAuthStatsRepresentation(AuthStatsDailyEntity entity) {
        AuthStatsRepresentation rep = new AuthStatsRepresentation();
        rep.setDate(entity.getStatDate().toLocalDate().toString());
        rep.setEventType(entity.getEventType());
        rep.setCredentialType(entity.getCredentialType());
        rep.setCount(entity.getEventCount());
        return rep;
    }

    private static LocalDate parseDate(String value, LocalDate defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return defaultValue;
        }
    }
}
