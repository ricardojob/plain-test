package io.vepo.plaintest.runner.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.vepo.plaintest.Suite;
import io.vepo.plaintest.SuiteFactory;
import io.vepo.plaintest.runner.utils.MockOs;

public class FailsTest {
	@BeforeEach
	public void setup() {
		MockOs.mockUnix();
	}

	@AfterEach
	public void shutdown() {
		MockOs.clear();
	}

	@Test
	@Disabled
	public void unknownPluginTest() {
		Suite suite = SuiteFactory.parseSuite("Suite UnknownTest {\n" + //
				"        Unknown DoNothing {\n" + //
				"            cmd    : \"xyz\"\n" + //
				"            timeout: 500\n" + //
				"            assert stdout Equals \"other string\"\n" + //
				"        }\n" + //
				"    }\n" + //
				"}");
		PlainTestExecutor executor = new PlainTestExecutor();
		Result result = executor.execute(suite);
		Result doNothingResult = result.getResults().stream().filter(r -> r.getName().equals("DoNothing")).findFirst()
				.orElse(null);
		assertNotNull(doNothingResult);
		assertEquals(Arrays.asList(new Fail(FailReason.PLUGIN_NOT_FOUND, "Could not find plugin: Unknown")),
				doNothingResult.getFails());
	}

	@Test
	public void unknownCommandPluginTest() {
		Suite suite = SuiteFactory.parseSuite("Suite UnknownTest {\n" + //
				"        CMD DoNothing {\n" + //
				"            cmd    : \"unknownCommand\"\n" + //
				"            timeout: 500\n" + //
				"       }\n" + //
				"    }\n" + //
				"}");
		PlainTestExecutor executor = new PlainTestExecutor();
		Result result = executor.execute(suite);
		Result doNothingResult = result.getResults().stream().filter(r -> r.getName().equals("DoNothing")).findFirst()
				.orElse(null);
		assertNotNull(doNothingResult);
		assertEquals(doNothingResult.getFails().size(), 1);
		assertEquals(FailReason.FAILED, doNothingResult.getFails().get(0).getReason());
	}
}
