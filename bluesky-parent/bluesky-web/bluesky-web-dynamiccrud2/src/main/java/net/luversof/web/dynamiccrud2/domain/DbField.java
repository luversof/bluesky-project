package net.luversof.web.dynamiccrud2.domain;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 테이블 단위 필드 메타(DbTable 묶음의 하위). 키는 (dbTable_id, columnName). PK · unique 는 저장하지 않고 쓸 때 DB 에서 읽는다.
 */
@Table("DbField")
public class DbField {

  @Column("columnName")
  private String columnName;

  /** 계산 · 함수 결과처럼 DB 에 없는 조회 전용 필드 */
  @Column("virtual")
  private boolean virtual;

  /** 사용자 지정 라벨. 화면 제목 = 쿼리 별칭 → label → dbComment → columnName */
  @Column("label")
  private String label;

  @Column("dbComment")
  private String dbComment;

  /** DB 가 알려 준 타입 이름 */
  @Column("dataType")
  private String dataType;

  @Column("inputType")
  private InputType inputType;

  @Column("required")
  private boolean required;

  @Column("maxLength")
  private Integer maxLength;

  @Column("autoIncrement")
  private boolean autoIncrement;

  @Column("sortable")
  private boolean sortable;

  /** 사용자가 sortable 을 바꿨으면 "컬럼 다시 읽기" 가 덮어쓰지 않는다 */
  @Column("sortableUserSet")
  private boolean sortableUserSet;

  /**
   * 수정 작업(UPDATE)에서 값을 바꿀 수 있는가. PK · unique · 자동 증가 컬럼은 처음에 false. null 은 아직 정하지 않은 것(이 열이 생기기 전에
   * 등록된 필드) - 사용 화면이 처음 쓸 때 "컬럼 다시 읽기" 로 채운다
   */
  @Column("updatable")
  private Boolean updatable;

  /** 사용자가 updatable 을 바꿨으면 "컬럼 다시 읽기" 가 덮어쓰지 않는다 */
  @Column("updatableUserSet")
  private boolean updatableUserSet;

  /** 다시 읽었을 때 DB 에서 사라진 컬럼 */
  @Column("missingInDb")
  private boolean missingInDb;

  @Column("displayOrder")
  private int displayOrder;

  /**
   * 화면 표시 설정(jsonb). 보이기만 바꾸는 설정을 열을 늘리지 않고 여기 키로 더한다 - 행 JSON · 엑셀 · 이력은 저장값 그대로. 키는 아래 접근자로만 다룬다.
   * 모르는 키는 그대로 둔다(옛 판 · 새 판이 섞여 돌 때)
   */
  @Column("displayOptions")
  private java.util.Map<String, Object> displayOptions;

  private static final String VALUE_OPTIONS = "valueOptions";
  private static final String DATE_FORMAT = "dateFormat";

  public java.util.Map<String, Object> getDisplayOptions() {
    return displayOptions;
  }

  public void setDisplayOptions(java.util.Map<String, Object> displayOptions) {
    this.displayOptions = displayOptions;
  }

  private String displayOption(String key) {
    return displayOptions == null || displayOptions.get(key) == null
        ? null
        : displayOptions.get(key).toString();
  }

  /** 값을 넣고, 비면 키를 지운다(다 비면 null 로 - 열이 NULL 인 필드와 같게) */
  private void setDisplayOption(String key, String value) {
    var map =
        displayOptions == null
            ? new java.util.LinkedHashMap<String, Object>()
            : new java.util.LinkedHashMap<>(displayOptions);
    if (value == null || value.isBlank()) {
      map.remove(key);
    } else {
      map.put(key, value);
    }
    displayOptions = map.isEmpty() ? null : map;
  }

  /**
   * 값 목록: 한 줄에 "저장값=표시" (= 가 없으면 저장값을 그대로 보인다). 있으면 목록 칸은 표시 글자로 보이고, 검색칸 · 작업 입력칸은 이 목록의 셀렉트가 된다
   */
  public String getValueOptions() {
    return displayOption(VALUE_OPTIONS);
  }

  public void setValueOptions(String valueOptions) {
    setDisplayOption(VALUE_OPTIONS, valueOptions);
  }

  /** 날짜 · 일시 필드의 목록 표시 형식(DateTimeFormatter 패턴, 예: yyyy.MM.dd). 없으면 기본 모양 */
  public String getDateFormat() {
    return displayOption(DATE_FORMAT);
  }

  public void setDateFormat(String dateFormat) {
    setDisplayOption(DATE_FORMAT, dateFormat);
  }

  /** 값 목록을 저장값 → 표시(적은 순서)로. 없으면 빈 맵 */
  public java.util.Map<String, String> options() {
    var options = new java.util.LinkedHashMap<String, String>();
    var valueOptions = getValueOptions();
    if (valueOptions == null) {
      return options;
    }
    for (var line : valueOptions.split("\\R")) {
      if (line.isBlank()) {
        continue;
      }
      var eq = line.indexOf('=');
      var value = (eq < 0 ? line : line.substring(0, eq)).strip();
      options.putIfAbsent(value, eq < 0 ? value : line.substring(eq + 1).strip());
    }
    return options;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public boolean isVirtual() {
    return virtual;
  }

  public void setVirtual(boolean virtual) {
    this.virtual = virtual;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDbComment() {
    return dbComment;
  }

  public void setDbComment(String dbComment) {
    this.dbComment = dbComment;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public InputType getInputType() {
    return inputType;
  }

  public void setInputType(InputType inputType) {
    this.inputType = inputType;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public Integer getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(Integer maxLength) {
    this.maxLength = maxLength;
  }

  public boolean isAutoIncrement() {
    return autoIncrement;
  }

  public void setAutoIncrement(boolean autoIncrement) {
    this.autoIncrement = autoIncrement;
  }

  public boolean isSortable() {
    return sortable;
  }

  public void setSortable(boolean sortable) {
    this.sortable = sortable;
  }

  public boolean isSortableUserSet() {
    return sortableUserSet;
  }

  public void setSortableUserSet(boolean sortableUserSet) {
    this.sortableUserSet = sortableUserSet;
  }

  public Boolean getUpdatable() {
    return updatable;
  }

  public void setUpdatable(Boolean updatable) {
    this.updatable = updatable;
  }

  /** 수정 작업에서 바꿀 수 있는가(정하지 않았으면 바꿀 수 있는 것으로 본다) */
  public boolean isUpdatable() {
    return !Boolean.FALSE.equals(updatable);
  }

  public boolean isUpdatableUserSet() {
    return updatableUserSet;
  }

  public void setUpdatableUserSet(boolean updatableUserSet) {
    this.updatableUserSet = updatableUserSet;
  }

  public boolean isMissingInDb() {
    return missingInDb;
  }

  public void setMissingInDb(boolean missingInDb) {
    this.missingInDb = missingInDb;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }
}
