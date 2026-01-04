package net.luversof.client.user.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;

import net.luversof.client.user.httpexchange.UserInfoApiClient;
import net.luversof.client.user.httpexchange.UserInfoApiClient.CreateSessionRequest;
import net.luversof.client.user.httpexchange.UserInfoApiClient.DeleteSessionRequest;

public class ApiSessionRepository implements SessionRepository<MapSession> {

	private final UserInfoApiClient userInfoApiClient;

	public ApiSessionRepository(UserInfoApiClient userInfoApiClient) {
		this.userInfoApiClient = userInfoApiClient;
	}

	@Override
	public MapSession createSession() {
		String sessionId = userInfoApiClient.createNewSession();
		return new MapSession(sessionId);
	}

	@Override
	public void save(MapSession session) {
		Map<String, Object> attributes = new HashMap<>();
		session.getAttributeNames().forEach(name -> {
			Object value = session.getAttribute(name);
			attributes.put(name, serialize(value));
		});
		
		userInfoApiClient.createSession(new CreateSessionRequest(session.getId(), attributes));
	}

	@Override
	public MapSession findById(String id) {
		try {
			var userInfo = userInfoApiClient.validateSession(id);
			if (userInfo == null) {
				return null;
			}

			MapSession session = new MapSession(id);
			if (userInfo.sessionAttributes() != null) {
				userInfo.sessionAttributes().forEach((k, v) -> {
					session.setAttribute(k, deserialize(v));
				});
			}
			
			session.setLastAccessedTime(Instant.now());
			session.setMaxInactiveInterval(Duration.ofMinutes(30));

			return session;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void deleteById(String id) {
		userInfoApiClient.deleteSession(new DeleteSessionRequest(id));
	}
	
	private Object serialize(Object object) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(bos)) {
			out.writeObject(object);
			return Base64.getEncoder().encodeToString(bos.toByteArray());
		} catch (IOException e) {
			return null;
		}
	}

	private Object deserialize(Object object) {
		if (object instanceof String str) {
			try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(str));
					ObjectInputStream in = new ObjectInputStream(bis)) {
				return in.readObject();
			} catch (IOException | ClassNotFoundException e) {
				return null;
			}
		}
		return object;
	}

}
