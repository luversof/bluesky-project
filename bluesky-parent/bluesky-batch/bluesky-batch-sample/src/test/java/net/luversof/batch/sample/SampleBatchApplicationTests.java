package net.luversof.batch.sample;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.rule.OutputCapture;

import net.luversof.batch.Application;

import static org.assertj.core.api.Assertions.assertThat;

public class SampleBatchApplicationTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testDefaultSettings() {
		assertThat(SpringApplication
				.exit(SpringApplication.run(Application.class))).isEqualTo(0);
		String output = this.outputCapture.toString();
		assertThat(output).contains("completed with the following parameters");
	}

}
