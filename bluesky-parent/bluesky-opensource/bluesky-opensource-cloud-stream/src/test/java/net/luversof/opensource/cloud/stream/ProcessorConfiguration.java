package net.luversof.opensource.cloud.stream;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.SendTo;

import reactor.core.publisher.Flux;

@Configuration
@EnableBinding(Processor.class)
public class ProcessorConfiguration {

	// @StreamListener(Processor.INPUT)
	// @SendTo(Processor.OUTPUT)
	// public String convert(String input) {
	// System.out.println("test~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	// return input.toLowerCase();
	// }

	@Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
    public String loggerSink(String payload) {
		System.out.println("test~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        return payload.toUpperCase();
    }

}
