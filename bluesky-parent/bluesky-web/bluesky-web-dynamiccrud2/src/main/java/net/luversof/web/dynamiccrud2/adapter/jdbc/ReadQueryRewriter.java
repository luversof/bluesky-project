package net.luversof.web.dynamiccrud2.adapter.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter.Sort;
import net.luversof.web.dynamiccrud2.adapter.Filter;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.ResultColumn;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.SearchCondition;
import net.luversof.web.dynamiccrud2.config.Msg;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;

/**
 * 조회 쿼리 분석 · 재작성. 저장된 원문은 고치지 않고 실행할 때마다 새로 만든다.
 *
 * <p>JSqlParser 는 이 클래스에서만 쓴다(빈 선택 조건 제거 · 페이징 · 건수 · 정렬 교체 · 결과 컬럼 판별). 해석에 실패한 쿼리는 감싸기 (SELECT *
 * FROM (원문) t)로 대신하고, 이때 선택 조건은 쓸 수 없다.
 */
public final class ReadQueryRewriter {

  private static final Pattern PARAMETER = Pattern.compile("(?<![:\\w]):([A-Za-z_]\\w*)");
  private static final Pattern SUBQUERY = Pattern.compile("(?i)\\bselect\\b");
  public static final String LIMIT_PARAM = "_limit";
  public static final String OFFSET_PARAM = "_offset";

  private ReadQueryRewriter() {}

  public static ReadAnalysis analyze(String sql) {
    var source = QueryText.trimTrailingSemicolon(sql);
    var userPaging = QueryText.parameterNames(source).contains(LIMIT_PARAM);
    PlainSelect select;
    try {
      select = parse(source);
    } catch (JSQLParserException | ClassCastException e) {
      var conditions = new ArrayList<SearchCondition>();
      for (var name : QueryText.parameterNames(source)) {
        if (!name.equals(LIMIT_PARAM) && !name.equals(OFFSET_PARAM)) {
          conditions.add(new SearchCondition(name, null, false));
        }
      }
      return new ReadAnalysis(false, rootMessage(e), conditions, List.of(), userPaging, false);
    }
    return new ReadAnalysis(
        true,
        null,
        conditions(select),
        columns(select),
        userPaging || hasOwnPaging(select),
        hasOrderBy(select));
  }

  /**
   * 한 페이지 조회 쿼리. 페이징 절은 :_limit · :_offset 으로 바인딩한다.
   *
   * @param emptyOptionalParams 값이 비어 조건을 뺄 선택 파라미터
   * @param sort 정렬(없으면 null, 쿼리의 ORDER BY 유지)
   */
  public static String pageQuery(
      String sql, SqlDialect dialect, Set<String> emptyOptionalParams, Sort sort) {
    return pageQuery(sql, dialect, emptyOptionalParams, List.of(), sort);
  }

  /**
   * 한 페이지 조회 쿼리 + 화면 검색 조건. 해석한 쿼리는 WHERE 에 결과 컬럼의 원래 식으로 AND 해 인덱스를 쓸 수 있게 하고, 해석하지 못한 쿼리는 감싼 바깥에서
   * 결과 이름으로 거른다.
   *
   * @throws IllegalArgumentException 검색 조건을 붙일 수 없을 때(메시지는 화면에 보인다)
   */
  public static String pageQuery(
      String sql,
      SqlDialect dialect,
      Set<String> emptyOptionalParams,
      List<Filter> filters,
      Sort sort) {
    var source = QueryText.trimTrailingSemicolon(sql);
    var userPaging = QueryText.parameterNames(source).contains(LIMIT_PARAM);
    PlainSelect select;
    try {
      select = parse(source);
    } catch (JSQLParserException | ClassCastException e) {
      if (userPaging) {
        rejectFiltersOnUserPaging(filters);
        return source;
      }
      var wrapped = "SELECT * FROM (" + source + ") t" + outerWhere(dialect, filters);
      if (sort != null) {
        wrapped += orderByClause(dialect, sort);
      }
      return wrapped + dialect.pagingSuffix(sort != null);
    }
    dropConditions(select, emptyOptionalParams);
    applyFilters(select, filters);
    if (sort != null) {
      var element = new OrderByElement();
      element.setExpression(new Column(dialect.quote(sort.label())));
      element.setAsc(!sort.descending());
      select.setOrderByElements(List.of(element));
    }
    if (userPaging || hasOwnPaging(select)) {
      return select.toString();
    }
    return select + dialect.pagingSuffix(hasOrderBy(select));
  }

