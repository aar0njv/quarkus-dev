package com.example;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

    @GET
    public List<Task> listAll() {
        // Panache automatically queries the database and maps it to a JSON array
        return Task.listAll();
    }

    @POST
    @Transactional // Required for operations that modify the database
    public Response create(Task task) {
        task.persist();
        return Response.status(Response.Status.CREATED).entity(task).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Task update(@PathParam("id") Long id, Task updatedTask) {
        Task entity = Task.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Task not found", 404);
        }
        entity.title = updatedTask.title;
        entity.completed = updatedTask.completed;
        return entity; // Panache automatically synchronizes this updated object with the database
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = Task.deleteById(id);
        if (deleted) {
            return Response.noContent().build();
        }
        throw new WebApplicationException("Task not found", 404);
    }
}
