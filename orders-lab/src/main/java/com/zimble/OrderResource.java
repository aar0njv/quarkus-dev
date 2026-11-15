package com.zimble;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class OrderResource {
    
    private final OrderService orderService;

    public OrderResource(OrderService orderService) {
        this.orderService = orderService;
    }

    // return all orders (200 OK)
    @GET 
    public Response getAllOrders() {
        return Response.ok(orderService.getAllOrders()).build();
    }

    // returns specific order (200 OK) or (404 Not Found)
    @GET 
    @Path("/{id}")
    public Response getOrder(@PathParam("id") String id) {
        Order order = orderService.getOrder(id);
        if (order != null) {
            return Response.ok(order).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}