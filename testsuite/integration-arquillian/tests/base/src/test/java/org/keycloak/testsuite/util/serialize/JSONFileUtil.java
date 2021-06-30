package org.keycloak.testsuite.util.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.util.SystemPropertiesJsonParserFactory;

import java.io.IOException;
import java.net.URL;

public class JSONFileUtil<T> extends SerializedFileUtil<T> {
    private static final ObjectMapper mapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());

    public JSONFileUtil(URL file, Class<T> clazz) {
        super(file, clazz);
    }

    @Override
    protected T readObject() {
        if (file == null) return null;
        try {
            return mapper.readValue(file, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void writeToFile(T object) {
        try {
            mapper.writeValue(file, object);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}