package org.keycloak.admin.ui.rest.model;

public class UserStatsRepresentation {

    private long active;
    private long inactive;
    private long locked;
    private long total;
    private long clients;

    public long getActive() {
        return active;
    }

    public void setActive(long active) {
        this.active = active;
    }

    public long getInactive() {
        return inactive;
    }

    public void setInactive(long inactive) {
        this.inactive = inactive;
    }

    public long getLocked() {
        return locked;
    }

    public void setLocked(long locked) {
        this.locked = locked;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getClients() {
        return clients;
    }

    public void setClients(long clients) {
        this.clients = clients;
    }
}
