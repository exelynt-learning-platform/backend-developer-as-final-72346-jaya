package com.exelynt.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginReturnsJwtAndAuthenticatedUserCanReadResources() throws Exception {
        String token = login("user@example.com", "user123");

        mockMvc.perform(get("/resources")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void userCannotCreateUpdateOrDeleteResourcesButAdminCan() throws Exception {
        String userToken = login("user@example.com", "user123");
        String adminToken = login("admin@example.com", "admin123");

        String resourceJson = """
                {
                  "name": "Test Lab",
                  "type": "ROOM",
                  "description": "Room for RBAC integration testing",
                  "pricePerHour": 33.25,
                  "available": true
                }
                """;

        mockMvc.perform(post("/resources")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson))
                .andExpect(status().isForbidden());

        long resourceId = createResource(adminToken, "Admin Managed Room", "42.00");

        mockMvc.perform(put("/resources/" + resourceId)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/resources/" + resourceId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/resources/" + resourceId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void reservationUsesJwtUserAndBlocksOtherUsersReservationAccess() throws Exception {
        String adminToken = login("admin@example.com", "admin123");
        String userToken = login("user@example.com", "user123");
        String secondUserToken = login("user2@example.com", "user2123");
        long resourceId = createResource(adminToken, "JWT Owned Reservation Room", "20.00");

        long reservationId = createReservation(userToken, resourceId, 2);

        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("user@example.com"))
                .andExpect(jsonPath("$.price").value(40.00));

        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", bearer(secondUserToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void reservationsSupportFilteringPaginationSortingAndAdminStatusUpdates() throws Exception {
        String adminToken = login("admin@example.com", "admin123");
        String userToken = login("user@example.com", "user123");
        long resourceId = createResource(adminToken, "Filtered Reservation Room", "45.00");
        long reservationId = createReservation(userToken, resourceId, 2);

        mockMvc.perform(get("/reservations")
                        .header("Authorization", bearer(userToken))
                        .param("status", "PENDING")
                        .param("minPrice", "80")
                        .param("maxPrice", "100")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(reservationId))
                .andExpect(jsonPath("$.content[0].price").value(90.00));

        mockMvc.perform(put("/reservations/" + reservationId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(patch("/reservations/" + reservationId + "/cancel")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private long createResource(String token, String name, String pricePerHour) throws Exception {
        String response = mockMvc.perform(post("/resources")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "type": "ROOM",
                                  "description": "Created by integration test",
                                  "pricePerHour": %s,
                                  "available": true
                                }
                                """.formatted(name, pricePerHour)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createReservation(String token, long resourceId, int hours) throws Exception {
        LocalDateTime startTime = LocalDateTime.now().plusDays(30).withNano(0);
        LocalDateTime endTime = startTime.plusHours(hours);
        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": %d,
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "status": "CONFIRMED"
                                }
                                """.formatted(resourceId, startTime, endTime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
