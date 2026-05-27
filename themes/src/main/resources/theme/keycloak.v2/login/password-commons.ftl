<#import "field.ftl" as field>
<#macro logoutOtherSessions>
    <div id="kc-form-options" class="${properties.kcFormGroupClass!}">
        <@field.checkbox name="logout-sessions" label=msg("logoutOtherSessions") value=false />
    </div>
</#macro>
