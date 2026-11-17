package net.luversof.web.dynamiccrud2.adapter.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter.Sort;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.ResultColumn;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.SearchCondition;

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
}
