package net.luversof.app.statemachine.state;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum States {
	SI("none"),
	S1("none"),
	S2("none"),
	
	IDLE("root"),
	PROGRESS("root"),
	END("root"),
	
	PROGRESS_ATURN("progress"),
	PROGRESS_BTURN("progress"),
	
	;
	
	
	private String group;
	
	
	public static Set<States> findByGroup(String group) {
		return Arrays.asList(States.values()).stream().filter(x -> x.getGroup().equals(group)).collect(Collectors.toSet());
	}
}
