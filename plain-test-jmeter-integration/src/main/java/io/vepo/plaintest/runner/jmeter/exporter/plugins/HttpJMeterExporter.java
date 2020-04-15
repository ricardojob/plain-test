package io.vepo.plaintest.runner.jmeter.exporter.plugins;

import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.testelement.TestElement;

import io.vepo.plaintest.Step;
import io.vepo.plaintest.runner.jmeter.exporter.StepJMeterExporter;

public class HttpJMeterExporter implements StepJMeterExporter {
	public static final String HTTP_EXECUTOR_PLUGIN_NAME = "HTTP";

	@Override
	public String name() {
		return HTTP_EXECUTOR_PLUGIN_NAME;
	}

	@Override
	public AbstractSampler createSampler(Step step) {
		HTTPSamplerProxy sampler = new HTTPSamplerProxy();
		sampler.setName(step.getName());
		sampler.setProtocol(getProtocol(step));
		sampler.setDomain(getDomain(step));
		sampler.setPath(getPath(step));
		sampler.setPort(getPort(step));
		sampler.setMethod(step.requiredAttribute("method"));
		sampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
		sampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
		return sampler;
	}

	private int getPort(Step step) {
		String url = step.requiredAttribute("url");
		if (url.toLowerCase().startsWith("http://")) {
			url = url.substring("http://".length());
		} else if (url.toLowerCase().startsWith("https://")) {
			url = url.substring("https://".length());
		}
		int slashPosition = url.indexOf('/');
		if (slashPosition > 0) {
			url = url.substring(0, slashPosition);
		}

		int portPosition = url.indexOf(':');
		if (portPosition > 0) {
			return Integer.valueOf(url.substring(portPosition + 1));
		}
		return 80;
	}

	private String getPath(Step step) {
		String url = step.requiredAttribute("url");
		if (url.toLowerCase().startsWith("http://")) {
			url = url.substring("http://".length());
		} else if (url.toLowerCase().startsWith("https://")) {
			url = url.substring("https://".length());
		}
		int slashPosition = url.indexOf('/');
		if (slashPosition > 0) {
			return url.substring(slashPosition);
		} else {
			return "";
		}
	}

	private String getProtocol(Step step) {
		String url = step.requiredAttribute("url");
		if (url.toLowerCase().startsWith("http://")) {
			return "http";
		} else if (url.toLowerCase().startsWith("https://")) {
			return "https";
		}
		return "http";
	}

	private String getDomain(Step step) {
		String url = step.requiredAttribute("url");
		if (url.toLowerCase().startsWith("http://")) {
			url = url.substring("http://".length());
		} else if (url.toLowerCase().startsWith("https://")) {
			url = url.substring("https://".length());
		}
		int slashPosition = url.indexOf('/');
		if (slashPosition > 0) {
			url = url.substring(0, slashPosition);
		}

		int portPosition = url.indexOf(':');
		if (portPosition > 0) {
			url = url.substring(0, portPosition);
		}
		return url;
	}

}
