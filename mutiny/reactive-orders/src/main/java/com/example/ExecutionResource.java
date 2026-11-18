
// Usage of @Blocking for blocking thread (worker thread) and 
// non blocking thread (event-loop thread)

package com.example;

import io.smallrye.mutiny.Uni;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;


@Path("execution")
public class ExecutionResource {

    // Event Loop - non blocking
    @GET
    @Path("/reactive")
    public Uni<String> reactive() {
        return Uni.createFrom().item(() -> {
        try {
            System.out.println("Reactive endpoint running on: " + Thread.currentThread().getName());
            Thread.sleep(5000); // Blocks the event-loop thread!
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Hello Reactive";
    });
    }


    // Worker Thread - blocking
    @GET
    @Path("/blocking")
    @Blocking
    public String blocking() throws InterruptedException {
        System.out.println("Blocking endpoint running on: " + Thread.currentThread().getName());
        Thread.sleep(5000);
        return "Completed";
    }
}