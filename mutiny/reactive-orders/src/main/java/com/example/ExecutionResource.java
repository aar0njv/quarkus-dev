
// Usage of @Blocking for blocking thread (worker thread) and 
// non blocking thread (event-loop thread)

package com.example;

import io.smallrye.mutiny.Uni;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.time.Duration;


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

    // non-blocking delay using onItem().delayIt().by()
    @GET 
    @Path("/delay")
    public Uni<String> delay() {
        System.out.println("Started on: " + Thread.currentThread().getName());
        
        return Uni.createFrom().item("Completed without blocking")
                .onItem().delayIt().by(Duration.ofSeconds(2));
    }

    // Testing what happens when we call .await() on an Event Loop thread
    @GET
    @Path("/await-fail")
    public Uni<String> awaitFail() {

        System.out.println("Called on: " + Thread.currentThread().getName());

        Uni<String> pendingUni = Uni.createFrom().item("Hello from late data")
                .onItem().delayIt().by(Duration.ofMillis(500));

        String result = pendingUni.await().indefinitely();
        return Uni.createFrom().item(result);
    }

    // Java 21 Virtual Thread
    @GET
    @Path("/virtual")
    @RunOnVirtualThread
    public String virtualThread() throws InterruptedException {
        System.out.println("Thread name: " + Thread.currentThread().getName());
        System.out.println("Virtual Thread? " + Thread.currentThread().isVirtual());
        Thread.sleep(2000); // Blocking is fine on a virtual thread!
        return "Hello from Virtual Thread!";
    }
}
