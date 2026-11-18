package net.luversof.web.dynamiccrud2.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/** 대상 DB 테이블. 같은 테이블을 쓰는 메뉴들이 필드를 공유한다. */
@Table("DbTable")
public class DbTable {

  @Id
  @Column("id")
  private UUID id;

  /** connectionInfo 의 connection 이름 */
  @Column("connectionName")
  private String connectionName;

  @Column("catalogName")
  private String catalogName;

  @Column("schemaName")
  private String schemaName;

  @Column("tableName")
  private String tableName;

  /** 마지막 "컬럼 다시 읽기" 시각 */
  @Column("columnsReadDate")
  private Instant columnsReadDate;

  @CreatedDate
  @Column("createdDate")
  private Instant createdDate;

  @MappedCollection(idColumn = "dbTable_id")
  private Set<DbField> fields = new LinkedHashSet<>();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getConnectionName() {
    return connectionName;
  }

  public void setConnectionName(String connectionName) {
    this.connectionName = connectionName;
  }

  public String getCatalogName() {
    return catalogName;
  }

  public void setCatalogName(String catalogName) {
    this.catalogName = catalogName;
  }

  public String getSchemaName() {
    return schemaName;
  }

  /**
   * 쿼리에서 테이블 앞에 붙인 이름(qualifier)을 이 테이블과 같은 DB 안의 스키마로 바꾼다. 없거나 이 테이블의 catalog 와 같으면(MySQL 은 DB 이름이
   * catalog 이고 스키마가 없다 - {@code dynamiccrud_localdev.mainmenu}) 이 테이블의 스키마. 그래야 같은 테이블을 둘로 세지 않는다.
   */
  public String schemaOf(String qualifier) {
    return qualifier == null || qualifier.equalsIgnoreCase(catalogName) ? schemaName : qualifier;
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public Instant getColumnsReadDate() {
    return columnsReadDate;
  }

  public void setColumnsReadDate(Instant columnsReadDate) {
    this.columnsReadDate = columnsReadDate;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  public Set<DbField> getFields() {
    return fields;
  }

  public void setFields(Set<DbField> fields) {
    this.fields = fields;
  }
}
