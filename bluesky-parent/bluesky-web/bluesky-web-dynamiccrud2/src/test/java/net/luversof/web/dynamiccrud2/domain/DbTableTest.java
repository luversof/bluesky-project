package net.luversof.web.dynamiccrud2.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 쿼리의 테이블 한정자 → 스키마: 같은 테이블을 둘로 세지 않는다(MySQL 자동 생성 쿼리는 DB 이름을 붙인다) */
class DbTableTest {

  private static DbTable table(String catalog, String schema) {
    var dbTable = new DbTable();
    dbTable.setCatalogName(catalog);
    dbTable.setSchemaName(schema);
    dbTable.setTableName("mainmenu");
    return dbTable;
  }

  @Test
  void mysqlDatabaseNameIsSameTable() {
    var mysql = table("dynamiccrud_localdev", null);
    assertThat(mysql.schemaOf("dynamiccrud_localdev")).isNull();
    assertThat(mysql.schemaOf("DYNAMICCRUD_LOCALDEV")).isNull();
    assertThat(mysql.schemaOf(null)).isNull();
    assertThat(mysql.schemaOf("other_db")).isEqualTo("other_db");
  }

  @Test
  void postgresqlSchemaStays() {
    var postgres = table("dynamiccrud2", "public");
    assertThat(postgres.schemaOf(null)).isEqualTo("public");
    assertThat(postgres.schemaOf("public")).isEqualTo("public");
    assertThat(postgres.schemaOf("audit")).isEqualTo("audit");
  }
}
