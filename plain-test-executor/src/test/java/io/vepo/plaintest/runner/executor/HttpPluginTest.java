package io.vepo.plaintest.runner.executor;

import static io.vepo.plaintest.SuiteFactory.parseSuite;
import static io.vepo.plaintest.runner.executor.FailReason.RUNTIME_EXCEPTION;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.Delay.seconds;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.atLeast;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.Delay;
import org.mockserver.model.HttpRequest;

import io.vepo.plaintest.Suite;
import io.vepo.plaintest.SuiteFactory;

@ExtendWith(MockServerExtension.class)
@DisplayName("HTTP Executor")
public class HttpPluginTest extends AbstractTest {

	private MockServerClient client;

	@BeforeEach
	public void setup(MockServerClient client) {
		this.client = client;
	}

	@AfterEach
	public void tearDown() {
		client.reset();
	}

	private static final String HTTP_TEST_SUITE = "Suite HttpGet {\n" + //
			"\n" + //
			"    HTTP GetRequest {\n" + //
			"        url: \"http://${host}:${port}/defaultGet\"\n" + //
			"        method: \"GET\"\n" + //
			"    }\n" + //
			"}";

	@Test
	@DisplayName("It should execute GET")
	public void getTest() {
		InetSocketAddress remoteAddress = client.remoteAddress();
		validateHttp("/defaultGet", "GET", 200, "{\"response\":\"OK\"}", 1, seconds(0), () -> {

			Suite suite = SuiteFactory.parseSuite(HTTP_TEST_SUITE.replace("${host}", remoteAddress.getHostName())
					.replace("${port}", Integer.toString(remoteAddress.getPort())));
			PlainTestExecutor executor = new PlainTestExecutor();

			assertThat(executor.execute(suite)).satisfies(result -> assertTrue(result.isSuccess()))
					.satisfies(result -> assertThat(find(result, "HttpGet")).isPresent().get()
							.satisfies(r -> assertTrue(r.isSuccess())))
					.satisfies(result -> assertThat(find(result, "GetRequest")).isPresent().get()
							.satisfies(r -> assertTrue(r.isSuccess())));
		});

	}

	private static final String HTTP_INVALID_METHOD_TEST_SUITE = "Suite HttpGet {\n" + //
			"\n" + //
			"    HTTP GetRequest {\n" + //
			"        url: \"http://${host}:${port}/defaultGet\"\n" + //
			"        method: \"INVALID_METHOD\"\n" + //
			"    }\n" + //
			"}";

	@Test
	@DisplayName("It should fail for invalid Method")
	public void invalidMethodTest() {
		InetSocketAddress remoteAddress = client.remoteAddress();
		validateHttp("/defaultGet", "GET", 200, "{\"response\":\"OK\"}", 0, seconds(0), () -> {

			Suite suite = parseSuite(HTTP_INVALID_METHOD_TEST_SUITE.replace("${host}", remoteAddress.getHostName())
					.replace("${port}", Integer.toString(remoteAddress.getPort())));
			PlainTestExecutor executor = new PlainTestExecutor();

			assertThat(executor.execute(suite)).satisfies(result -> assertFalse(result.isSuccess()))
					.satisfies(result -> assertThat(find(result, "HttpGet")).isPresent().get()
							.satisfies(r -> assertFalse(r.isSuccess())))
					.satisfies(result -> assertThat(find(result, "GetRequest")).isPresent().get().satisfies(r -> {
						assertFalse(r.isSuccess());
						assertEquals(r.getFails(),
								asList(new Fail(RUNTIME_EXCEPTION, "Invalid Method: INVALID_METHOD")));
					}));
		});

	}

	private static final String HTTP_INVALID_URL_TEST_SUITE = "Suite HttpGet {\n" + //
			"\n" + //
			"    HTTP GetRequest {\n" + //
			"        url: \"${host}:${port}/defaultGet\"\n" + //
			"        method: \"GET\"\n" + //
			"    }\n" + //
			"}";

