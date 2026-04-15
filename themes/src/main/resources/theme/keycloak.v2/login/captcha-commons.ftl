<#function isInvisible>
    <#return (captchaRequired??) && !(captchaVisible!false)>
</#function>

<#macro widget>
    <#if captchaRequired?? && (captchaVisible!false)>
        <div class="form-group">
            <div class="${properties.kcInputWrapperClass!}">
                <div class="${captchaWidgetClass!}" data-size="compact" data-sitekey="${captchaSiteKey!}" data-action="${captchaAction!}"></div>
            </div>
        </div>
    </#if>
</#macro>

<#macro button formId label id="" name="" tabindex="" class="">
    <script>
        function onCaptchaSubmit(token) {
            document.getElementById("${formId}").requestSubmit();
        }
    </script>
    <button class="<#if class?has_content>${class}<#else>${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}</#if> ${captchaWidgetClass!}"
            <#if id?has_content>id="${id}"</#if>
            <#if name?has_content>name="${name}"</#if>
            <#if tabindex?has_content>tabindex="${tabindex}"</#if>
            data-sitekey="${captchaSiteKey!}"
            data-callback="onCaptchaSubmit"
            data-action="${captchaAction!}"
            type="submit">
        ${msg(label)}
    </button>
</#macro>

<#macro submitButton formId label>
    <#if isInvisible()>
        <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
            <#nested>
            <@button formId=formId label=label />
        </div>
    <#else>
        <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
            <#nested>
            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg(label)}"/>
        </div>
    </#if>
</#macro>
