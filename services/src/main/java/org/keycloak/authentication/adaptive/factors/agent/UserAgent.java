package org.keycloak.authentication.adaptive.factors.agent;

import java.util.Set;

public interface UserAgent {

    String getName();

    UserAgent MOZILLA = () -> "Mozilla";
    UserAgent CHROME = () -> "Chrome";
    UserAgent SAFARI = () -> "Safari";

    Set<UserAgent> knownAgents = Set.of(MOZILLA, CHROME, SAFARI);
}
