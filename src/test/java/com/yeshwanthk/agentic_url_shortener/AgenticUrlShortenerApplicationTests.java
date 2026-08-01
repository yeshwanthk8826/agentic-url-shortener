package com.yeshwanthk.agentic_url_shortener;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@SpringBootTest
class AgenticUrlShortenerApplicationTests {

	private static final DockerImageName POSTGRES_IMAGE =
			DockerImageName.parse("postgres:17-alpine");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres =
			new PostgreSQLContainer(POSTGRES_IMAGE);

	@Test
	void contextLoads() {
	}
}
