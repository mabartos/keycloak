package org.keycloak.admin.api;

import jakarta.ws.rs.BadRequestException;
import org.keycloak.models.KeycloakSession;

public class AdminApiProvider {

    public static <T extends AdminApi> T realmsApi(KeycloakSession session, Class<T> apiClass) {
        return realmsApi(session, apiClass, 2);
    }

    public static <T extends AdminApi> T realmsApi(KeycloakSession session, Class<T> apiClass, String version) {
        if (version == null || !version.matches("^v\\d+$")) {
            throw new BadRequestException("Wrong version format. Expected 'v' followed by an integer, e.g. v1, v2");
        }

        int intVersion;
        try {
            intVersion = Integer.parseInt(version.substring(1));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Wrong version value");
        }
        return realmsApi(session, apiClass, intVersion);
    }

    public static <T extends AdminApi> T realmsApi(KeycloakSession session, Class<T> apiClass, int version) {
        return session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(apiClass)
                .filter(f -> ((AdminApiFactory<?>) f).getVersion() == version)
                .findFirst()
                .map(f -> (T) f.create(session))
                .orElseThrow();
    }
}