  /** 전체 건수 쿼리. ORDER BY 는 빼고 감싼다(MSSQL 은 서브쿼리 안 ORDER BY 를 막는다). */
  public static String countQuery(String sql, Set<String> emptyOptionalParams) {
    return countQuery(sql, SqlDialect.POSTGRESQL, emptyOptionalParams, List.of());
  }

  /** 전체 건수 쿼리 + 화면 검색 조건(pageQuery 와 같은 규칙) */
  public static String countQuery(
      String sql, SqlDialect dialect, Set<String> emptyOptionalParams, List<Filter> filters) {
    var source = QueryText.trimTrailingSemicolon(sql);
    String inner;
    var outer = "";
    try {
      var select = parse(source);
      dropConditions(select, emptyOptionalParams);
      applyFilters(select, filters);
      select.setOrderByElements(null);
      inner = select.toString();
    } catch (JSQLParserException | ClassCastException e) {
      if (QueryText.parameterNames(source).contains(LIMIT_PARAM)) {
        rejectFiltersOnUserPaging(filters);
      }
      inner = source;
      outer = outerWhere(dialect, filters);
    }
    return "SELECT COUNT(*) FROM (" + inner + ") t" + outer;
  }

  /**
   * 검색 조건을 WHERE 에 AND 로 붙인다. 결과 컬럼이 실제 컬럼(p.name)이면 그 식을 쓴다 - 인덱스를 탈 수 있다. 원래 WHERE 는 괄호로 감싸 OR 와
   * 섞여도 뜻이 바뀌지 않게 한다.
   */
  private static void applyFilters(PlainSelect select, List<Filter> filters) {
    if (filters == null || filters.isEmpty()) {
      return;
    }
    var expressions = new HashMap<String, String>();
    for (var item : select.getSelectItems()) {
      if (item.getExpression() instanceof Column column) {
        var label =
            item.getAlias() == null
                ? unquote(column.getColumnName())
                : unquote(item.getAlias().getName());
        expressions.putIfAbsent(label, column.toString());
      }
    }
    var conditions = new ArrayList<String>();
    for (var filter : filters) {
      var parts = new ArrayList<String>();
      for (var label : filter.labels()) {
        var expression = expressions.get(label);
        if (expression == null) {
          throw new IllegalArgumentException(Msg.t("error.search.notColumn", label));
        }
        parts.add(condition(expression, filter));
      }
      conditions.add(parts.size() == 1 ? parts.get(0) : "(" + String.join(" OR ", parts) + ")");
    }
    var combined = String.join(" AND ", conditions);
    var where =
        select.getWhere() == null ? combined : "(" + select.getWhere() + ") AND " + combined;
    try {
      select.setWhere(
          CCJSqlParserUtil.parseCondExpression(
              where, true, parser -> parser.withSquareBracketQuotation(true)));
    } catch (JSQLParserException e) {
      throw new IllegalArgumentException(Msg.t("error.search.attachFailed", rootMessage(e)), e);
    }
  }

  /** 해석하지 못한 쿼리: 감싼 바깥(t)에서 결과 이름으로 거른다 */
  private static String outerWhere(SqlDialect dialect, List<Filter> filters) {
    if (filters == null || filters.isEmpty()) {
      return "";
    }
    var conditions = new ArrayList<String>();
    for (var filter : filters) {
      var parts =
          filter.labels().stream()
              .map(label -> condition("t." + dialect.quote(label), filter))
              .toList();
      conditions.add(parts.size() == 1 ? parts.get(0) : "(" + String.join(" OR ", parts) + ")");
    }
    return " WHERE " + String.join(" AND ", conditions);
  }

