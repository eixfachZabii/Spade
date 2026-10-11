package com.spade;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * The full app on a throwaway PostgreSQL, with MockMvc.
 * The tests' invite code is "letmein"; the property is its SHA-256.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(properties = "spade.invite-code-sha256=1c8bfe8f801d79745c4631d09fff36c82aa37fc4cce4fc946683d7b336b63032")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
public @interface SpadeIntegrationTest {
}
