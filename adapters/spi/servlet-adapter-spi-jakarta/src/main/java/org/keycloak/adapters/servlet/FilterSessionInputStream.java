package org.keycloak.adapters.servlet;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class FilterSessionInputStream {

    public static ServletInputStream setInputStream(ByteArrayInputStream is) {
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() throws IOException {
                return is.read();
            }
        };
    }
}
