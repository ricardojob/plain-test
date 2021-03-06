package io.vepo.plaintest.runner.jmeter.exporter.plugins;

import static org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase.PORT;

import java.util.Optional;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.StringProperty;

import io.vepo.plaintest.PropertyReference;
import io.vepo.plaintest.Step;
import io.vepo.plaintest.runner.jmeter.exporter.StepExporter;

public class HttpExporter implements StepExporter {
	public static final String HTTP_EXECUTOR_PLUGIN_NAME = "HTTP";
	private static final String HTTP_PROTOCOL = "http://";
	private static final String HTTPS_PROTOCOL = "https://";

	@Override
	public AbstractSampler createSampler(Step step) {
		HTTPSamplerProxy sampler = new HTTPSamplerProxy();
		sampler.setArguments(new Arguments());
		sampler.setName(step.getName());
		sampler.setProtocol(getProtocol(step));
		sampler.setDomain(getDomain(step));
		sampler.setPath(getPath(step));
		Optional<String> maybePort = getPort(step);
		if (maybePort.isPresent()) {
			String port = maybePort.get();
			if (PropertyReference.matches(port)) {
				sampler.setProperty(new StringProperty(PORT, port));
			} else {
				sampler.setPort(Integer.parseInt(port));
			}
		} else {
			sampler.setPort(80);
		}
		Object method = step.getAttributes().get("method");
		if (method instanceof PropertyReference) {
			sampler.setMethod("${" + ((PropertyReference) method).getName() + "}");
		} else {
			sampler.setMethod(method.toString());
		}
		step.optionalAttribute("body", String.class).ifPresent(body -> sampler.addNonEncodedArgument("", body, ""));
		sampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
		sampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
		return sampler;
	}

	private String getDomain(Step step) {
		String url = removePath(removeProtocol(step));

		int portPosition = url.indexOf(':');
		if (portPosition > 0) {
			url = url.substring(0, portPosition);
		}
		return url;
	}

	private String getPath(Step step) {
		String url = removeProtocol(step);
		int slashPosition = url.indexOf('/');
		if (slashPosition > 0) {
			return url.substring(slashPosition);
		} else {
			return "";
		}
	}

	private Optional<String> getPort(Step step) {
		String url = removePath(removeProtocol(step));

		int portPosition = url.indexOf(':');
		if (portPosition > 0) {
			return Optional.of(url.substring(portPosition + 1));
		}
		return Optional.empty();
	}

	private String getProtocol(Step step) {
		String url = step.getAttributes().get("url").toString();
		if (url.toLowerCase().startsWith(HTTP_PROTOCOL)) {
			return "http";
		} else if (url.toLowerCase().startsWith(HTTPS_PROTOCOL)) {
			return "https";
		}
		return "http";
	}

	@Override
	public String pluginName() {
		return HTTP_EXECUTOR_PLUGIN_NAME;
	}

	private String removePath(String url) {
		int slashPosition = url.indexOf('/');
		if (slashPosition > 0) {
			url = url.substring(0, slashPosition);
		}
		return url;
	}

	private String removeProtocol(Step step) {
		String url = step.requiredAttribute("url");
		if (url.toLowerCase().startsWith(HTTP_PROTOCOL)) {
			url = url.substring(HTTP_PROTOCOL.length());
		} else if (url.toLowerCase().startsWith(HTTPS_PROTOCOL)) {
			url = url.substring(HTTPS_PROTOCOL.length());
		}
		return url;
	}

}