	@Test
	@DisplayName("It should fail for invalid URL")
	public void invalidUrlTest() {
		InetSocketAddress remoteAddress = client.remoteAddress();
		validateHttp("/defaultGet", "GET", 200, "{\"response\":\"OK\"}", 0, seconds(0), () -> {
			String port = Integer.toString(remoteAddress.getPort());
			Suite suite = parseSuite(
					HTTP_INVALID_URL_TEST_SUITE.replace("${host}", "THIS|IS|AN|INVALID|URL").replace("${port}", port));
			PlainTestExecutor executor = new PlainTestExecutor();

			assertThat(executor.execute(suite)).satisfies(result -> assertFalse(result.isSuccess()))
					.satisfies(result -> assertThat(find(result, "HttpGet")).isPresent().get()
							.satisfies(r -> assertFalse(r.isSuccess())))
					.satisfies(result -> assertThat(find(result, "GetRequest")).isPresent().get().satisfies(r -> {
						assertFalse(r.isSuccess());

						assertEquals(
								asList(new Fail(RUNTIME_EXCEPTION,
										String.format("Invalid URL: THIS|IS|AN|INVALID|URL:%s/defaultGet", port))),
								r.getFails());
					}));
		});

	}

	@Test
	@DisplayName("It should fail for invalid URL")
	public void couldNotConnectTest() {
		InetSocketAddress remoteAddress = client.remoteAddress();
		validateHttp("/defaultGet", "GET", 200, "{\"response\":\"OK\"}", 0, seconds(0), () -> {
			String port = Integer.toString(remoteAddress.getPort());
			Suite suite = parseSuite(
					HTTP_TEST_SUITE.replace("${host}", "not-a-valid-endpoint.com.br").replace("${port}", port));
			PlainTestExecutor executor = new PlainTestExecutor();

			assertThat(executor.execute(suite)).satisfies(result -> assertFalse(result.isSuccess()))
					.satisfies(result -> assertThat(find(result, "HttpGet")).isPresent().get()
							.satisfies(r -> assertFalse(r.isSuccess())))
					.satisfies(result -> assertThat(find(result, "GetRequest")).isPresent().get().satisfies(r -> {
						assertFalse(r.isSuccess());

						assertEquals(asList(new Fail(RUNTIME_EXCEPTION, String.format(
								"Could not connect with: http://not-a-valid-endpoint.com.br:%s/defaultGet. Unknown Host.",
								port))), r.getFails());
					}));
		});

	}

	private static final String HTTP_TIMEOUT_URL_TEST_SUITE = "Suite HttpGet {\n" + //
			"\n" + //
			"    HTTP GetRequest {\n" + //
			"        url: \"http://${host}:${port}/defaultGet\"\n" + //
			"        method: \"GET\"\n" + //
			"        timeout: 1000\n" + //
			"    }\n" + //
			"}";

	@Nested
	public class TimeoutTest {

		@Test
		@DisplayName("It should fail if execution time exceeds timeout")
		public void timeoutErrorTest() {
			InetSocketAddress remoteAddress = client.remoteAddress();
			validateHttp("/defaultGet", "GET", 200, "{\"response\":\"OK\"}", 0, seconds(2), () -> {
				String port = Integer.toString(remoteAddress.getPort());
				Suite suite = parseSuite(HTTP_TIMEOUT_URL_TEST_SUITE.replace("${host}", remoteAddress.getHostName())
						.replace("${port}", port));
				PlainTestExecutor executor = new PlainTestExecutor();

				assertThat(executor.execute(suite)).satisfies(result -> assertFalse(result.isSuccess()))
						.satisfies(result -> assertThat(find(result, "HttpGet")).isPresent().get()
								.satisfies(r -> assertFalse(r.isSuccess())))
						.satisfies(result -> assertThat(find(result, "GetRequest")).isPresent().get().satisfies(r -> {
							assertFalse(r.isSuccess());
							assertEquals(
									asList(new Fail(FailReason.TIMED_OUT, "Execution exceeds timeout! timeout=1000ms")),
									r.getFails());
						}));
			});
		}
	}

	private void validateHttp(String path, String method, int statusCode, String body, int times, Delay delay,
			Runnable code) {
		HttpRequest serverRequest = request().withMethod(method).withPath(path);

		client.when(serverRequest).respond(response().withStatusCode(statusCode).withBody(body).withDelay(delay));

		code.run();

		client.verify(serverRequest, atLeast(times));
	}

}
