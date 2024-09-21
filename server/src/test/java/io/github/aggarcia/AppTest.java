package io.github.aggarcia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AppTest {

	@Test
	void test_junitTests_run() {
		assertEquals(1 + 1, 2);
	}

}
