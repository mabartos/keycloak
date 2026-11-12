<#-- Use <@localeSelector.selector/> for the login header's language menu. -->
<#macro selector>
  <#if realm.internationalizationEnabled && locale.supported?size gt 1>
    <div class="${properties.kcLoginMainHeaderUtilities!}">
      <div id="kc-locale" class="${properties.kcLocaleClass!}" data-menu-button-links>
        <button id="kc-current-locale-link" type="button" class="${properties.kcLocaleToggleClass!}"
                aria-label="${msg('languages')}: ${locale.current}"
                aria-haspopup="menu" aria-expanded="false" aria-controls="kc-locale-menu" title="${locale.current}">
          <span class="${properties.kcLocaleToggleTextClass!}">${locale.current}</span>
          <span class="${properties.kcLocaleToggleControlsClass!}">
            <span class="${properties.kcLocaleToggleIconClass!}">
              <svg class="${properties.kcSvgIconClass!}" viewBox="0 0 320 512" fill="currentColor"
                   aria-hidden="true" width="1em" height="1em">
                <path d="M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z"></path>
              </svg>
            </span>
          </span>
        </button>
        <div id="kc-locale-dropdown" class="${properties.kcLocaleMenuClass!}" data-menu-popup hidden>
          <div class="${properties.kcLocaleMenuContentClass!}">
            <ul id="kc-locale-menu" class="${properties.kcLocaleMenuListClass!}" role="menu" aria-labelledby="kc-current-locale-link">
              <#list locale.supported as l>
                <li class="${properties.kcLocaleMenuListItemClass!}" role="none">
                  <a class="${properties.kcLocaleMenuItemClass!}<#if l.languageTag == locale.currentLanguageTag> ${properties.kcLocaleMenuItemCurrentClass!}</#if>"
                     href="${l.url}" hreflang="${l.languageTag}" role="menuitem" tabindex="-1"<#if l.languageTag == locale.currentLanguageTag> aria-current="true"</#if>>
                    <span class="${properties.kcLocaleMenuItemTextClass!}">${l.label}</span>
                  </a>
                </li>
              </#list>
            </ul>
          </div>
        </div>
      </div>
    </div>
    <script type="module" src="${url.resourcesPath}/js/menu-button-links.js"></script>
  </#if>
</#macro>
