package com.exelynt.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-secret-for-jwt-signing-that-is-long-enough")
class BookingApplicationTests {
    @Test
    void contextLoads() {
    }
}
