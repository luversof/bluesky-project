package net.luversof.web.dynamiccrud2.adapter;

import java.sql.Types;

import net.luversof.web.dynamiccrud2.domain.InputType;

/**
 * DB 에서 읽은 컬럼 정보.
 *
 * @param name 컬럼 이름
 * @param jdbcType {@link Types} 값
 * @param typeName DB 타입 이름
 * @param size 길이(문자 타입) 또는 정밀도
 * @param nullable NULL 허용
 * @param defaultValue 기본값 식(없으면 null)
 * @param autoIncrement 자동 증가 · 생성 컬럼
 * @param comment DB 컬럼 주석
 */
public record ColumnMeta(
    String name,
    int jdbcType,
    String typeName,
    Integer size,
    boolean nullable,
    String defaultValue,
    boolean autoIncrement,
    String comment) {

  /** 이보다 긴 문자 컬럼은 여러 줄 입력으로 본다 */
  private static final int TEXTAREA_THRESHOLD = 500;

  /** 필드 자동 등록 때 입력 형식 초기값 */
  public InputType inputType() {
    return switch (jdbcType) {
      case Types.TINYINT,
          Types.SMALLINT,
          Types.INTEGER,
          Types.BIGINT,
          Types.DECIMAL,
          Types.NUMERIC,
          Types.REAL,
          Types.FLOAT,
          Types.DOUBLE ->
          InputType.NUMBER;
      case Types.DATE -> InputType.DATE;
      case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> InputType.DATETIME;
      case Types.BIT, Types.BOOLEAN -> InputType.BOOLEAN;
      case Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB -> InputType.TEXTAREA;
      case Types.CHAR, Types.VARCHAR, Types.NCHAR, Types.NVARCHAR ->
          size != null && size > TEXTAREA_THRESHOLD ? InputType.TEXTAREA : InputType.TEXT;
      default -> InputType.TEXT;
    };
  }

  /** 입력 필수 초기값: NOT NULL 이면서 기본값 · 자동 증가가 없다 */
  public boolean required() {
    return !nullable && defaultValue == null && !autoIncrement;
  }

  /** 최대 길이 초기값: 문자 타입만, 사실상 무제한(PG text 등)은 null */
  public Integer maxLength() {
    var isText =
        switch (jdbcType) {
          case Types.CHAR, Types.VARCHAR, Types.NCHAR, Types.NVARCHAR -> true;
          default -> false;
        };
    return isText && size != null && size < Integer.MAX_VALUE ? size : null;
  }
}
