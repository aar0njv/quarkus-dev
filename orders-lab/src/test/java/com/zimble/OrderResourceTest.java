package com.zimble;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderResourceTest {

    @Test
    @Order(1)
    public void testGetAllOrdersInitiallyEmpty() {
        given()
            .when().get("/orders")
            .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    @Order(2)
    public void testCreateOrderSuccess() {
        String newOrderJson = """
            {
                "id": "order-101",
                "item": "Mechanical Keyboard",
                "quantity": 5
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(newOrderJson)
            .when().post("/orders")
            .then()
            .statusCode(201)
            .header("Location", notNullValue())
            .body("id", equalTo("order-101"))
            .body("item", equalTo("Mechanical Keyboard"))
            .body("quantity", equalTo(5));
    }

    @Test
    @Order(3)
    public void testCreateOrderWithoutIdGeneratesUuid() {
        String orderWithoutId = """
            {
                "item": "Wireless Mouse",
                "quantity": 2
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(orderWithoutId)
            .when().post("/orders")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("item", equalTo("Wireless Mouse"))
            .body("quantity", equalTo(2));
    }

    @Test
    @Order(4)
    public void testCreateOrderExceedingLimitFails() {
        // In default test profile, maxItems is 10 (or 100 in dev profile)
        String oversizedOrder = """
            {
                "id": "oversized-1",
                "item": "Bulk Monitors",
                "quantity": 999
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(oversizedOrder)
            .when().post("/orders")
            .then()
            .statusCode(400)
            .body("error", notNullValue());
    }

    @Test
    @Order(5)
    public void testGetOrderByIdSuccess() {
        given()
            .when().get("/orders/order-101")
            .then()
            .statusCode(200)
            .body("id", equalTo("order-101"))
            .body("item", equalTo("Mechanical Keyboard"))
            .body("quantity", equalTo(5));
    }

    @Test
    @Order(6)
    public void testGetNonExistentOrderReturns404() {
        given()
            .when().get("/orders/non-existent-id")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(7)
    public void testUpdateOrderSuccess() {
        String updatedOrder = """
            {
                "item": "Mechanical Keyboard Pro",
                "quantity": 8
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(updatedOrder)
            .when().put("/orders/order-101")
            .then()
            .statusCode(200)
            .body("id", equalTo("order-101"))
            .body("item", equalTo("Mechanical Keyboard Pro"))
            .body("quantity", equalTo(8));
    }

    @Test
    @Order(8)
    public void testDeleteOrderSuccess() {
        given()
            .when().delete("/orders/order-101")
            .then()
            .statusCode(204);

        // Verify it is gone
        given()
            .when().get("/orders/order-101")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(9)
    public void testDeleteNonExistentOrderReturns404() {
        given()
            .when().delete("/orders/random-id")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(10)
    public void testDeleteAllOrders() {
        // Create an order first
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"id": "temp-order", "item": "Headphones", "quantity": 1}
            """)
            .when().post("/orders")
            .then().statusCode(201);

        // Delete all orders
        given()
            .when().delete("/orders")
            .then().statusCode(204);

        // Verify collection is empty
        given()
            .when().get("/orders")
            .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    @Order(11)
    public void testCreateOrderWithBlankItemFailsValidation() {
        String invalidOrder = """
            {
                "item": "",
                "quantity": 5
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidOrder)
            .when().post("/orders")
            .then()
            .statusCode(400)
            .header("validation-exception", "true");
    }

    @Test
    @Order(12)
    public void testCreateOrderWithZeroQuantityFailsValidation() {
        String invalidOrder = """
            {
                "item": "Monitor",
                "quantity": 0
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidOrder)
            .when().post("/orders")
            .then()
            .statusCode(400)
            .header("validation-exception", "true");
    }

    @Test
    @Order(13)
    public void testFilterOrdersByItemQueryParam() {
        // Clear all first to start clean
        given().when().delete("/orders").then().statusCode(204);

        // Add 2 Keyboards and 1 Mouse
        given().contentType(ContentType.JSON)
            .body("""
                {"id": "qp-1", "item": "Keyboard", "quantity": 1}
            """)
            .when().post("/orders").then().statusCode(201);

        given().contentType(ContentType.JSON)
            .body("""
                {"id": "qp-2", "item": "keyboard", "quantity": 2}
            """)
            .when().post("/orders").then().statusCode(201);

        given().contentType(ContentType.JSON)
            .body("""
                {"id": "qp-3", "item": "Mouse", "quantity": 1}
            """)
            .when().post("/orders").then().statusCode(201);

        // Query with ?item=Keyboard should return 2 orders
        given()
            .queryParam("item", "Keyboard")
            .when().get("/orders")
            .then()
            .statusCode(200)
            .body("$", hasSize(2));

        // Query with ?item=Mouse should return 1 order
        given()
            .queryParam("item", "Mouse")
            .when().get("/orders")
            .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].id", equalTo("qp-3"));

        // Query without parameter should return all 3
        given()
            .when().get("/orders")
            .then()
            .statusCode(200)
            .body("$", hasSize(3));
    }
}
