package net.luversof.batch.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.rule.OutputCapture;

import net.luversof.batch.Application;

public class SampleBatchApplicationTests {

	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testDefaultSettings() {
		assertThat(SpringApplication
				.exit(SpringApplication.run(Application.class))).isEqualTo(0);
		String output = this.outputCapture.toString();
		assertThat(output).contains("completed with the following parameters");
	}

}
