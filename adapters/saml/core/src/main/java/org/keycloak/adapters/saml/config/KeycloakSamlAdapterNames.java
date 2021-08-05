package org.keycloak.adapters.saml.config;

public interface KeycloakSamlAdapterNames {
    String ALLOWED_CLOCK_SKEW = "AllowedClockSkew";
    String ATTRIBUTE = "Attribute";
    String CERTIFICATE = "Certificate";
    String CERTIFICATE_PEM = "CertificatePem";
    String HTTP_CLIENT = "HttpClient";
    String IDP = "IDP";
    String KEY = "Key";
    String KEYCLOAK_SAML_ADAPTER = "keycloak-saml-adapter";
    String KEYS = "Keys";
    String KEY_STORE = "KeyStore";
    String PRINCIPAL_NAME_MAPPING = "PrincipalNameMapping";
    String PRIVATE_KEY = "PrivateKey";
    String PRIVATE_KEY_PEM = "PrivateKeyPem";
    String PROPERTY = "Property";
    String PUBLIC_KEY_PEM = "PublicKeyPem";
    String ROLE_IDENTIFIERS = "RoleIdentifiers";
    String ROLE_MAPPINGS_PROVIDER = "RoleMappingsProvider";
    String SINGLE_LOGOUT_SERVICE = "SingleLogoutService";
    String SINGLE_SIGN_ON_SERVICE = "SingleSignOnService";
    String SP = "SP";

    String ATTR_ALIAS = "alias";
    String ATTR_ALLOW_ANY_HOSTNAME = "allowAnyHostname";
    String ATTR_ASSERTION_CONSUMER_SERVICE_URL = "assertionConsumerServiceUrl";
    String ATTR_ATTRIBUTE = "attribute";
    String ATTR_AUTODETECT_BEARER_ONLY = "autodetectBearerOnly";
    String ATTR_BINDING_URL = "bindingUrl";
    String ATTR_CLIENT_KEYSTORE = "clientKeystore";
    String ATTR_CLIENT_KEYSTORE_PASSWORD = "clientKeystorePassword";
    String ATTR_CONNECTION_POOL_SIZE = "connectionPoolSize";
    String ATTR_DISABLE_TRUST_MANAGER = "disableTrustManager";
    String ATTR_ENCRYPTION = "encryption";
    String ATTR_ENTITY_ID = "entityID";
    String ATTR_FILE = "file";
    String ATTR_FORCE_AUTHENTICATION = "forceAuthentication";
    String ATTR_ID = "id";
    String ATTR_IS_PASSIVE = "isPassive";
    String ATTR_LOGOUT_PAGE = "logoutPage";
    String ATTR_METADATA_URL = "metadataUrl";
    String ATTR_NAME = "name";
    String ATTR_NAME_ID_POLICY_FORMAT = "nameIDPolicyFormat";
    String ATTR_PASSWORD = "password";
    String ATTR_POLICY = "policy";
    String ATTR_POST_BINDING_URL = "postBindingUrl";
    String ATTR_PROXY_URL = "proxyUrl";
    String ATTR_REDIRECT_BINDING_URL = "redirectBindingUrl";
    String ATTR_REQUEST_BINDING = "requestBinding";
    String ATTR_RESOURCE = "resource";
    String ATTR_RESPONSE_BINDING = "responseBinding";
    String ATTR_SIGNATURES_REQUIRED = "signaturesRequired";
    String ATTR_SIGNATURE_ALGORITHM = "signatureAlgorithm";
    String ATTR_SIGNATURE_CANONICALIZATION_METHOD = "signatureCanonicalizationMethod";
    String ATTR_SIGNING = "signing";
    String ATTR_SIGN_REQUEST = "signRequest";
    String ATTR_SIGN_RESPONSE = "signResponse";
    String ATTR_SSL_POLICY = "sslPolicy";
    String ATTR_TRUSTSTORE = "truststore";
    String ATTR_TRUSTSTORE_PASSWORD = "truststorePassword";
    String ATTR_TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN = "turnOffChangeSessionIdOnLogin";
    String ATTR_TYPE = "type";
    String ATTR_UNIT = "unit";
    String ATTR_VALIDATE_ASSERTION_SIGNATURE = "validateAssertionSignature";
    String ATTR_VALIDATE_REQUEST_SIGNATURE = "validateRequestSignature";
    String ATTR_VALIDATE_RESPONSE_SIGNATURE = "validateResponseSignature";
    String ATTR_VALUE = "value";
    String ATTR_KEEP_DOM_ASSERTION = "keepDOMAssertion";
    String ATTR_SOCKET_TIMEOUT = "socketTimeout";
    String ATTR_CONNECTION_TIMEOUT = "connectionTimeout";
    String ATTR_CONNECTION_TTL = "connectionTtl";

    String XMLNS = "xmlns";
    String XSI = "xsi";
    String SCHEMA_LOCATION = "schemaLocation";

    String XMLNS_XSI = XMLNS + ":" + XSI;
    String XSI_SCHEMA_LOCATION = XSI + ":" + SCHEMA_LOCATION;

    String XML_SCHEMA_DEFAULT = "http://www.w3.org/2001/XMLSchema-instance";

    String UNKNOWN_ELEMENT = "";
}
