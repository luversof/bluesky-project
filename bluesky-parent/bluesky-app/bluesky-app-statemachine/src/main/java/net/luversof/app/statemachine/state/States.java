package net.luversof.app.statemachine.state;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

	States(String group) {
		this.group = group;
	}

	public String getGroup() {
		return group;
	}

	public static Set<States> findByGroup(String group) {
		return Arrays.asList(States.values()).stream().filter(x -> x.getGroup().equals(group))
				.collect(Collectors.toSet());
	}
}
