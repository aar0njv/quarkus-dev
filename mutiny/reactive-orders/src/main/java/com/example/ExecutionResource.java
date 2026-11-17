
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
        Thread.sleep(5000);
        return "Completed";
    }
}