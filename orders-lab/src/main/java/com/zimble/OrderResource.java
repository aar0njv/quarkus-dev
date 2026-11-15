package com.zimble;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.POST;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.validation.Valid;


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

    // Create order (201 Created) or (400 Bad Request)
    @POST 
    public Response createOrder(@Valid Order order) {
        // if ID not provided, generate random UUID
        String orderId = (order.id() == null || order.id().isBlank()) ? UUID.randomUUID().toString() : order.id();
        
        Order newOrder = new Order(orderId, order.item(), order.quantity());
        
        try {
            orderService.createOrder(newOrder);
            URI location = URI.create("/orders/" + newOrder.id());
            return Response.created(location).entity(newOrder).build();
        } catch (IllegalArgumentException e) {
            String errorMsg = (e.getMessage() != null && !e.getMessage().isBlank()) 
                              ? e.getMessage() 
                              : "Invalid order request";
            
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", errorMsg)).build();
        }
    }

    // Update order (200 OK) or (404 Not Found) or (400 Bad Request)
    @PUT
    @Path("/{id}")
    public Response updateOrder(@PathParam("id") String id, @Valid Order order) {
        Order orderToUpdate = new Order(id, order.item(), order.quantity());
        try {
            boolean updated = orderService.updateOrder(id, orderToUpdate);
            if (!updated) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(orderToUpdate).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", e.getMessage()))
                           .build();
        }
    }

    // Delete order (204 No Content) or (404 Not Found)
    @DELETE
    @Path("/{id}")
    public Response deleteOrder(@PathParam("id") String id) {
        boolean deleted = orderService.deleteOrder(id);
        if (deleted) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // Delete all orders (204 No Content)
    @DELETE
    public Response deleteAllOrders() {
        orderService.deleteAllOrders();
        return Response.noContent().build();
    }
}