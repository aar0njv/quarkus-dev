package com.example;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;



@Path("execution")
public class UniPractice {

    @GET
    @Path("/pizza-manual")
    public String pizzaManualSubscribe() {
        System.out.println("1. Method entered");

        Uni<String> pizzaUni = Uni.createFrom().item("PIZZA")
            .invoke(item -> System.out.println(">>> 2. BAKING: " + item));

        // WE manually trigger:
        pizzaUni.subscribe().with(item -> {
            System.out.println(">>> 3. EATING: " + item);
        });

        System.out.println("4. Method exiting");
        return "Finished";
    }

    @GET 
    @Path("/pizza")
    public Uni<String> pizza() {
        System.out.println("Method entered");
        return Uni.createFrom().item("PIZZA")
            .invoke(item -> System.out.println(">>>BAKING: " + item));
        
    }
    
}
