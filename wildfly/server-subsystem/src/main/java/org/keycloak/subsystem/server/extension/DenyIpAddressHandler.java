package org.keycloak.subsystem.server.extension;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;

import javax.ws.rs.core.Response;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DenyIpAddressHandler implements HttpHandler {
    private static final Logger log = Logger.getLogger(DenyIpAddressHandler.class);

    private HttpHandler next;
    private static ConcurrentHashMap<BlockItem, Integer> records = new ConcurrentHashMap<>();
    private static Long startTime = 0L;
    private Integer maxRequestsPerTime;
    private Long intervalMs;

    private static final Integer DEFAULT_REQUESTS_PER_TIME = 300;
    private static final Long DEFAULT_INTERVAL_MS = 2_000L;

    public DenyIpAddressHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        if (startTime == 0) {
            startTime = Time.currentTimeMillis();
        }

        String address = exchange.getSourceAddress().getHostName();

        BlockItem item = records.keySet()
                .stream()
                .filter(f -> f.getAddress().equals(address))
                .findFirst()
                .orElse(null);

        checkProperties();

        if (Time.currentTimeMillis() - startTime > intervalMs) {
            startTime = 0L;
            records = new ConcurrentHashMap<>();
        }

        if (item != null && records.containsKey(item)) {
            int count = records.get(item) + 1;
            final long shouldWaitMillis = item.getShouldWaitMillis();

            if (shouldWaitMillis != 0) {
                long time = Time.currentTimeMillis();
                if (time - item.getStartBlockingMillis() > shouldWaitMillis) {
                    item.clearTimeouts();
                } else {
                    endExchangeManyRequests(exchange);
                    return;
                }
            }

            if (count > maxRequestsPerTime) {
                item.setStartBlockingMillis(Time.currentTimeMillis());
                item.doProgressiveDelay();
                endExchangeManyRequests(exchange);
                return;
            }
            records.put(item, count);
        } else {
            records.put(new BlockItem(address), 1);
        }
        next.handleRequest(exchange);
    }

    private void endExchangeManyRequests(final HttpServerExchange exchange) {
        exchange.setStatusCode(Response.Status.TOO_MANY_REQUESTS.getStatusCode());
        exchange.endExchange();
    }

    private void checkProperties() {
        if (maxRequestsPerTime == null) {
            maxRequestsPerTime = DEFAULT_REQUESTS_PER_TIME;
        }

        if (intervalMs == null) {
            intervalMs = DEFAULT_INTERVAL_MS;
        }
    }

    private static class BlockItem {
        private final Long WAIT_TIME_MS = 2_000L;

        private String address;
        private long startBlockingMillis = 0;
        private long shouldWaitMillis = 0;
        private int iteration = 1;

        public BlockItem(String address) {
            this.address = address;
        }

        public String getAddress() {
            return address;
        }

        public long getStartBlockingMillis() {
            return startBlockingMillis;
        }

        public long getShouldWaitMillis() {
            return shouldWaitMillis;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public void setStartBlockingMillis(long startBlockingMillis) {
            this.startBlockingMillis = startBlockingMillis;
        }

        public void doProgressiveDelay() {
            this.shouldWaitMillis = 2 * iteration * WAIT_TIME_MS;
            iteration++;
        }

        public void clearTimeouts() {
            this.startBlockingMillis = 0;
            this.shouldWaitMillis = 0;
            this.iteration = 1;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(address);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof BlockItem)) {
                return true;
            } else {
                BlockItem clazz = (BlockItem) obj;
                return (this.getAddress().equals(clazz.getAddress()));
            }
        }
    }

}
