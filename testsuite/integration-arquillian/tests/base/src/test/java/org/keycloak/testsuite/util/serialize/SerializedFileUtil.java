package org.keycloak.testsuite.util.serialize;

import java.io.File;
import java.net.URL;
import java.util.function.Consumer;

public abstract class SerializedFileUtil<T> {

    protected File file;
    protected Class<T> clazz;

    public SerializedFileUtil(URL file, Class<T> clazz) {
        this.file = new File(file.getFile());
        this.clazz = clazz;
    }

    /**
     * Get object from file
     */
    protected abstract T readObject();

    /**
     * Update configuration file for servlet
     *
     * @param object consumer with required changes in file
     */
    protected abstract void writeToFile(T object);

    public File updateFile(Consumer<T> config) {
        if (file == null) return null;
        T object = readObject();

        if (object != null && config != null) {
            config.accept(object);

            try {
                writeToFile(object);
            } catch (Exception e) {
                throw new RuntimeException("Cannot write to file", e);
            }
        }
        return file;
    }
}
