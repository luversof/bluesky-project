package net.luversof.web.dynamiccrud.use.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
import net.sf.jsqlparser.JSQLParserException;

public interface UseService {

  SubMenuDbType getSupportDbType();

  Page<Map<String, Object>> find(
      SettingParameter settingParameter, Pageable pageable, Map<String, String> dataMap);

  Object create(SettingParameter settingParameter, Map<String, String> dataMap);

  Object update(SettingParameter settingParameter, Map<String, String> dataMap);

  Object delete(SettingParameter settingParameter, MultiValueMap<String, String> dataMap);

  /**
   * 사용자가 입력한 임의 SQL을 해당 subMenu의 DataSource에 실행한다. SELECT면 조회 결과(List&lt;Map&gt;), 그 외(DML)면 영향
   * 행수(Integer)를 반환한다. ⚠️ 위험 작업이므로 호출부에서 ROLE_MASTER/ROLE_ADMIN 권한을 반드시 검증할 것.
   */
  Object executeRawQuery(SettingParameter settingParameter, String sql) throws JSQLParserException;
}
