package net.luversof.web.dynamiccrud.setting.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType;

/**
 * 실행 시점에 파라미터로 전달받은 테이블명을 쿼리 문자열의 {@code :columnId} placeholder에 안전하게 치환한다.
 *
 * <p>테이블명(식별자)은 JDBC named parameter로 바인딩할 수 없어 SQL 문자열에 직접 써넣어야 하므로, SQL injection을 막기 위해 반드시 (1)
 * 식별자 형식 검증 + (2) 화이트리스트 대조를 거친 값만 치환한다. (jsqlparser는 테이블 위치의 {@code :key}를 파싱하지 못하므로, 파싱 이전 단계에서
 * 문자열 치환한다.)
 *
 * <p>설정 방법: 대상 메뉴에 {@link DbFieldColumnType#TABLE_NAME} 타입의 DbField를 하나 등록하고, 해당 DbField의 {@code
 * columnPreset}에 허용 테이블 목록을 콤마로 구분해 넣는다 ({@code value|label} 형식도 허용하며 이때 value가 실제 테이블명).
 * columnPreset은 검색 화면의 드롭다운 옵션으로도 재사용되므로 허용 목록과 선택 UI가 한 곳에서 관리된다. 쿼리에는 WHERE의 named parameter와
 * 동일하게 {@code SELECT * FROM :columnId ...} 처럼 쓴다 (columnId가 {@code tableName}이면 {@code :tableName},
 * 실행 시 같은 키의 요청 파라미터로 실제 테이블명을 받는다).
 */
public class DynamicTableUtil {

  /** 스키마 접두어(선택) 포함 단순 식별자만 허용. 백틱/대괄호/공백/특수문자 불허. */
  private static final Pattern TABLE_NAME_PATTERN =
      Pattern.compile("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)?$");

  /** dbFieldList에서 동적 테이블명 지정용 DbField(TABLE_NAME 타입)를 반환. 없으면 null. */
  public static DbField findTableNameField(List<DbField> dbFieldList) {
    if (dbFieldList == null) {
      return null;
    }
    return dbFieldList.stream()
        .filter(x -> DbFieldColumnType.TABLE_NAME.equals(x.getColumnType()))
        .findFirst()
        .orElse(null);
  }

  /**
   * TABLE_NAME DbField가 설정되어 있으면 dataMap의 값으로 검증 후 {@code :columnId} placeholder를 치환한 쿼리를 반환. 설정이
   * 없으면 원본 쿼리를 그대로 반환(기존 고정 테이블 방식과 완전 호환).
   *
   * @param queryString 원본 쿼리 문자열
   * @param dbFieldList 대상 메뉴의 DbField 목록
   * @param dataMap 실행 파라미터 (요청 파라미터)
   * @return 테이블명이 치환된 쿼리 문자열
   */
  public static String resolveQueryString(
      String queryString, List<DbField> dbFieldList, Map<String, String> dataMap) {
    var tableNameField = findTableNameField(dbFieldList);
    if (tableNameField == null) {
      return queryString;
    }

    var columnId = tableNameField.getColumnId();
    var placeholderPattern = placeholderPattern(columnId);

    // 쿼리에 :columnId placeholder가 있어야 치환 가능. 없으면 치환이 조용히 누락되어
    // 엉뚱한 SQL 오류가 나므로, 설정 오류로 명확히 알린다.
    if (queryString == null || !placeholderPattern.matcher(queryString).find()) {
      throw new BlueskyException("DYNAMIC_TABLE_NAME_PLACEHOLDER_NOT_FOUND", ":" + columnId);
    }

    var requestedTableName = dataMap == null ? null : dataMap.get(columnId);
    if (!StringUtils.hasText(requestedTableName)) {
      throw new BlueskyException("DYNAMIC_TABLE_NAME_REQUIRED", columnId);
    }
    requestedTableName = requestedTableName.trim();

    // 1) 식별자 형식 검증
    if (!TABLE_NAME_PATTERN.matcher(requestedTableName).matches()) {
      throw new BlueskyException("DYNAMIC_TABLE_NAME_INVALID_FORMAT", requestedTableName);
    }

    // 2) 화이트리스트 대조 (columnPreset에 콤마로 구분된 허용 테이블 목록. value|label 형식이면 value가 테이블명)
    var allowedTableNames = parseAllowedTableNames(tableNameField.getColumnPreset());
    if (!allowedTableNames.contains(requestedTableName)) {
      throw new BlueskyException("DYNAMIC_TABLE_NAME_NOT_ALLOWED", requestedTableName);
    }

    // 검증된 값이므로 쿼리의 :columnId placeholder를 실제 테이블명으로 치환
    return placeholderPattern
        .matcher(queryString)
        .replaceAll(Matcher.quoteReplacement(requestedTableName));
  }

  /**
   * {@code :columnId} placeholder 매칭 패턴. 뒤에 식별자 문자가 오면 매칭하지 않아 {@code :tableName}이 {@code
   * :tableNameOther} 같은 다른 파라미터의 접두어와 충돌하지 않는다.
   */
  private static Pattern placeholderPattern(String columnId) {
    return Pattern.compile(":" + Pattern.quote(columnId) + "(?![A-Za-z0-9_])");
  }

  private static Set<String> parseAllowedTableNames(String columnPreset) {
    if (!StringUtils.hasText(columnPreset)) {
      return Collections.emptySet();
    }
    return Arrays.stream(columnPreset.split(","))
        .map(part -> part.contains("|") ? part.split("\\|")[0] : part)
        .map(String::trim)
        .filter(StringUtils::hasText)
        .collect(Collectors.toSet());
  }
}
