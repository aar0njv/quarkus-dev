package com.zimble;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

@ApplicationScoped
public class OrderService {
    
    private static final Logger LOG = Logger.getLogger(OrderService.class);

    private final Map<String, Order> database = new ConcurrentHashMap<>();

    // @Observes is an event listening annotation
    // When framework finishes wiring all beans together, a Startup event is generated, which can be observed
    void onStart(@Observes Startup event) {
        LOG.info("OrderService has started. In-memory database initialized.");
    }

    public void saveOrder(Order order) {
        database.put(order.id(), order);
        LOG.infof("Order %s saved.", order.id());
    }

    public Order getOrder(String id) {
        return database.get(id);
    }

    public Collection<Order> getAllOrders() {
        return database.values();
    }
}
