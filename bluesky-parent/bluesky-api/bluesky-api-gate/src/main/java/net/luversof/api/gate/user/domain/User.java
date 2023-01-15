package net.luversof.api.gate.user.domain;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(allowSetters = true, value = {"password", "createdDate", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled", "userType", "externalId"})
public record User(long idx, String userId, String userName, String password, ZonedDateTime createdDate, boolean accountNonExpired, boolean accountNonLocked, boolean credentialsNonExpired, boolean enabled, List<UserAuthority> userAuthorityList,
		UserType userType, String externalId) {
}
