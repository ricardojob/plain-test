package io.vepo.plaintest;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "ts-runner", version = "1.0", subcommands = GenerateTestSuite.class, mixinStandardHelpOptions = true)
public class TestSuiteRunner implements Callable<Integer> {
	@Spec
	CommandSpec spec;

	public static void main(String... args) {
		int exitCode = new CommandLine(new TestSuiteRunner()).execute(args);
		System.exit(exitCode);
	}

	public Integer call() throws Exception {
		spec.commandLine().usage(System.out);
		return 0;
	}
}