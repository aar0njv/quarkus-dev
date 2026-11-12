// REST Controller which injects the bean OrderService using constructor injection

package com.zimble;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/orders")
public class OrderResource {

    private final OrderService service;

    // Constructor injection - no @Inject annotation needed in Quarkus for single constructors
    public OrderResource(OrderService service) {
        this.service = service;
    }

    @GET
    @Path("/{id}")
    public String getOrder(@PathParam("id") String id) {
        return service.getOrderDetails(id);
    }
}