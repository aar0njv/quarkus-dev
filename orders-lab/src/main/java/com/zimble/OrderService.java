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

    public void createOrder(Order order) {
        if (order.quantity() > maxItems){
            throw new IllegalArgumentException("Order exceeds maximum items limit of " + maxItems);
        }
        database.put(order.id(), order);
        LOG.infof("Order %s saved.", order.id());
    }

    public Order getOrder(String id) {
        return database.get(id);
    }

    public boolean deleteOrder(String id) {
        if (database.remove(id) != null) {
            LOG.infof("Order %s deleted.", id);
            return true;
        } else {
            return false;
        }
    }

    public void deleteAllOrders() {
        int count = database.size();
        database.clear();
        LOG.infof("All %d orders deleted.", count);
    }

    public boolean updateOrder(String id, Order updatedOrder) {
        if (!database.containsKey(id)) {
            return false;
        }
        if (updatedOrder.quantity() > maxItems) {
            throw new IllegalArgumentException("Order exceeds maximum items limit of " + maxItems);
        }
        database.put(id, updatedOrder);
        LOG.infof("Order %s updated.", id);
        return true;
    }

    public Collection<Order> getAllOrders() {
        return database.values();
    }

    public Collection<Order> getOrdersByItem(String item) {
        if (item == null || item.isBlank()) {
            return database.values();
        }
        return database.values().stream()
                .filter(order -> order.item().toLowerCase().contains(item.trim().toLowerCase()))
                .toList();
    }
}
