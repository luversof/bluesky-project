package net.luversof.web.dynamiccrud.use;

import java.util.List;
import java.util.Objects;

public class QueryCase {

  private QueryCaseDbType dbType;

  private String queryStr;

  public QueryCase() {}

  public QueryCase(QueryCaseDbType dbType, String queryStr) {
    this.dbType = dbType;
    this.queryStr = queryStr;
  }

  public QueryCaseDbType getDbType() {
    return dbType;
  }

  public void setDbType(QueryCaseDbType dbType) {
    this.dbType = dbType;
  }

  public String getQueryStr() {
    return queryStr;
  }

  public void setQueryStr(String queryStr) {
    this.queryStr = queryStr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryCase queryCase = (QueryCase) o;
    return dbType == queryCase.dbType && Objects.equals(queryStr, queryCase.queryStr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dbType, queryStr);
  }

  @Override
  public String toString() {
    return "QueryCase{" + "dbType=" + dbType + ", queryStr='" + queryStr + '\'' + '}';
  }

  public static List<QueryCase> of(QueryCaseDbType k1, String v1) {
    return List.of(new QueryCase(k1, v1));
  }

  public static List<QueryCase> of(QueryCaseDbType k1, String v1, QueryCaseDbType k2, String v2) {
    return List.of(new QueryCase(k1, v1), new QueryCase(k2, v2));
  }

  public static List<QueryCase> of(
      QueryCaseDbType k1, String v1, QueryCaseDbType k2, String v2, QueryCaseDbType k3, String v3) {
    return List.of(new QueryCase(k1, v1), new QueryCase(k2, v2), new QueryCase(k3, v3));
  }

  public static List<QueryCase> of(
      QueryCaseDbType k1,
      String v1,
      QueryCaseDbType k2,
      String v2,
      QueryCaseDbType k3,
      String v3,
      QueryCaseDbType k4,
      String v4) {
    return List.of(
        new QueryCase(k1, v1), new QueryCase(k2, v2), new QueryCase(k3, v3), new QueryCase(k4, v4));
  }
}
