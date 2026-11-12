package com.zimble;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

@ApplicationScoped
public class OrderService {
    
    private static final Logger LOG = Logger.getLogger(OrderService.class);
    private final Map<String, Order> database = new ConcurrentHashMap<>();

    private final int maxItems;
    public OrderService(@ConfigProperty(name = "orders.max-items") int maxItems) {
        this.maxItems = maxItems;
    }

    // @Observes is an event listening annotation
    // When framework finishes wiring all beans together, a Startup event is generated, which can be observed
    void onStart(@Observes Startup event) {
        LOG.infof("OrderService started. Maximum items allowed per order: %d", maxItems);
    }

    public void saveOrder(Order order) {
        if (order.quantity() > maxItems){
            throw new IllegalArgumentException("Order exceeds maximum items limit of " + maxItems);
        }
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
