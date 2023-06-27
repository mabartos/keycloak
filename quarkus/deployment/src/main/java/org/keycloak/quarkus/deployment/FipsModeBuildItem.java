package org.keycloak.quarkus.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import org.keycloak.common.crypto.FipsMode;

public final class FipsModeBuildItem extends SimpleBuildItem {

    final FipsMode fipsMode;

    public FipsModeBuildItem(FipsMode fipsMode) {
        this.fipsMode = fipsMode;
    }

    public FipsMode getFipsMode() {
        return fipsMode;
    }
}
