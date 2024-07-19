package net.luversof.api.board.constant;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spring의 Role Hierarchy 구현을 해야 할까?
 * Spring의 경우 Map<String, Set<GrantedAuthority>> Role이 각각 자신이 허용하는 Authority를 전부 들고 있는 형태임
 */
@Getter
@AllArgsConstructor
public enum BoardRole {
	ADMIN(List.of("ROLE_ADMIN", "ROLE_USER", "ROLE_GUEST")),
	USER(List.of("ROLE_USER", "ROLE_GUEST")),
	GUEST(List.of("ROLE_GUEST"))
	;
	
	List<String> reachableRoleList;
}
