package net.luversof.core;

import org.springframework.stereotype.Component;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import net.luversof.core.exception.BlueskyException;

@Component
public class HystrixTestService {


    public String defaultStores(String message) {
        return "fail";
    }

    @HystrixCommand(fallbackMethod = "defaultStores")
    public String getStores(String message) {
    	
    	if (message.equals("ERROR")) {
    		throw new BlueskyException("ERROR");
    	}
        return message;
    }
}
