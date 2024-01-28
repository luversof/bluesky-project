//package net.luversof.web.gate.feign.user.config;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.ObjectProvider;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
//import org.springframework.cloud.openfeign.support.AbstractFormWriter;
//import org.springframework.cloud.openfeign.support.FeignEncoderProperties;
//import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
//import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
//import org.springframework.cloud.openfeign.support.SpringDecoder;
//import org.springframework.cloud.openfeign.support.SpringEncoder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.http.converter.HttpMessageConverter;
//import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//import org.springframework.security.jackson2.SecurityJackson2Modules;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import feign.codec.Decoder;
//import feign.codec.Encoder;
//import feign.form.spring.SpringFormEncoder;
//import feign.optionals.OptionalDecoder;
//
///**
// * UserClient에서 사용하기 위한 FeignConfig
// */
//public class UserClientFeignConfig {
//	
//	@Autowired
//	private Jackson2ObjectMapperBuilder builder;
//	
//	@Autowired
//	private ObjectProvider<HttpMessageConverter<?>> converters;
//	
//	@Autowired(required = false) 
//	private FeignEncoderProperties encoderProperties;
//	
//	@Bean
//	Decoder userFeignDecoder(ObjectProvider<HttpMessageConverterCustomizer> customizers) {
//		return new OptionalDecoder(new ResponseEntityDecoder(new SpringDecoder(this::getHttpMessageConverters, customizers)));
//	}
//	
//	@Bean
//	Encoder feignEncoder(ObjectProvider<AbstractFormWriter> formWriterProvider, ObjectProvider<HttpMessageConverterCustomizer> customizers) {
//		return new SpringEncoder(new SpringFormEncoder(), this::getHttpMessageConverters, encoderProperties, customizers);
//	}
//
//	private HttpMessageConverters getHttpMessageConverters() {
//		ObjectMapper objectMapper = builder.createXmlMapper(false).build();
//		objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
//		
//		List<HttpMessageConverter<?>> converterList = converters.orderedStream().filter(x -> !(x instanceof MappingJackson2HttpMessageConverter)).collect(Collectors.toList());
//		converterList.add(new MappingJackson2HttpMessageConverter(objectMapper));
//		return new HttpMessageConverters(converterList);
//	}
//	
//}