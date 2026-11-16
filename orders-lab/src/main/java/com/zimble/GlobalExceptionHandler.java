package com.zimble;

import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class GlobalExceptionHandler {
    
    @ServerExceptionMapper 
    public Response handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(),
            "Bad Request",
            e.getMessage()
        );
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity(errorResponse)
                       .build();
    }

    @ServerExceptionMapper 
    public Response handleGenericException(Exception e) {
        ErrorResponse errorResponse = new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later."
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity(errorResponse)
                       .build();
    }
}
