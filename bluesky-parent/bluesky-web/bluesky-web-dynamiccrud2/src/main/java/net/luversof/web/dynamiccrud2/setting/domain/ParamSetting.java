package net.luversof.web.dynamiccrud2.setting.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * 쿼리 파라미터 하나의 설정. MenuQuery.paramSettings(jsonb)에 이름 → 이 값의 맵으로 저장한다.
 *
 * @param role 역할
 * @param required 필수 여부(검색 조건은 최상위 AND 단순 조건만 선택으로 바꿀 수 있다)
 * @param defaultValue 값이 비었을 때 쓸 기본값(없으면 null)
 */
public record ParamSetting(Role role, boolean required, String defaultValue) {

  /** INPUT = 폼에서 입력, ROW_KEY = 선택한 행에서 자동 바인딩, SEARCH = 조회 검색 조건 */
  public enum Role {
    INPUT,
    ROW_KEY,
    SEARCH
  }

  public Map<String, Object> toMap() {
    var map = new HashMap<String, Object>();
    map.put("role", role.name());
    map.put("required", required);
    if (defaultValue != null && !defaultValue.isEmpty()) {
      map.put("defaultValue", defaultValue);
    }
    return map;
  }

  public static ParamSetting from(Object value) {
    if (!(value instanceof Map<?, ?> map)) {
      return null;
    }
    var role = map.get("role");
    var defaultValue = map.get("defaultValue");
    return new ParamSetting(
        role == null ? Role.INPUT : Role.valueOf(role.toString()),
        !Boolean.FALSE.equals(map.get("required")),
        defaultValue == null ? null : defaultValue.toString());
  }
}
