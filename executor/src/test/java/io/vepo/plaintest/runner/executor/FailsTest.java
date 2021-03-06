package io.vepo.plaintest.runner.executor;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.vepo.plaintest.Suite;
import io.vepo.plaintest.SuiteFactory;

public class FailsTest {

	@Test
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
		assertEquals(asList(new Fail(FailReason.PLUGIN_NOT_FOUND, "Could not find plugin: Unknown")),
				doNothingResult.getFails());
	}

	@Test
	public void unknownCommandPluginTest() {
		Suite suite = SuiteFactory.parseSuite("Suite UnknownTest {\n" + //
				"        Process DoNothing {\n" + //
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
		assertEquals(1, doNothingResult.getFails().size());
		assertEquals(FailReason.FAILED, doNothingResult.getFails().get(0).getReason());
	}
}
