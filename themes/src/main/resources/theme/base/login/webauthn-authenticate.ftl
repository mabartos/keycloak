    <#import "template.ftl" as layout>
    <@layout.registrationLayout; section>
    <#if section = "title">
     title
    <#elseif section = "header">
        ${kcSanitize(msg("webauthn-login-title"))?no_esc}
    <#elseif section = "form">
        <form id="webauth" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
                <input type="hidden" id="authenticatorData" name="authenticatorData"/>
                <input type="hidden" id="signature" name="signature"/>
                <input type="hidden" id="credentialId" name="credentialId"/>
                <input type="hidden" id="userHandle" name="userHandle"/>
                <input type="hidden" id="error" name="error"/>
            </div>
        </form>

        <#if authenticators??>
            <form id="kc-webauthn-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
                  method="post">
                <div class="${properties.kcSelectAuthListClass!}">
                    <#list authenticators.authenticators as authenticator>
                        <div class="${properties.kcSelectAuthListItemClass!} kc-webauthn-authenticator"
                             onclick="webAuthnAuthenticate('${authenticator.credentialId}')">

                            <div class="${properties.kcSelectAuthListItemIconClass!}">
                                <i class="${properties.kcWebAuthnKeyIcon} fa-2x"></i>
                            </div>
                            <div class="${properties.kcSelectAuthListItemBodyClass!}">
                                <div class="${properties.kcSelectAuthListItemHeadingClass!}">
                                    ${msg('${authenticator.label}')}
                                </div>
                                <div class="${properties.kcSelectAuthListItemDescriptionClass!}">
                                    ${msg('webauthn-createdAt-label')} ${authenticator.createdAt}
                                </div>
                            </div>
                            <div class="${properties.kcSelectAuthListItemFillClass!}"></div>
                            <div class="${properties.kcSelectAuthListItemArrowClass!}">
                                <i class="${properties.kcSelectAuthListItemArrowIconClass!}"></i>
                            </div>
                        </div>
                    </#list>
                </div>
            </form>
            <#if authenticators.authenticators?size == 1>
                <form class="${properties.kcFormClass!}">
                    <div class="${properties.kcFormGroupClass!}">
                        <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                            <input id="authenticateWebAuthnButton" type="button" onclick="webAuthnAuthenticate()"
                                   value="${kcSanitize(msg("webauthn-doAuthenticate"))}"
                                   class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}">
                        </div>
                    </div>
                </form>
            </#if>

        <#else>
            <script>
                window.onload = function () {
                    webAuthnAuthenticate();
                };
            </script>
        </#if>

        <script type="text/javascript" src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js"></script>
        <script type="text/javascript" src="${url.resourcesPath}/js/base64url.js"></script>
        <script type="text/javascript">
            function webAuthnAuthenticate(credential) {
                let isUserIdentified = ${isUserIdentified};
                if (!isUserIdentified) {
                    doAuthenticate([]);
                    return;
                }
                checkAllowCredential(credential);
            }

            function checkAllowCredential(credential) {
                let allowCredentials = [];
                if (credential) {
                    allowCredentials.push({
                        id: base64url.decode(credential, {loose: true}),
                        type: 'public-key',
                    });
                }
                doAuthenticate(allowCredentials);
            }

            function doAuthenticate(allowCredentials) {

                // Check if WebAuthn is supported by this browser
                if (!window.PublicKeyCredential) {
                    $("#error").val("${msg("webauthn-unsupported-browser-text")?no_esc}");
                    $("#webauth").submit();
                    return;
                }

                let challenge = "${challenge}";
                let userVerification = "${userVerification}";
                let rpId = "${rpId}";
                let publicKey = {
                    rpId: rpId,
                    challenge: base64url.decode(challenge, {loose: true})
                };

                let createTimeout = ${createTimeout};
                if (createTimeout !== 0) publicKey.timeout = createTimeout * 1000;

                if (allowCredentials.length) {
                    publicKey.allowCredentials = allowCredentials;
                }

                if (userVerification !== 'not specified') publicKey.userVerification = userVerification;

                navigator.credentials.get({publicKey})
                    .then((result) => {
                        window.result = result;

                        let clientDataJSON = result.response.clientDataJSON;
                        let authenticatorData = result.response.authenticatorData;
                        let signature = result.response.signature;

                        $("#clientDataJSON").val(base64url.encode(new Uint8Array(clientDataJSON), {pad: false}));
                        $("#authenticatorData").val(base64url.encode(new Uint8Array(authenticatorData), {pad: false}));
                        $("#signature").val(base64url.encode(new Uint8Array(signature), {pad: false}));
                        $("#credentialId").val(result.id);
                        if (result.response.userHandle) {
                            $("#userHandle").val(base64url.encode(new Uint8Array(result.response.userHandle), {pad: false}));
                        }
                        $("#webauth").submit();
                    })
                    .catch((err) => {
                        $("#error").val(err);
                        $("#webauth").submit();
                    })
                ;
            }

        </script>
    <#elseif section = "info">

    </#if>
    </@layout.registrationLayout>
