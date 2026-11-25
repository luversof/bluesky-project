package net.luversof.web.dynamiccrud2.adapter.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

class QueryTextTest {

  /** 실행 계획(GENERIC_PLAN)용 번호 파라미터: 같은 이름은 같은 번호, 문자열 · 주석 · 형 변환(::)은 그대로 */
  @Test
  void toNumberedParametersKeepsLiteralsAndCasts() {
    assertThat(
            QueryText.toNumberedParameters(
                "SELECT ':no', a::text /* :c */ FROM t WHERE a = :a AND (b = :이름 OR :a IS NULL)"))
        .isEqualTo("SELECT ':no', a::text /* :c */ FROM t WHERE a = $1 AND (b = $2 OR $1 IS NULL)");
  }

  @Test
  void kindOfSkipsCommentsAndReadsFirstKeyword() {
    assertThat(QueryText.kindOf("-- 목록\n/* x */ select 1")).isEqualTo(QueryKind.READ);
    assertThat(QueryText.kindOf("WITH t AS (SELECT 1) SELECT * FROM t")).isEqualTo(QueryKind.READ);
    assertThat(QueryText.kindOf("insert into a values (:a)")).isEqualTo(QueryKind.INSERT);
    assertThat(QueryText.kindOf("UPDATE a SET b = :b WHERE id = :id")).isEqualTo(QueryKind.UPDATE);
    assertThat(QueryText.kindOf("DELETE FROM a WHERE id = :id")).isEqualTo(QueryKind.DELETE);
    assertThatThrownBy(() -> QueryText.kindOf("DROP TABLE a")).isInstanceOf(BlueskyException.class);
    assertThatThrownBy(() -> QueryText.kindOf("  -- only comment"))
        .isInstanceOf(BlueskyException.class);
  }

  @Test
  void parameterNamesIgnoreLiteralsCommentsAndPostgresCast() {
    var sql =
        "SELECT ':no', a::text FROM t -- :no2\n WHERE b = :b AND c LIKE CONCAT('%', :c, '%') AND d = :b";
    assertThat(QueryText.parameterNames(sql)).containsExactly("b", "c");
  }

  @Test
  void parameterNamesRejectSpringOnlyForms() {
    assertThatThrownBy(() -> QueryText.parameterNames("SELECT * FROM t WHERE a = :{a}"))
        .isInstanceOf(BlueskyException.class)
        .extracting("errorCode")
        .isEqualTo("QueryErrorCode.UNSUPPORTED_PARAMETER_FORM");
    // 공백 없는 비트 연산은 Spring 이 &b 를 파라미터로 오인한다
    assertThatThrownBy(() -> QueryText.parameterNames("SELECT a&b FROM t"))
        .isInstanceOf(BlueskyException.class);
    // 공백이 있거나 문자열 안이면 괜찮다
    assertThat(QueryText.parameterNames("SELECT a & b, '&x' FROM t")).isEmpty();
  }

  @Test
  void validateRejectsMultipleStatementsButAllowsTrailingSemicolon() {
    QueryText.validate("SELECT * FROM t WHERE a = ';' ;", QueryKind.READ);
    assertThatThrownBy(() -> QueryText.validate("SELECT 1; DELETE FROM t", QueryKind.READ))
        .isInstanceOf(BlueskyException.class)
        .extracting("errorCode")
        .isEqualTo("QueryErrorCode.MULTIPLE_STATEMENTS");
  }

  @Test
  void validateRejectsKindMismatchAndModifyWithoutParameter() {
    assertThatThrownBy(() -> QueryText.validate("DELETE FROM t WHERE id = :id", QueryKind.UPDATE))
        .isInstanceOf(BlueskyException.class)
        .extracting("errorCode")
        .isEqualTo("QueryErrorCode.STATEMENT_KIND_MISMATCH");
    for (var sql :
        new String[] {
          "UPDATE t SET a = 1",
          // SET 에만 파라미터 - WHERE 가 없어 전체 행이 바뀐다
          "UPDATE t SET a = :a",
          "UPDATE t SET a = :a WHERE b = 1",
          "DELETE FROM t -- WHERE id = :id",
          "DELETE FROM t WHERE note = ':id'",
          // 서브쿼리의 WHERE 는 문장의 WHERE 가 아니다 - 전체 행이 바뀐다
          "UPDATE t SET a = (SELECT b FROM u WHERE u.id = :id)"
        }) {
      var kind = sql.startsWith("UPDATE") ? QueryKind.UPDATE : QueryKind.DELETE;
      assertThatThrownBy(() -> QueryText.validate(sql, kind))
          .as(sql)
          .isInstanceOf(BlueskyException.class)
          .extracting("errorCode")
          .isEqualTo("QueryErrorCode.NO_PARAMETER_IN_MODIFY");
    }
    QueryText.validate("UPDATE t SET a = :a WHERE id = :id", QueryKind.UPDATE);
    QueryText.validate(
        "UPDATE t SET a = (SELECT b FROM u WHERE u.id = :b) WHERE id = :id", QueryKind.UPDATE);
    QueryText.validate("delete from t where id in (:ids)", QueryKind.DELETE);
  }

  @Test
  void dialectAlwaysQuotes() {
    assertThat(SqlDialect.POSTGRESQL.quote("article")).isEqualTo("\"article\"");
    assertThat(SqlDialect.POSTGRESQL.quote("MenuQuery")).isEqualTo("\"MenuQuery\"");
    assertThat(SqlDialect.POSTGRESQL.quote("a\"b")).isEqualTo("\"a\"\"b\"");
    assertThat(SqlDialect.MYSQL.quote("MenuQuery")).isEqualTo("`MenuQuery`");
    // MySQL 8 예약어 컬럼(예: virtual)
    assertThat(SqlDialect.MYSQL.quote("virtual")).isEqualTo("`virtual`");
    assertThat(SqlDialect.MYSQL.quote("my col")).isEqualTo("`my col`");
    assertThat(SqlDialect.MSSQL.quote("a]b")).isEqualTo("[a]]b]");
    assertThat(SqlDialect.MYSQL.qualifiedName("shop", null, "item")).isEqualTo("`shop`.`item`");
    assertThat(SqlDialect.POSTGRESQL.qualifiedName("db", "public", "Menu"))
        .isEqualTo("\"public\".\"Menu\"");
  }

  @Test
  void dialectOfProductName() {
    assertThat(SqlDialect.of("PostgreSQL")).isEqualTo(SqlDialect.POSTGRESQL);
    assertThat(SqlDialect.of("MariaDB")).isEqualTo(SqlDialect.MYSQL);
    assertThat(SqlDialect.of("MySQL")).isEqualTo(SqlDialect.MYSQL);
    assertThat(SqlDialect.of("Microsoft SQL Server")).isEqualTo(SqlDialect.MSSQL);
    assertThatThrownBy(() -> SqlDialect.of("Oracle")).isInstanceOf(BlueskyException.class);
  }
}
