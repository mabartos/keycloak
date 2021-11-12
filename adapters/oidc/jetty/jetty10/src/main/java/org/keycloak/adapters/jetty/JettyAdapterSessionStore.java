package org.keycloak.adapters.jetty;

import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiMap;
import org.keycloak.adapters.jetty.spi.JettyHttpFacade;
import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.common.util.MultivaluedHashMap;

import javax.servlet.http.HttpSession;

public class JettyAdapterSessionStore implements AdapterSessionStore {
    public static final String CACHED_FORM_PARAMETERS = "__CACHED_FORM_PARAMETERS";
    protected Request request;

    public JettyAdapterSessionStore(Request request) {
        this.request = request;
    }

    @Override
    public void saveRequest() {
        // remember the current URI
        HttpSession session = request.getSession();
        synchronized (session) {
            // But only if it is not set already, or we save every uri that leads to a login form redirect
            if (session.getAttribute(FormAuthenticator.__J_URI) == null) {
                StringBuffer buf = request.getRequestURL();
                if (request.getQueryString() != null)
                    buf.append("?").append(request.getQueryString());
                session.setAttribute(FormAuthenticator.__J_URI, buf.toString());
                session.setAttribute(JettyHttpFacade.__J_METHOD, request.getMethod());

                if ("application/x-www-form-urlencoded".equals(request.getContentType()) && "POST".equalsIgnoreCase(request.getMethod())) {
                    MultiMap<String> formParameters = extractFormParameters(request);
                    MultivaluedHashMap<String, String> map = new MultivaluedHashMap<String, String>();
                    for (String key : formParameters.keySet()) {
                        for (Object value : formParameters.getValues(key)) {
                            map.add(key, (String) value);
                        }
                    }
                    session.setAttribute(CACHED_FORM_PARAMETERS, map);
                }
            }
        }
    }

    @Override
    public boolean restoreRequest() {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        synchronized (session) {
            String j_uri = (String) session.getAttribute(FormAuthenticator.__J_URI);
            if (j_uri != null) {
                // check if the request is for the same url as the original and restore
                // params if it was a post
                StringBuffer buf = request.getRequestURL();
                if (request.getQueryString() != null)
                    buf.append("?").append(request.getQueryString());
                if (j_uri.equals(buf.toString())) {
                    String method = (String) session.getAttribute(JettyHttpFacade.__J_METHOD);
                    request.setMethod(method);
                    MultivaluedHashMap<String, String> j_post = (MultivaluedHashMap<String, String>) session.getAttribute(CACHED_FORM_PARAMETERS);
                    if (j_post != null) {
                        request.setContentType("application/x-www-form-urlencoded");
                        MultiMap<String> map = new MultiMap<String>();
                        for (String key : j_post.keySet()) {
                            for (String val : j_post.getList(key)) {
                                map.add(key, val);
                            }
                        }
                        restoreFormParameters(map, request);
                    }
                    session.removeAttribute(FormAuthenticator.__J_URI);
                    session.removeAttribute(JettyHttpFacade.__J_METHOD);
                    session.removeAttribute(FormAuthenticator.__J_POST);
                }
                return true;
            }
        }
        return false;
    }

    protected static MultiMap<String> extractFormParameters(Request baseRequest) {
        MultiMap<String> formParameters = new MultiMap<String>();
        baseRequest.extractFormParameters(formParameters);
        return formParameters;
    }

    protected static void restoreFormParameters(MultiMap<String> j_post, Request baseRequest) {
        baseRequest.setContentParameters(j_post);
    }
}
