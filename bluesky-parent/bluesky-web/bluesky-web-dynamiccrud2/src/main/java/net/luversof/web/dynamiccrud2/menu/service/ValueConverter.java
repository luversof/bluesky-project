package net.luversof.web.dynamiccrud2.menu.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Set;
import java.util.UUID;

import net.luversof.web.dynamiccrud2.adapter.UntypedText;
import net.luversof.web.dynamiccrud2.config.Msg;
import net.luversof.web.dynamiccrud2.domain.DbField;

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
        case TEXT, TEXTAREA -> textValue(raw, field.getDataType());
      };
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          Msg.t("menu.action.badValue", field.getColumnName(), raw), e);
    }
  }

  /** 글자로 보내도 되는 문자 타입 이름(DB 마다 다른 이름, 소문자). 이 밖의 타입은 {@link UntypedText} 로 표시해 어댑터가 정한다 */
  private static final Set<String> STRING_TYPES =
      Set.of(
          "text", "varchar", "character varying", "char", "character", "bpchar", "name",
          "nvarchar", "nchar", "ntext", "tinytext", "mediumtext", "longtext", "string");

  /**
   * 글자 칸 값. uuid 는 UUID 로. 표준 문자 타입이 아닌 컬럼(jsonb · inet · enum · interval · 배열 등)은 {@link UntypedText} -
   * PostgreSQL 어댑터는 타입 없이 보내 서버가 컬럼 타입으로 바꾸게 하고, 다른 DB 는 글자 그대로 보낸다(2026-11-26, 예전엔 json ·
   * jsonb 만 대소문자로 PostgreSQL 을 짐작했다). 타입을 모르면(필드 정보 없음) 글자 그대로.
   */
  private static Object textValue(String raw, String dataType) {
    if ("uuid".equalsIgnoreCase(dataType)) {
      return UUID.fromString(raw.strip());
    }
    if (dataType != null && !STRING_TYPES.contains(dataType.toLowerCase(java.util.Locale.ROOT))) {
      return new UntypedText(raw);
    }
    return raw;
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

  /**
   * 목록 칸 날짜 · 일시 글자({@link #toText} 결과)를 필드 표시 형식으로. 읽을 수 없거나 형식이 맞지 않으면(날짜에 시각 패턴 등은 0시로 본다) 원래 글자
   * 그대로
   */
  public static String formatDate(String raw, String pattern) {
    if (raw == null || raw.isEmpty() || pattern == null || pattern.isBlank()) {
      return raw;
    }
    try {
      return DateTimeFormatter.ofPattern(pattern).format(toLocalDateTime(raw.strip()));
    } catch (DateTimeException | IllegalArgumentException e) {
      return raw;
    }
  }

  /** 표시 형식 패턴이 맞는가(빈 값은 형식 없음이라 맞다) */
  public static boolean isDateFormat(String pattern) {
    if (pattern == null || pattern.isBlank()) {
      return true;
    }
    try {
      DateTimeFormatter.ofPattern(pattern).format(LocalDateTime.of(2000, 1, 2, 3, 4, 5));
      return true;
    } catch (DateTimeException | IllegalArgumentException e) {
      return false;
    }
  }

  /** 엑셀 수(double)로 넣어도 자릿수가 그대로인가. 엑셀은 15자리까지만 지켜 큰 ID · 정밀한 numeric 은 글자로 넣어야 한다 */
  public static boolean fitsExcelNumber(Number number) {
    if (number instanceof Double || number instanceof Float) {
      return true;
    }
    try {
      return new BigDecimal(number.toString()).precision() <= 15;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /** datetime-local("2026-11-17T13:20") · 초 포함 · "2026-11-17 13:20:00" 모두 받는다 */
  private static LocalDateTime toLocalDateTime(String raw) {
    var normalized = raw.replace(' ', 'T');
    return normalized.length() == 10
        ? LocalDate.parse(normalized).atStartOfDay()
        : LocalDateTime.parse(normalized);
  }
}
