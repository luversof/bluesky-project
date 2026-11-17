package net.luversof.web.dynamiccrud2.use.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.UUID;

import net.luversof.web.dynamiccrud2.setting.domain.DbField;

/**
 * 화면 문자열 ↔ 바인딩 값. PostgreSQL 은 문자열을 uuid · 숫자 · 날짜 컬럼과 비교 · 저장하지 않아(예: uuid = character varying) 필드
 * 정보로 타입을 맞춘다. 필드를 모르면 문자열 그대로.
 */
public final class ValueConverter {

  private ValueConverter() {}

  /**
   * 화면 문자열 → 바인딩 값. 빈 문자열은 null.
   *
   * @throws IllegalArgumentException 형식이 맞지 않을 때(메시지는 화면에 보인다)
   */
  public static Object toBindValue(String raw, DbField field) {
    if (raw == null || raw.isEmpty()) {
      return null;
    }
    if (field == null || field.getInputType() == null) {
      return raw;
    }
    try {
      return switch (field.getInputType()) {
        case NUMBER -> new BigDecimal(raw.strip());
        case DATE -> LocalDate.parse(raw.strip().substring(0, Math.min(10, raw.strip().length())));
        case DATETIME -> toLocalDateTime(raw.strip());
        case BOOLEAN -> Boolean.valueOf(raw.strip());
        case TEXT, TEXTAREA ->
            "uuid".equalsIgnoreCase(field.getDataType()) ? UUID.fromString(raw.strip()) : raw;
      };
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(field.getColumnName() + " 값 형식이 맞지 않습니다: " + raw, e);
    }
  }

  /** DB 값 → 화면 문자열(목록 표시 · 폼 초기값 · 행 키 전달). 날짜시간은 초 단위 로컬 시각(datetime-local 입력과 같은 모양) */
  public static String toText(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS).toString();
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime
          .atZoneSameInstant(ZoneId.systemDefault())
          .toLocalDateTime()
          .truncatedTo(ChronoUnit.SECONDS)
          .toString();
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.truncatedTo(ChronoUnit.SECONDS).toString();
    }
    if (value instanceof TemporalAccessor || value instanceof java.util.Date) {
      return value.toString();
    }
    return String.valueOf(value);
  }

  /** datetime-local("2026-11-17T13:20") · 초 포함 · "2026-11-17 13:20:00" 모두 받는다 */
  private static LocalDateTime toLocalDateTime(String raw) {
    var normalized = raw.replace(' ', 'T');
    return normalized.length() == 10
        ? LocalDate.parse(normalized).atStartOfDay()
        : LocalDateTime.parse(normalized);
  }
}
