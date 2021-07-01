package org.keycloak.testsuite.util.serialize;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.net.URL;

public class XMLFileUtil<T> extends SerializedFileUtil<T> {

    public XMLFileUtil(URL file, Class<T> clazz) {
        super(file, clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T readObject() {
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unm = context.createUnmarshaller();
            unm.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
            return (T) unm.unmarshal(file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void writeToFile(T object) {
        try {
            JAXBContext context = JAXBContext.newInstance();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(object, file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}