  private static String condition(String expression, Filter filter) {
    return switch (filter.operator()) {
      case CONTAINS, STARTS_WITH ->
          "LOWER("
              + expression
              + ") LIKE LOWER(:"
              + filter.param()
              + ") ESCAPE '"
              + Filter.ESCAPE
              + "'";
      case EQUALS -> expression + " = :" + filter.param();
      case RANGE -> {
        var bounds = new ArrayList<String>();
        if (filter.param() != null) {
          bounds.add(expression + " >= :" + filter.param());
        }
        if (filter.paramTo() != null) {
          bounds.add(expression + " <= :" + filter.paramTo());
        }
        yield String.join(" AND ", bounds);
      }
    };
  }

  /** 페이징을 직접 한(:_limit) 해석 불가 쿼리는 바깥에서 거르면 쪽 단위로 걸러져 결과가 틀린다 */
  private static void rejectFiltersOnUserPaging(List<Filter> filters) {
    if (filters != null && !filters.isEmpty()) {
      throw new IllegalArgumentException(Msg.t("error.search.manualPaging"));
    }
  }

  private static PlainSelect parse(String sql) throws JSQLParserException {
    // 이름 붙은 파라미터(:name)는 JdbcNamedParameter 로 해석된다. UNION 등 PlainSelect 가 아니면 감싸기로 대신한다.
    // [이름]: MSSQL 따옴표(SqlDialect.quote 가 늘 붙인다). 기본값은 배열 첨자로 읽어 파싱에 실패한다
    return (PlainSelect) CCJSqlParserUtil.parse(sql, parser -> parser.withSquareBracketQuotation(true));
  }

  private static String orderByClause(SqlDialect dialect, Sort sort) {
    return " ORDER BY " + dialect.quote(sort.label()) + (sort.descending() ? " DESC" : " ASC");
  }

  private static boolean hasOwnPaging(PlainSelect select) {
    return select.getLimit() != null
        || select.getOffset() != null
        || select.getFetch() != null
        || select.getTop() != null;
  }

  private static boolean hasOrderBy(PlainSelect select) {
    return select.getOrderByElements() != null && !select.getOrderByElements().isEmpty();
  }

  /** 최상위 AND 로 연결된 조건들(괄호 안으로는 들어가지 않는다). */
  private static List<Expression> conjuncts(Expression where) {
    var result = new ArrayList<Expression>();
    if (where instanceof AndExpression and) {
      result.addAll(conjuncts(and.getLeftExpression()));
      result.addAll(conjuncts(and.getRightExpression()));
    } else if (where != null) {
      result.add(where);
    }
    return result;
  }

  private static List<String> parameters(Expression expression) {
    var names = new ArrayList<String>();
    var matcher = PARAMETER.matcher(expression.toString());
    while (matcher.find()) {
      names.add(matcher.group(1));
    }
    return names;
  }

  /** 파라미터 하나만 쓰는 단순 비교 · LIKE(서브쿼리 없음)만 선택 조건이 될 수 있다. */
  private static String optionalCapableParameter(Expression conjunct) {
    if (!(conjunct instanceof ComparisonOperator || conjunct instanceof LikeExpression)) {
      return null;
    }
    var names = parameters(conjunct);
    if (names.size() != 1 || SUBQUERY.matcher(conjunct.toString()).find()) {
      return null;
    }
    return names.get(0);
  }

  private static List<SearchCondition> conditions(PlainSelect select) {
    var result = new ArrayList<SearchCondition>();
    for (var conjunct : conjuncts(select.getWhere())) {
      var optional = optionalCapableParameter(conjunct);
      if (optional != null) {
        result.add(new SearchCondition(optional, conjunct.toString(), true));
        continue;
      }
      for (var name : parameters(conjunct)) {
        result.add(new SearchCondition(name, conjunct.toString(), false));
      }
    }
    return result;
  }

