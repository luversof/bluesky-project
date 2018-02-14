package net.luversof.core;

import org.springframework.stereotype.Component;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import net.luversof.core.exception.BlueskyException;

@Component
public class HystrixTestService {


    @HystrixCommand(fallbackMethod = "defaultStores")
    public Object getStores(String message) {
        return "fail";
    }

    public String defaultStores(String message) {
    	
    	if (message.equals("ERROR")) {
    		throw new BlueskyException("ERROR");
    	}
        return message;
    }
}
