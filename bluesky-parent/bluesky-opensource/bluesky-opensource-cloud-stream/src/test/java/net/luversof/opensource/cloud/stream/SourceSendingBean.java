//package net.luversof.opensource.cloud.stream;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.stream.messaging.Source;
//import org.springframework.messaging.MessageChannel;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class SourceSendingBean {
//
//    private Source source;
//
//    @Autowired
//    public SourceSendingBean(Source source) {
//        this.source = source;
//    }
//
//    public void sayHello(String name) {
//    	source.output().send(MessageBuilder.withPayload(name).build());
//    }
//}