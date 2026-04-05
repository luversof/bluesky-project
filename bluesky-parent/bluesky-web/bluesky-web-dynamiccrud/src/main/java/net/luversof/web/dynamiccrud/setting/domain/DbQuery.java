package net.luversof.web.dynamiccrud.setting.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "DbQuery",
    uniqueConstraints =
        @UniqueConstraint(
            columnNames = {
              "adminProjectId",
              "projectId",
              "mainMenuId",
              "subMenuId",
              "sqlCommandType"
            }))
public class DbQuery extends Setting {

  @Column(length = 20)
  private String adminProjectId;

  @Column(length = 20)
  private String projectId;

  @Column(length = 40)
  private String mainMenuId;

  @Column(length = 40)
  private String subMenuId;

  @Column(length = 20)
  @Enumerated(EnumType.STRING)
  private DbQuerySqlCommandType sqlCommandType; // INSERT, SELECT, UPDATE, DELETE

  @Column(length = 40, nullable = false)
  private String dataSourceName;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String queryString;

  public DbQuery() {}

  public DbQuery(
      String adminProjectId,
      String projectId,
      String mainMenuId,
      String subMenuId,
      DbQuerySqlCommandType sqlCommandType,
      String dataSourceName,
      String queryString) {
    this.adminProjectId = adminProjectId;
    this.projectId = projectId;
    this.mainMenuId = mainMenuId;
    this.subMenuId = subMenuId;
    this.sqlCommandType = sqlCommandType;
    this.dataSourceName = dataSourceName;
    this.queryString = queryString;
  }

  public String getAdminProjectId() {
    return adminProjectId;
  }

  public void setAdminProjectId(String adminProjectId) {
    this.adminProjectId = adminProjectId;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getMainMenuId() {
    return mainMenuId;
  }

  public void setMainMenuId(String mainMenuId) {
    this.mainMenuId = mainMenuId;
  }

  public String getSubMenuId() {
    return subMenuId;
  }

  public void setSubMenuId(String subMenuId) {
    this.subMenuId = subMenuId;
  }

  public DbQuerySqlCommandType getSqlCommandType() {
    return sqlCommandType;
  }

  public void setSqlCommandType(DbQuerySqlCommandType sqlCommandType) {
    this.sqlCommandType = sqlCommandType;
  }

  public String getDataSourceName() {
    return dataSourceName;
  }

  public void setDataSourceName(String dataSourceName) {
    this.dataSourceName = dataSourceName;
  }

  public String getQueryString() {
    return queryString;
  }

  public void setQueryString(String queryString) {
    this.queryString = queryString;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    DbQuery other = (DbQuery) obj;
    return Objects.equals(adminProjectId, other.adminProjectId)
        && Objects.equals(dataSourceName, other.dataSourceName)
        && Objects.equals(mainMenuId, other.mainMenuId)
        && Objects.equals(projectId, other.projectId)
        && Objects.equals(queryString, other.queryString)
        && sqlCommandType == other.sqlCommandType
        && Objects.equals(subMenuId, other.subMenuId);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result
            + Objects.hash(
                adminProjectId,
                dataSourceName,
                mainMenuId,
                projectId,
                queryString,
                sqlCommandType,
                subMenuId);
    return result;
  }

  @Override
  public String toString() {
    return "DbQuery [adminProjectId="
        + adminProjectId
        + ", projectId="
        + projectId
        + ", mainMenuId="
        + mainMenuId
        + ", subMenuId="
        + subMenuId
        + ", sqlCommandType="
        + sqlCommandType
        + ", dataSourceName="
        + dataSourceName
        + ", queryString="
        + queryString
        + "]";
  }
}
