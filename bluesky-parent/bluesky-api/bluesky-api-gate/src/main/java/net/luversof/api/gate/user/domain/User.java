package net.luversof.api.gate.user.domain;

import java.util.List;

public record User(long idx, String userId, String userName, List<UserAuthority> userAuthorityList) {

}
