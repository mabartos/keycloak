package org.keycloak.quarkus.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import org.keycloak.theme.ClasspathThemeProviderFactory;

import java.util.List;

public final class ThemesBuildItem extends SimpleBuildItem {

    final List<ClasspathThemeProviderFactory.ThemesRepresentation> themes;

    public ThemesBuildItem(List<ClasspathThemeProviderFactory.ThemesRepresentation> themes) {
        this.themes = themes;
    }

    public List<ClasspathThemeProviderFactory.ThemesRepresentation> getThemes() {
        return themes;
    }
}
