package com.example;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.time.Duration;

@Path("/execution")
public class Operators {
    
    // Transform Operators map and chain

    @GET
    @Path("/map")
    public Uni<String> map() {
        return Uni.createFrom().item("piZza")
                        .map(item -> item.toUpperCase());
    }
    // In full syntax : .item("piZza").onItem().transform(item -> item.toUpperCase());


    
    // fetching order from a db, findOrderById  return Uni<Order>, which causes nested Uni (Uni<Uni<Order>>). chain() automatically unwraps it    
    private Uni<String> findOrderFromDb(String orderId) {
    return Uni.createFrom().item("Details for [" + orderId + "]")
            .onItem().delayIt().by(Duration.ofMillis(500));
    }

    @GET 
    @Path("/chain")
    public Uni<String> chain() {
        return Uni.createFrom().item("order-101")
                .chain(orderId -> findOrderFromDb(orderId));
    }

    

    
    
    // Side effect Operators: to do something with the item (like logging, auditing etc) without modifying the outcoming item.

    @GET 
    @Path("/invoke")
    public Uni<String> invoke() {
        return Uni.createFrom().item("PIZZA")
                            .invoke(item -> System.out.println("LOG Recieved for item: " + item)) // side operation
                            .map(item -> item.toLowerCase());   // main operation
    }


    // if side operation takes time and returns a Uni, then we use call() function instead of invoke 

    private Uni<Void> sendAuditLog(String item) {
    System.out.println(">>> AUDIT LOG: Saving event for " + item + " to external server...");
    return Uni.createFrom().voidItem()
            .onItem().delayIt().by(Duration.ofMillis(300));
    }

    @GET
    @Path("/call")
    public Uni<String> callExample() {
        return Uni.createFrom().item("PIZZA")
                .call(item -> sendAuditLog(item)) // Async side-effect: pauses until audit finishes, but passes "PIZZA" downstream
                .map(item -> item.toLowerCase());   
    }
    
}
