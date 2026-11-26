package net.luversof.web.dynamiccrud2.adapter.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter.Sort;
import net.luversof.web.dynamiccrud2.adapter.Filter;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.ResultColumn;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.SearchCondition;
import net.luversof.web.dynamiccrud2.domain.SearchOperator;

class ReadQueryRewriterTest {

  private static final String JOIN_QUERY =
      """
      SELECT a.id, a.title AS "제목", u.name AS writer_name, CONCAT(a.x, 'y') AS full_name, a.*
      FROM public."Article" a JOIN users u ON u.id = a.user_id
      WHERE a.status = :status AND (a.x = :x OR a.y = :y) AND a.created >= :from
      ORDER BY a.id DESC
      """;

  @Test
  void analyzeFindsSearchConditionsAndWhichCanBeOptional() {
    var analysis = ReadQueryRewriter.analyze(JOIN_QUERY);
    assertThat(analysis.parsed()).isTrue();
    assertThat(analysis.conditions())
        .extracting(SearchCondition::paramName, SearchCondition::optionalCapable)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("status", true),
            org.assertj.core.groups.Tuple.tuple("x", false),
            org.assertj.core.groups.Tuple.tuple("y", false),
            org.assertj.core.groups.Tuple.tuple("from", true));
    assertThat(analysis.hasOrderBy()).isTrue();
    assertThat(analysis.hasOwnPaging()).isFalse();
  }

  @Test
  void analyzeResolvesResultColumnsToSourceTables() {
    var columns = ReadQueryRewriter.analyze(JOIN_QUERY).columns();
    assertThat(columns)
        .extracting(
            ResultColumn::label,
            ResultColumn::alias,
            ResultColumn::tableSchema,
            ResultColumn::tableName,
            ResultColumn::columnName,
            ResultColumn::kind)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                "id", null, "public", "Article", "id", ResultColumn.Kind.COLUMN),
            org.assertj.core.groups.Tuple.tuple(
                "제목", "제목", "public", "Article", "title", ResultColumn.Kind.COLUMN),
            org.assertj.core.groups.Tuple.tuple(
                "writer_name", "writer_name", null, "users", "name", ResultColumn.Kind.COLUMN),
            org.assertj.core.groups.Tuple.tuple(
                "full_name", "full_name", null, null, null, ResultColumn.Kind.EXPRESSION),
            org.assertj.core.groups.Tuple.tuple(
                "*", null, "public", "Article", null, ResultColumn.Kind.ALL_TABLE));
  }

  @Test
  void pageQueryDropsEmptyOptionalConditionAndReplacesOrderBy() {
    var sql =
        ReadQueryRewriter.pageQuery(
            JOIN_QUERY, SqlDialect.POSTGRESQL, Set.of("status", "x"), new Sort("제목", false));
    assertThat(sql)
        .doesNotContain(":status")
        // OR 안의 :x 는 선택 조건이 될 수 없어 남는다
        .contains("(a.x = :x OR a.y = :y)")
        .contains("a.created >= :from")
        .contains("ORDER BY \"제목\"")
        .doesNotContain("a.id DESC")
        .endsWith(" LIMIT :_limit OFFSET :_offset");
  }

  @Test
  void pageQueryRemovesWhereWhenAllConditionsDropped() {
    var sql =
        ReadQueryRewriter.pageQuery(
            "SELECT * FROM t WHERE a = :a", SqlDialect.MYSQL, Set.of("a"), null);
    assertThat(sql).isEqualTo("SELECT * FROM t LIMIT :_limit OFFSET :_offset");
  }

  @Test
  void pageQueryForMssqlAddsOrderByWhenMissing() {
    assertThat(ReadQueryRewriter.pageQuery("SELECT * FROM t", SqlDialect.MSSQL, Set.of(), null))
        .isEqualTo(
            "SELECT * FROM t ORDER BY (SELECT NULL) OFFSET :_offset ROWS FETCH NEXT :_limit ROWS ONLY");
    assertThat(
            ReadQueryRewriter.pageQuery(
                "SELECT * FROM t ORDER BY id", SqlDialect.MSSQL, Set.of(), null))
        .isEqualTo("SELECT * FROM t ORDER BY id OFFSET :_offset ROWS FETCH NEXT :_limit ROWS ONLY");
  }

  @Test
  void pageQueryKeepsQueryOwnPaging() {
    assertThat(
            ReadQueryRewriter.pageQuery(
                "SELECT * FROM t LIMIT 10", SqlDialect.POSTGRESQL, Set.of(), null))
        .isEqualTo("SELECT * FROM t LIMIT 10");
    var userPaging = "SELECT * FROM t LIMIT :_limit OFFSET :_offset";
    assertThat(ReadQueryRewriter.analyze(userPaging).hasOwnPaging()).isTrue();
    assertThat(ReadQueryRewriter.pageQuery(userPaging, SqlDialect.POSTGRESQL, Set.of(), null))
        .isEqualTo(userPaging);
  }

  @Test
  void unparseableQueryFallsBackToWrappingAndRequiredConditions() {
    var union = "SELECT a FROM t WHERE a = :a UNION SELECT b FROM u;";
    var analysis = ReadQueryRewriter.analyze(union);
    assertThat(analysis.parsed()).isFalse();
    assertThat(analysis.conditions())
        .extracting(SearchCondition::paramName, SearchCondition::optionalCapable)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("a", false));
    assertThat(ReadQueryRewriter.pageQuery(union, SqlDialect.POSTGRESQL, Set.of("a"), null))
        .isEqualTo(
            "SELECT * FROM (SELECT a FROM t WHERE a = :a UNION SELECT b FROM u) t"
                + " LIMIT :_limit OFFSET :_offset");
  }

  @Test
  void countQueryDropsOrderBy() {
    assertThat(ReadQueryRewriter.countQuery("SELECT * FROM t WHERE a = :a ORDER BY id", Set.of()))
        .isEqualTo("SELECT COUNT(*) FROM (SELECT * FROM t WHERE a = :a) t");
  }

  @Test
  void filtersUseOriginalColumnExpressionAndKeepOrInsideParentheses() {
    var filters =
        java.util.List.of(
            new Filter(java.util.List.of("제목"), SearchOperator.CONTAINS, "_f0", null),
            new Filter(java.util.List.of("id"), SearchOperator.RANGE, null, "_f1_to"),
            new Filter(
                java.util.List.of("제목", "writer_name"), SearchOperator.CONTAINS, "_q", null));
    var sql =
        ReadQueryRewriter.pageQuery(JOIN_QUERY, SqlDialect.POSTGRESQL, Set.of(), filters, null);
    // 별칭이 아니라 원래 식(a.title · u.name)으로 걸러야 인덱스를 탈 수 있다
    assertThat(sql)
        .contains("LOWER(a.title) LIKE LOWER(:_f0) ESCAPE '!'")
        .contains("a.id <= :_f1_to")
        .contains("LOWER(u.name) LIKE LOWER(:_q) ESCAPE '!'")
        .contains(" OR LOWER(u.name)")
        // 원래 WHERE 의 OR 가 새 AND 와 섞이지 않게 괄호로 감싼다
        .containsPattern("WHERE \\(+a\\.status = :status");
    assertThat(sql).doesNotContain(":_f1 ");
    assertThat(ReadQueryRewriter.countQuery(JOIN_QUERY, SqlDialect.POSTGRESQL, Set.of(), filters))
        .contains("LOWER(a.title) LIKE LOWER(:_f0)")
        .doesNotContain("ORDER BY");
  }

  @Test
  void filtersOnComputedColumnAreRejectedAndUnparsedQueryFiltersOutside() {
    var computed =
        java.util.List.of(
            new Filter(java.util.List.of("full_name"), SearchOperator.EQUALS, "_f0", null));
    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            ReadQueryRewriter.pageQuery(
                JOIN_QUERY, SqlDialect.POSTGRESQL, Set.of(), computed, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("full_name");

    var union = "SELECT a FROM x UNION SELECT a FROM y";
    var filters =
        java.util.List.of(new Filter(java.util.List.of("a"), SearchOperator.EQUALS, "_f0", null));
    assertThat(ReadQueryRewriter.pageQuery(union, SqlDialect.POSTGRESQL, Set.of(), filters, null))
        .startsWith("SELECT * FROM (" + union + ") t WHERE t.\"a\" = :_f0");
    assertThat(ReadQueryRewriter.countQuery(union, SqlDialect.POSTGRESQL, Set.of(), filters))
        .isEqualTo("SELECT COUNT(*) FROM (" + union + ") t WHERE t.\"a\" = :_f0");
  }

  /** MSSQL [이름] 따옴표도 파싱한다 - 못 하면 건수 쿼리에 ORDER BY 가 남아 MSSQL 이 거부한다 */
  @Test
  void parsesSquareBracketNames() {
    var sql = "SELECT [name] FROM [Item] WHERE [name] LIKE :q ORDER BY [name]";
    assertThat(ReadQueryRewriter.countQuery(sql, SqlDialect.MSSQL, Set.of(), java.util.List.of()))
        .doesNotContain("ORDER BY");
  }

  /** FROM 이 하위 쿼리 하나면 결과 컬럼의 테이블을 모른다(DMT2 59fa9fd - 분석이 NoSuchElementException 으로 죽던 것) */
  @Test
  void analyzeSubqueryFrom() {
    var analysis =
        ReadQueryRewriter.analyze(
            "SELECT date, count FROM (SELECT d AS date, COUNT(*) AS count FROM t GROUP BY d) AS x ORDER BY date");
    assertThat(analysis.parsed()).isTrue();
    assertThat(analysis.columns())
        .extracting(c -> c.label() + ":" + c.tableName())
        .containsExactly("date:null", "count:null");
    assertThat(ReadQueryRewriter.analyze("SELECT * FROM (SELECT 1 AS a) x").columns()).hasSize(1);
  }

  /** 해석 못 하는 쿼리(JSqlParser 5.4 는 컬럼 이름 number 를 못 읽는다)의 끝 ORDER BY 는 감싼 바깥으로 - MSSQL 은 감싼 안쪽 ORDER BY 를 막는다(DMT2 e4fec06) */
  @Test
  void unparsedQueryMovesTrailingOrderByOutside() {
    var sql =
        "SELECT idx, number, name FROM dbo.promotion_coupon WITH(NOLOCK) WHERE name LIKE '%a, b%' ORDER BY idx DESC, c.name";
    assertThat(ReadQueryRewriter.analyze(sql).parsed()).isFalse();
    var body =
        "SELECT idx, number, name FROM dbo.promotion_coupon WITH(NOLOCK) WHERE name LIKE '%a, b%'";
    assertThat(ReadQueryRewriter.pageQuery(sql, SqlDialect.MSSQL, Set.of(), null))
        .isEqualTo(
            "SELECT * FROM ("
                + body
                + ") t ORDER BY t.[idx] DESC, t.[name]"
                + SqlDialect.MSSQL.pagingSuffix(true));
    // 화면 정렬이 있으면 원래 ORDER BY 는 버린다
    assertThat(ReadQueryRewriter.pageQuery(sql, SqlDialect.MSSQL, Set.of(), new Sort("name", false)))
        .isEqualTo(
            "SELECT * FROM (" + body + ") t ORDER BY [name] ASC" + SqlDialect.MSSQL.pagingSuffix(true));
    assertThat(ReadQueryRewriter.countQuery(sql, SqlDialect.MSSQL, Set.of(), java.util.List.of()))
        .isEqualTo("SELECT COUNT(*) FROM (" + body + ") t");
    // 식 정렬은 건드리지 않는다
    var expr = "SELECT number FROM t ORDER BY LEN(number)";
    assertThat(ReadQueryRewriter.countQuery(expr, SqlDialect.MSSQL, Set.of(), java.util.List.of()))
        .isEqualTo("SELECT COUNT(*) FROM (" + expr + ") t");
  }

  @Test
  void likeValuesEscapeWildcards() {
    assertThat(Filter.escapeLike("10%_a!b")).isEqualTo("10!%!_a!!b");
  }
}
