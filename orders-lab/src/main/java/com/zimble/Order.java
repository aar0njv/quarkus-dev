
package com.zimble;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record Order(
    String id,
    @NotBlank(message = "Item name cannot be blank") String item,
    @Positive(message = "Quantity must be greater than 0") int quantity
) {}