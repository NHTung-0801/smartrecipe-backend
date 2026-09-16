package com.smartrecipe.smartrecipe_backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires live MySQL and Redis instances - excluded in CI")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
