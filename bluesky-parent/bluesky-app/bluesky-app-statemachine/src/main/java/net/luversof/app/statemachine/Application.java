package net.luversof.app.statemachine;

import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.statemachine.StateMachine;

import net.luversof.app.statemachine.state.Events;
import net.luversof.app.statemachine.state.States;

@SpringBootApplication
public class Application implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Autowired
    private StateMachine<States, Events> stateMachine;

	@Override
	public void run(String... args) throws Exception {
		
//		Scanner scanner = new Scanner(System.in);
//		while (true) {
//			String input = scanner.nextLine().trim().toLowerCase();
//			
//			System.out.println("scanner : " + input);
//		}
		
		stateMachine.sendEvent(Events.START);
	    stateMachine.sendEvent(Events.A_USER_ACT);
	    stateMachine.sendEvent(Events.B_USER_ACT);
	}

}  