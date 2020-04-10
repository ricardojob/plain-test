package io.vepo.plaintest.parser;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.IntStream.range;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vepo.plaintest.Assertion;
import io.vepo.plaintest.Step;
import io.vepo.plaintest.Suite;
import io.vepo.plaintest.parser.antlr4.generated.TestSuiteListener;
import io.vepo.plaintest.parser.antlr4.generated.TestSuiteParser.AssertionContext;
import io.vepo.plaintest.parser.antlr4.generated.TestSuiteParser.AttributeContext;
import io.vepo.plaintest.parser.antlr4.generated.TestSuiteParser.ExecDirectoryContext;
import io.vepo.plaintest.parser.antlr4.generated.TestSuiteParser.StepContext;
import io.vepo.plaintest.parser.antlr4.generated.TestSuiteParser.SuiteContext;
import io.vepo.plaintest.parser.antlr4.generated.TestSuiteParser.ValueContext;

public class SuiteCreator implements TestSuiteListener {
	private static final Logger logger = LoggerFactory.getLogger(SuiteCreator.class);
	private static final Pattern LINE_START_PATTERN = Pattern.compile("^(\\s+)");
	private Suite testSuite;
	private Suite currentSuite;
	private Deque<Suite> suites;
	private Step currentStep;

	public SuiteCreator() {
		this.currentSuite = this.testSuite = null;
		this.suites = new LinkedList<>();
	}

	@Override
	public void visitTerminal(TerminalNode node) {
		logger.trace("Visit Terminal: {}", node);
	}

	@Override
	public void visitErrorNode(ErrorNode node) {
		logger.trace("Visit Error Node: {}", node);
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		logger.trace("Enter Every Rule: {}", ctx);
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {
		logger.trace("Exit Every Rule: {}", ctx);
	}

	@Override
	public void enterSuite(SuiteContext ctx) {
		logger.trace("Enter Suite: {}", ctx);
		Suite previousTestSuite = this.currentSuite;
		this.currentSuite = new Suite(
				Optional.ofNullable(previousTestSuite).map(Suite::lastIndex).map(i -> i + 1).orElse(0),
				ctx.IDENTIFIER().getText(), new ArrayList<>(), new ArrayList<>(), new HashMap<>());

		this.suites.addLast(currentSuite);

		if (isNull(this.testSuite)) {
			this.testSuite = this.currentSuite;
		}

		if (nonNull(previousTestSuite)) {
			previousTestSuite.addSuite(this.currentSuite);
		}
	}

	@Override
	public void exitSuite(SuiteContext ctx) {
		logger.trace("Exit Suite: {}", ctx);

		this.suites.pollLast();
		this.currentSuite = this.suites.peekLast();
	}

	public Suite getTestSuite() {
		return testSuite;
	}

	@Override
	public void enterStep(StepContext ctx) {
		logger.trace("Enter Step: {}", ctx);
		int lastIndex = this.currentSuite.lastIndex();
		if (ctx.IDENTIFIER().size() == 2) {
			this.currentStep = new Step(lastIndex + 1, ctx.IDENTIFIER(0).getText(), ctx.IDENTIFIER(1).getText(),
					new HashMap<>(), new ArrayList<>());
			this.currentSuite.addStep(currentStep);
		} else {
			logger.warn("Could not intantiate Step: context={}", ctx);
		}
	}

	@Override
	public void exitStep(StepContext ctx) {
		logger.trace("Exit Step: {}", ctx);
	}

	@Override
	public void enterAttribute(AttributeContext ctx) {
		logger.trace("Enter Attribute: {}", ctx);
	}

	@Override
	public void exitAttribute(AttributeContext ctx) {
		logger.trace("Exit Attribute: {}", ctx);

		if (nonNull(ctx.value().STRING())) {
			this.currentStep.addStringAttribute(ctx.IDENTIFIER().getText(), processString(ctx.value().getText()));
		} else if (nonNull(ctx.value().MULTILINE_STRING())) {
			this.currentStep.addStringAttribute(ctx.IDENTIFIER().getText(),
					processMultiLineString(ctx.value().getText()));
		} else if (nonNull(ctx.value().NUMBER())) {
			this.currentStep.addNumberAttribute(ctx.IDENTIFIER().getText(), Long.valueOf((ctx.value().getText())));
		} else {
			logger.warn("Invalid value! ctx={}", ctx);
		}
	}

	private String processString(String text) {
		return unescapeJava(text.substring(1, text.length() - 1));
	}

	private String processMultiLineString(String text) {
		String[] lines = text.substring(3, text.length() - 3).split("\n");
		if (lines.length > 0) {

			if (lines[0].trim().isEmpty()) {
				lines = Arrays.copyOfRange(lines, 1, lines.length);
			}

			if (lines[lines.length - 1].trim().isEmpty()) {
				lines = Arrays.copyOfRange(lines, 0, lines.length - 1);
			}

			removeTabs(lines);
		}
		return Stream.of(lines).collect(Collectors.joining("\n"));
	}

	private void removeTabs(String[] lines) {
		Matcher tabMatcher = LINE_START_PATTERN.matcher(lines[0]);
		if (tabMatcher.find()) {
			String tabPattern = tabMatcher.group(1);
			range(0, lines.length).filter(index -> lines[index].startsWith(tabPattern))
					.forEach(index -> lines[index] = lines[index].substring(tabPattern.length()));
		}
	}

	@Override
	public void enterValue(ValueContext ctx) {
		logger.trace("Enter Value: {}", ctx);
	}

	@Override
	public void exitValue(ValueContext ctx) {
		logger.trace("Exit Value: {}", ctx);
	}

	@Override
	public void enterAssertion(AssertionContext ctx) {
		logger.trace("Enter Assertion: {}", ctx);
	}

	@Override
	public void exitAssertion(AssertionContext ctx) {
		logger.trace("Enter Assertion: {}", ctx);

		if (nonNull(ctx.value().STRING())) {
			this.currentStep.addAssertion(new Assertion<>(ctx.IDENTIFIER().getText(), ctx.VERB().getText(),
					processString(ctx.value().getText())));
		} else if (nonNull(ctx.value().MULTILINE_STRING())) {
			this.currentStep.addAssertion(new Assertion<>(ctx.IDENTIFIER().getText(), ctx.VERB().getText(),
					processMultiLineString(ctx.value().getText())));
		} else if (nonNull(ctx.value().NUMBER())) {
			this.currentStep.addAssertion(new Assertion<>(ctx.IDENTIFIER().getText(), ctx.VERB().getText(),
					Long.valueOf((ctx.value().getText()))));
		} else {
			logger.warn("Invalid value! ctx={}", ctx);
		}
	}

	@Override
	public void enterExecDirectory(ExecDirectoryContext ctx) {
		logger.trace("Enter ExecDirectory: {}", ctx);
	}

	@Override
	public void exitExecDirectory(ExecDirectoryContext ctx) {
		logger.trace("Exit ExecDirectory: {}", ctx);
		if (nonNull(ctx.FILE_PATH())) {
			this.currentSuite.setExecDirectory(ctx.FILE_PATH().toString());
		} else if (nonNull(ctx.IDENTIFIER())) {
			this.currentSuite.setExecDirectory(ctx.IDENTIFIER().toString());
		} else {
			logger.warn("Invalid value! ctx={}", ctx);
		}
	}

}
