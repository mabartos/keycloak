<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("registerTitle")}
    <#elseif section = "form">
        <form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">
            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('firstName',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="firstName" class="${properties.kcLabelClass!}">${msg("firstName")}</label>
                    <span class="pf-c-form__label-required required" aria-hidden="true">&#42;</span>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <#if message?has_content && message.summary?contains('${msg("missingFirstNameMessage")}')>
                        <input type="text" id="firstName" class="${properties.kcInputClass!}" name="firstName"
                               value="${(register.formData.firstName!'')}" aria-invalid="true"/>
                        <span class="pf-c-form__helper-text pf-m-error required"
                              aria-live="polite">${kcSanitize(msg('missingFirstNameMessage'))?no_esc}</span>
                    <#else>
                        <input type="text" id="firstName" class="${properties.kcInputClass!}" name="firstName"
                               value="${(register.formData.firstName!'')}"/>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('lastName',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="lastName" class="${properties.kcLabelClass!}">${msg("lastName")}</label>
                    <span class="pf-c-form__label-required required" aria-hidden="true">&#42;</span>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <#if message?has_content && message.summary?contains('${msg("missingLastNameMessage")}')>
                        <input type="text" id="lastName" class="${properties.kcInputClass!}" name="lastName"
                               value="${(register.formData.lastName!'')}" aria-invalid="true"/>
                        <span class="pf-c-form__helper-text pf-m-error required"
                              aria-live="polite">${kcSanitize(msg('missingLastNameMessage'))?no_esc}</span>
                    <#else>
                        <input type="text" id="lastName" class="${properties.kcInputClass!}" name="lastName"
                               value="${(register.formData.lastName!'')}"/>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('email',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="email" class="${properties.kcLabelClass!}">${msg("email")}</label>
                    <span class="pf-c-form__label-required required" aria-hidden="true">&#42;</span>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <#if message?has_content && (message.summary?contains('${msg("invalidEmailMessage")}') || message.summary?contains('${msg("missingEmailMessage")}'))>
                        <input type="text" id="email" class="${properties.kcInputClass!}" name="email"
                               value="${(register.formData.email!'')}" autocomplete="email"
                               aria-invalid="true"
                        />
                        <span class="pf-c-form__helper-text pf-m-error required"
                              aria-live="polite">
                              <#if message.summary?contains('${msg("invalidEmailMessage")}')>
                                  ${kcSanitize(msg('invalidEmailMessage'))?no_esc}
                              <#elseif message.summary?contains('${msg("missingEmailMessage")}')>
                                  ${kcSanitize(msg('missingEmailMessage'))?no_esc}
                              </#if>
                              </span>
                    <#else>
                        <input type="text" id="email" class="${properties.kcInputClass!}" name="email"
                               value="${(register.formData.email!'')}" autocomplete="email"/>
                    </#if>
                </div>
            </div>

            <#if !realm.registrationEmailAsUsername>
                <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('username',properties.kcFormGroupErrorClass!)}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="username" class="${properties.kcLabelClass!}">${msg("username")}</label>
                        <span class="pf-c-form__label-required required" aria-hidden="true">&#42;</span>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <#if message?has_content && message.summary?contains('${msg("missingUsernameMessage")}')>
                            <input type="text" id="username" class="${properties.kcInputClass!}" name="username"
                                   value="${(register.formData.username!'')}" autocomplete="username"
                                   aria-invalid="true"/>
                            <span class="pf-c-form__helper-text pf-m-error required"
                                  aria-live="polite">${kcSanitize(msg('missingUsernameMessage'))?no_esc}</span>
                        <#else>
                            <input type="text" id="username" class="${properties.kcInputClass!}" name="username"
                                   value="${(register.formData.username!'')}" autocomplete="username"/>
                        </#if>
                    </div>
                </div>
            </#if>

            <#if passwordRequired??>
                <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('password',properties.kcFormGroupErrorClass!)}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
                        <span class="pf-c-form__label-required required" aria-hidden="true">&#42;</span>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <#if message?has_content && message.summary?contains('${msg("missingPasswordMessage")}')>
                            <input type="password" id="password" class="${properties.kcInputClass!}" name="password"
                                   autocomplete="new-password"
                                   aria-invalid="true"
                            />
                            <span class="pf-c-form__helper-text pf-m-error required"
                                  aria-live="polite">
                                  ${kcSanitize(msg('missingPasswordMessage'))?no_esc}
                              </span>
                        <#else>
                            <input type="password" id="password" class="${properties.kcInputClass!}" name="password"
                                   autocomplete="new-password"/>
                        </#if>
                    </div>
                </div>

                <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('password-confirm',properties.kcFormGroupErrorClass!)}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="password-confirm" class="${properties.kcLabelClass!}">${msg("passwordConfirm")}</label>
                        <span class="pf-c-form__label-required required" aria-hidden="true">&#42;</span>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <#if message?has_content && message.summary?contains('${msg("invalidPasswordConfirmMessage")}')>
                            <input type="password" id="password-confirm" class="${properties.kcInputClass!}"
                                   name="password-confirm"
                                   aria-invalid="true"
                            />
                            <span class="pf-c-form__helper-text pf-m-error required"
                                  aria-live="polite">
                                  ${kcSanitize(msg('invalidPasswordConfirmMessage'))?no_esc}
                              </span>
                        <#else>
                            <input type="password" id="password-confirm" class="${properties.kcInputClass!}"
                                   name="password-confirm"/>
                        </#if>
                    </div>
                </div>
            </#if>

            <#if recaptchaRequired??>
            <div class="form-group">
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
                </div>
            </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <span><a href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doRegister")}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
