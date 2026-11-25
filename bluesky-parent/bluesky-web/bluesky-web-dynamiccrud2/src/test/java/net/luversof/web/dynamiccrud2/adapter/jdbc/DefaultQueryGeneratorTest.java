package net.luversof.web.dynamiccrud2.adapter.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Types;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.ColumnMeta;
import net.luversof.web.dynamiccrud2.adapter.TableMeta;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.domain.InputType;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

class DefaultQueryGeneratorTest {

  private static ColumnMeta column(String name, int type, boolean autoIncrement) {
    return new ColumnMeta(name, type, "t", 50, false, null, autoIncrement, null);
  }

  private static final TableMeta ARTICLE =
      new TableMeta(
          new TableRef(null, "public", "Article"),
          List.of(
              column("id", Types.BIGINT, true),
              column("title", Types.VARCHAR, false),
              column("viewCount", Types.INTEGER, false)),
          List.of("id"),
          List.of(),
          Set.of("id"));

  @Test
  void generatesAllKindsWithQuotingAndRowKey() {
    var d = SqlDialect.POSTGRESQL;
    assertThat(DefaultQueryGenerator.generate(QueryKind.READ, ARTICLE, d))
        .isEqualTo(
            "SELECT \"id\", \"title\", \"viewCount\" FROM \"public\".\"Article\" ORDER BY \"id\"");
    assertThat(DefaultQueryGenerator.generate(QueryKind.INSERT, ARTICLE, d))
        .isEqualTo(
            "INSERT INTO \"public\".\"Article\" (\"title\", \"viewCount\") VALUES (:title, :viewCount)");
    assertThat(DefaultQueryGenerator.generate(QueryKind.UPDATE, ARTICLE, d))
        .isEqualTo(
            "UPDATE \"public\".\"Article\" SET \"title\" = :title, \"viewCount\" = :viewCount WHERE \"id\" = :id");
    assertThat(DefaultQueryGenerator.generate(QueryKind.DELETE, ARTICLE, d))
        .isEqualTo("DELETE FROM \"public\".\"Article\" WHERE \"id\" = :id");
  }

  @Test
  void generatedQueriesPassValidation() {
    for (var kind : QueryKind.values()) {
      var sql = DefaultQueryGenerator.generate(kind, ARTICLE, SqlDialect.MSSQL);
      QueryText.validate(sql, kind);
    }
  }

  @Test
  void usesFirstUniqueKeyWithoutPkAndRefusesWithoutAnyKey() {
    var noPk =
        new TableMeta(
            new TableRef("shop", null, "item"),
            List.of(column("code", Types.VARCHAR, false), column("name", Types.VARCHAR, false)),
            List.of(),
            List.of(List.of("code")),
            Set.of());
    assertThat(DefaultQueryGenerator.generate(QueryKind.DELETE, noPk, SqlDialect.MYSQL))
        .isEqualTo("DELETE FROM `shop`.`item` WHERE `code` = :code");
    assertThat(DefaultQueryGenerator.generate(QueryKind.READ, noPk, SqlDialect.MYSQL))
        .isEqualTo("SELECT `code`, `name` FROM `shop`.`item` ORDER BY `code`");

    var noKey =
        new TableMeta(
            new TableRef(null, null, "log"),
            List.of(column("msg", Types.VARCHAR, false)),
            List.of(),
            List.of(),
            Set.of());
    assertThatThrownBy(
            () -> DefaultQueryGenerator.generate(QueryKind.UPDATE, noKey, SqlDialect.MYSQL))
        .isInstanceOf(BlueskyException.class);
    assertThat(DefaultQueryGenerator.generate(QueryKind.READ, noKey, SqlDialect.MYSQL))
        .isEqualTo("SELECT `msg` FROM `log`");
  }

  @Test
  void parameterNameReplacesInvalidCharacters() {
    assertThat(DefaultQueryGenerator.parameterName("my col")).isEqualTo("my_col");
    assertThat(DefaultQueryGenerator.parameterName("1st")).isEqualTo("_1st");
  }

  @Test
  void columnMetaInitialFieldValues() {
    var longText =
        new ColumnMeta("body", Types.VARCHAR, "text", Integer.MAX_VALUE, true, null, false, null);
    assertThat(longText.inputType()).isEqualTo(InputType.TEXTAREA);
    assertThat(longText.maxLength()).isNull();
    assertThat(longText.required()).isFalse();
    var name = new ColumnMeta("name", Types.VARCHAR, "varchar", 100, false, null, false, null);
    assertThat(name.inputType()).isEqualTo(InputType.TEXT);
    assertThat(name.maxLength()).isEqualTo(100);
    assertThat(name.required()).isTrue();
    var withDefault = new ColumnMeta("n", Types.INTEGER, "int4", 10, false, "0", false, null);
    assertThat(withDefault.required()).isFalse();
    assertThat(withDefault.inputType()).isEqualTo(InputType.NUMBER);
  }
}
