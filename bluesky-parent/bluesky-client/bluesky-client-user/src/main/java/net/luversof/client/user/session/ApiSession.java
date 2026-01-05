package net.luversof.client.user.session;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.springframework.session.MapSession;
import org.springframework.session.Session;

import net.luversof.client.user.httpexchange.UserInfoApiClient;

public class ApiSession implements Session, Serializable {

	private static final long serialVersionUID = 1L;
	
	private final MapSession delegate;
	private final UserInfoApiClient userInfoApiClient;

	public ApiSession(String id, UserInfoApiClient userInfoApiClient) {
		this.delegate = new MapSession(id);
		this.userInfoApiClient = userInfoApiClient;
	}
	
	public ApiSession(MapSession session, UserInfoApiClient userInfoApiClient) {
		this.delegate = session;
		this.userInfoApiClient = userInfoApiClient;
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public String changeSessionId() {
		String newSessionId = userInfoApiClient.createNewSession();
		delegate.setId(newSessionId);
		return newSessionId;
	}

	@Override
	public <T> T getAttribute(String attributeName) {
		return delegate.getAttribute(attributeName);
	}

	@Override
	public Set<String> getAttributeNames() {
		return delegate.getAttributeNames();
	}

	@Override
	public void setAttribute(String attributeName, Object attributeValue) {
		delegate.setAttribute(attributeName, attributeValue);
	}

	@Override
	public void removeAttribute(String attributeName) {
		delegate.removeAttribute(attributeName);
	}

	@Override
	public Instant getCreationTime() {
		return delegate.getCreationTime();
	}

	@Override
	public void setLastAccessedTime(Instant lastAccessedTime) {
		delegate.setLastAccessedTime(lastAccessedTime);
	}

	@Override
	public Instant getLastAccessedTime() {
		return delegate.getLastAccessedTime();
	}

	@Override
	public void setMaxInactiveInterval(Duration interval) {
		delegate.setMaxInactiveInterval(interval);
	}

	@Override
	public Duration getMaxInactiveInterval() {
		return delegate.getMaxInactiveInterval();
	}

	@Override
	public boolean isExpired() {
		return delegate.isExpired();
	}

}
