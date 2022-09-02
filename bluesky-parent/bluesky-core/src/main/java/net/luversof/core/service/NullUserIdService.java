package net.luversof.core.service;

public class NullUserIdService implements UserIdService {

	@Override
	public String getUserId() {
		return null;
	}

}
