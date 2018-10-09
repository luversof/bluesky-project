package net.luversof.blog.domain;

import java.lang.reflect.Method;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;
import net.luversof.blog.LambdaTestService;

@Data
//@Entity
public class LambdaTestDomain {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	// 저장을 String으로 해야함.
	private Method method = LambdaTestService.class.getMethods()[0];
}
