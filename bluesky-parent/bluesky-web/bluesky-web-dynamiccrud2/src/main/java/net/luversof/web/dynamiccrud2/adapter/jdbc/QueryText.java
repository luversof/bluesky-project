package net.luversof.web.dynamiccrud2.adapter.jdbc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

/**
 * SQL 쿼리 원문 검사 · 파라미터 추출. 파싱(JSqlParser) 없이 동작해 벤더 문법에 영향받지 않는다.
 *
 * <p>파라미터 이름은 실행에 쓰는 Spring {@link NamedParameterUtils} 로 뽑아 실행 때와 같게 맞춘다.
 */
public final class QueryText {

  /** Spring 은 :{name} · &amp;name 도 파라미터로 보지만 JSqlParser 와 해석이 갈려 거부한다. */
  private static final Pattern UNSUPPORTED_PARAMETER_FORM = Pattern.compile(":\\{|&\\w");

  private static final Pattern WHERE = Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);

  private QueryText() {}

  /** 문장 종류. 주석 · 공백을 건너뛴 첫 키워드로 판별한다. */
  public static QueryKind kindOf(String sql) {
    var stripped = stripCommentsAndLiterals(sql).strip();
    if (stripped.isEmpty()) {
      throw new BlueskyException(QueryErrorCode.EMPTY_QUERY);
    }
    var firstWord = stripped.split("[\\s(]+", 2)[0].toUpperCase(Locale.ROOT);
    return switch (firstWord) {
      case "SELECT", "WITH" -> QueryKind.READ;
      case "INSERT" -> QueryKind.INSERT;
      case "UPDATE" -> QueryKind.UPDATE;
      case "DELETE" -> QueryKind.DELETE;
      default -> throw new BlueskyException(QueryErrorCode.UNSUPPORTED_STATEMENT, firstWord);
    };
  }

  /** 파라미터 이름(나온 순서, 중복 제거). 지원하지 않는 형식이면 거부한다. */
  public static List<String> parameterNames(String sql) {
    if (UNSUPPORTED_PARAMETER_FORM.matcher(stripCommentsAndLiterals(sql)).find()) {
      throw new BlueskyException(QueryErrorCode.UNSUPPORTED_PARAMETER_FORM);
    }
    var names = new LinkedHashSet<String>();
    var parsedSql = NamedParameterUtils.parseSqlStatement(sql);
    for (var parameter :
        NamedParameterUtils.buildSqlParameterList(parsedSql, new MapSqlParameterSource())) {
      names.add(parameter.getName());
    }
    return new ArrayList<>(names);
  }

  /** 저장 전 검사: 문장 1개, 작업 종류와 문장 종류 일치, WHERE 에 파라미터가 없는 UPDATE · DELETE 거부. */
  public static void validate(String sql, QueryKind expected) {
    var stripped = stripCommentsAndLiterals(sql).strip();
    if (stripped.endsWith(";")) {
      stripped = stripped.substring(0, stripped.length() - 1);
    }
    if (stripped.contains(";")) {
      throw new BlueskyException(QueryErrorCode.MULTIPLE_STATEMENTS);
    }
    var actual = kindOf(sql);
    if (actual != expected) {
      throw new BlueskyException(QueryErrorCode.STATEMENT_KIND_MISMATCH, expected, actual);
    }
    parameterNames(sql);
    if ((actual == QueryKind.UPDATE || actual == QueryKind.DELETE) && !whereHasParameter(sql)) {
      throw new BlueskyException(QueryErrorCode.NO_PARAMETER_IN_MODIFY);
    }
  }

  /**
   * WHERE 뒤에 파라미터가 하나라도 있는가. SET 에만 파라미터가 있으면(UPDATE t SET a = :a) 전체 행이 바뀌므로 거부 대상이다. 주석 · 문자열 안의
   * WHERE 는 보지 않는다(stripCommentsAndLiterals 는 길이를 유지해 위치가 원문과 같다). 괄호 안(SET 의 서브쿼리 등) WHERE 는 문장의
   * WHERE 가 아니므로 괄호 밖 WHERE 만 본다.
   */
  private static boolean whereHasParameter(String sql) {
    var stripped = stripCommentsAndLiterals(sql);
    var matcher = WHERE.matcher(stripped);
    while (matcher.find()) {
      if (depthAt(stripped, matcher.start()) == 0) {
        return !parameterNames(sql.substring(matcher.end())).isEmpty();
      }
    }
    return false;
  }

  private static int depthAt(String stripped, int position) {
    var depth = 0;
    for (int i = 0; i < position; i++) {
      var c = stripped.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
      }
    }
    return depth;
  }

  /** 끝의 세미콜론 하나를 뺀다(실행 · 감싸기 전에). */
  public static String trimTrailingSemicolon(String sql) {
    var trimmed = sql.strip();
    return trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1).strip() : trimmed;
  }

  /** 주석은 공백으로, 문자열 · 따옴표 식별자 안쪽은 공백으로 바꾼다(구조 검사용, 길이 유지). */
  static String stripCommentsAndLiterals(String sql) {
    var out = new StringBuilder(sql.length());
    int i = 0;
    while (i < sql.length()) {
      char c = sql.charAt(i);
      if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
        int end = sql.indexOf('\n', i);
        end = end < 0 ? sql.length() : end;
        out.append(" ".repeat(end - i));
        i = end;
      } else if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
        int end = sql.indexOf("*/", i + 2);
        end = end < 0 ? sql.length() : end + 2;
        out.append(" ".repeat(end - i));
        i = end;
      } else if (c == '\'' || c == '"' || c == '`' || c == '[') {
        char close = c == '[' ? ']' : c;
        int end = i + 1;
        while (end < sql.length()) {
          if (sql.charAt(end) == close) {
            // 같은 따옴표 두 번은 이스케이프
            if (end + 1 < sql.length() && sql.charAt(end + 1) == close) {
              end += 2;
              continue;
            }
            break;
          }
          end++;
        }
        end = Math.min(end + 1, sql.length());
        out.append(c)
            .append(" ".repeat(Math.max(0, end - i - 2)))
            .append(end - i >= 2 ? close : "");
        i = end;
      } else {
        out.append(c);
        i++;
      }
    }
    return out.toString();
  }
}
