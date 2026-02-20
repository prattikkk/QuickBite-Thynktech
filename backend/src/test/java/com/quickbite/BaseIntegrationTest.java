package com.quickbite;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Uses the "localtest" Spring profile which connects to the docker-compose
 * PostgreSQL instance running on localhost:5432/quickbite.
 */
@SpringBootTest
@ActiveProfiles("localtest")
public abstract class BaseIntegrationTest {
}
