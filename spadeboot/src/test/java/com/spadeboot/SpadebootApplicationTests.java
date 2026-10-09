package com.spadeboot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-only-secret-not-used-anywhere-else-0123456789")
class SpadebootApplicationTests {

    @Test
    void contextLoads() {
    }

}
