package org.keycloak.adapters.servlet;

import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class FilterSessionInputStream {

    public static ServletInputStream setInputStream(ByteArrayInputStream is) {
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return is.read();
            }
        };
    }
}
