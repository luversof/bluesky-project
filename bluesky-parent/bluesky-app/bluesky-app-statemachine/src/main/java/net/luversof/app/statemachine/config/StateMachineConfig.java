package net.luversof.app.statemachine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.luversof.app.statemachine.state.Events;
import net.luversof.app.statemachine.state.States;

@Configuration
@EnableStateMachine
public class StateMachineConfig
		extends EnumStateMachineConfigurerAdapter<States, Events> {

	private static final Logger log = LoggerFactory.getLogger(StateMachineConfig.class);

	@Override
	public void configure(StateMachineConfigurationConfigurer<States, Events> config)
			throws Exception {
		config
				.withConfiguration()
				.autoStartup(true)
				.listener(listener());
	}

	@Override
	public void configure(StateMachineStateConfigurer<States, Events> states)
			throws Exception {
		states
				.withStates()
				.initial(States.SI)
				.states(States.findByGroup("root"))
				.and()
				.withStates()
				.parent(States.PROGRESS)
				.initial(States.PROGRESS_ATURN)
				.states(States.findByGroup("progress"));
	}

	@Override
	public void configure(StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
		transitions
				.withExternal()
				.source(States.SI).target(States.S1).event(Events.E1)
				.and()
				.withExternal()
				.source(States.S1).target(States.S2).event(Events.E2)
				.and()
				.withExternal()
				.source(States.SI).target(States.PROGRESS_BTURN).event(Events.START)
				.and()
				.withExternal()
				.source(States.PROGRESS_ATURN).target(States.PROGRESS_BTURN).event(Events.A_USER_ACT)
				.and()
				.withExternal()
				.source(States.PROGRESS_BTURN).target(States.PROGRESS_ATURN).event(Events.B_USER_ACT)
				.and();
	}

	@Bean
	StateMachineListener<States, Events> listener() {

		return new StateMachineListenerAdapter<States, Events>() {

			@Override
			public void stateChanged(State<States, Events> from, State<States, Events> to) {
				log.debug("State change {} to {}", from == null ? null : from.getId(), to == null ? null : to.getId());
			}

		};

	}
}