package net.luversof.web.test.hystrix.service;

import org.springframework.stereotype.Service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import net.luversof.boot.exception.BlueskyException;

@Service
public class TestHystrixService {


    public String defaultStores(String message) {
        return "fail";
    }

    @HystrixCommand(fallbackMethod = "defaultStores", groupKey="testGroupKey", commandKey = "testCommandKey")
    public String getStores(String message) {
    	
    	if (message.equals("ERROR")) {
    		throw new BlueskyException("ERROR");
    	}
        return message;
    }
}