  private static void dropConditions(PlainSelect select, Set<String> emptyOptionalParams) {
    if (emptyOptionalParams == null || emptyOptionalParams.isEmpty()) {
      return;
    }
    Expression rebuilt = null;
    for (var conjunct : conjuncts(select.getWhere())) {
      var optional = optionalCapableParameter(conjunct);
      if (optional != null && emptyOptionalParams.contains(optional)) {
        continue;
      }
      rebuilt = rebuilt == null ? conjunct : new AndExpression(rebuilt, conjunct);
    }
    select.setWhere(rebuilt);
  }

  private static List<ResultColumn> columns(PlainSelect select) {
    var tables = new HashMap<String, Table>();
    var tableCount = collectTables(select, tables);
    var single = tableCount == 1 ? tables.values().iterator().next() : null;
    var result = new ArrayList<ResultColumn>();
    for (var item : select.getSelectItems()) {
      var expression = item.getExpression();
      var alias = item.getAlias() == null ? null : unquote(item.getAlias().getName());
      if (expression instanceof AllTableColumns allTable) {
        var table = resolve(tables, allTable.getTable());
        result.add(columnOf("*", null, table, null, ResultColumn.Kind.ALL_TABLE));
      } else if (expression instanceof AllColumns) {
        result.add(columnOf("*", null, single, null, ResultColumn.Kind.ALL));
      } else if (expression instanceof Column column) {
        var table =
            column.getTable() == null || column.getTable().getName() == null
                ? single
                : resolve(tables, column.getTable());
        var columnName = unquote(column.getColumnName());
        result.add(
            columnOf(
                alias == null ? columnName : alias,
                alias,
                table,
                columnName,
                ResultColumn.Kind.COLUMN));
      } else {
        var label = alias == null ? expression.toString() : alias;
        result.add(columnOf(label, alias, null, null, ResultColumn.Kind.EXPRESSION));
      }
    }
    return result;
  }

  private static ResultColumn columnOf(
      String label, String alias, Table table, String columnName, ResultColumn.Kind kind) {
    return new ResultColumn(
        label,
        alias,
        table == null ? null : unquote(table.getSchemaName()),
        table == null ? null : unquote(table.getName()),
        columnName,
        kind);
  }

  /** FROM · JOIN 의 실제 테이블을 별칭(없으면 이름) 으로 모은다. 서브쿼리는 원래 컬럼을 알 수 없어 뺀다. */
  private static int collectTables(PlainSelect select, Map<String, Table> tables) {
    var count = 0;
    var fromItems = new ArrayList<Object>();
    fromItems.add(select.getFromItem());
    if (select.getJoins() != null) {
      select.getJoins().forEach(join -> fromItems.add(join.getFromItem()));
    }
    for (var fromItem : fromItems) {
      if (fromItem == null) {
        continue;
      }
      count++;
      if (fromItem instanceof Table table) {
        var key = table.getAlias() == null ? table.getName() : table.getAlias().getName();
        tables.put(unquote(key).toLowerCase(), table);
      }
    }
    return count;
  }

  private static Table resolve(Map<String, Table> tables, Table reference) {
    return tables.get(unquote(reference.getName()).toLowerCase());
  }

  private static String unquote(String identifier) {
    if (identifier == null || identifier.length() < 2) {
      return identifier;
    }
    char first = identifier.charAt(0);
    char last = identifier.charAt(identifier.length() - 1);
    if ((first == '"' && last == '"')
        || (first == '`' && last == '`')
        || (first == '[' && last == ']')) {
      return identifier.substring(1, identifier.length() - 1);
    }
    return identifier;
  }

  private static String rootMessage(Exception e) {
    Throwable root = e;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    var message = root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    return message.lines().findFirst().orElse(message);
  }
}
