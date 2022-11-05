package org.keycloak.storage.async;

import org.keycloak.models.async.DatastoreAsyncProvider;
import org.keycloak.provider.Spi;

public class DatastoreAsyncSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "datastore-async";
    }

    @Override
    public Class<DatastoreAsyncProvider> getProviderClass() {
        return DatastoreAsyncProvider.class;
    }

    @Override
    public Class<DatastoreAsyncProviderFactory> getProviderFactoryClass() {
        return DatastoreAsyncProviderFactory.class;
    }

    @Override
    public boolean isEnabled() {
        //TODO Feature??
        return Spi.super.isEnabled();
    }
}
