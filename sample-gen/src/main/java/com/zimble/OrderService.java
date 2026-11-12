// sample application scoped bean

package com.zimble;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderService {
    
    private static final Logger LOG = Logger.getLogger(OrderService.class);

    @Startup // Forces this bean to start immediately when Quarkus boots
    void onStart() {
        LOG.info("OrderService has been eagerly initialized!");
    }

    public String getOrderDetails(String id) {
        return "Order details for ID: " + id;
    }
}