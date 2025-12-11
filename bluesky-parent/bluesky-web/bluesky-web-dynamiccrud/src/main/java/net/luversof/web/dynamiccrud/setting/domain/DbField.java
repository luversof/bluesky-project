package net.luversof.web.dynamiccrud.setting.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "DbField", uniqueConstraints = @UniqueConstraint(columnNames = { "adminProjectId", "projectId",
		"mainMenuId", "subMenuId", "columnId" }))
public class DbField extends Setting {

	@Column(length = 20)
	private String adminProjectId;

	@Column(length = 20)
	private String projectId;

	@Column(length = 40)
	private String mainMenuId;

	@Column(length = 40)
	private String subMenuId;

	@Column(length = 40)
	private String columnId;

	@Column(length = 40, nullable = false)
	private String columnName;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private DbFieldColumnType columnType;

	@Column
	private Short columnOrder;

	@Column(length = 40)
	private String columnGroupId;

	private String columnDefaultValue;

	private String columnPreset;

	private String columnFormat;

	private String columnValidation;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private DbFieldVisible columnVisible;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private DbFieldEnable enableSearch;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private DbFieldSearchType columnSearchType;

	private String columnSearchDefaultValue;

	private String columnSearchValidation;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private DbFieldEnable enableInsert;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private DbFieldEnable enableUpdate;

	private String formHelpText;

	@Column(length = 40)
	private String formPlaceholder;

	public DbField() {
	}

	public DbField(String adminProjectId, String projectId, String mainMenuId, String subMenuId, String columnId,
			String columnName, DbFieldColumnType columnType, Short columnOrder, String columnGroupId,
			String columnDefaultValue, String columnPreset, String columnFormat, String columnValidation,
			DbFieldVisible columnVisible, DbFieldEnable enableSearch, DbFieldSearchType columnSearchType,
			String columnSearchDefaultValue, String columnSearchValidation, DbFieldEnable enableInsert,
			DbFieldEnable enableUpdate, String formHelpText, String formPlaceholder) {
		this.adminProjectId = adminProjectId;
		this.projectId = projectId;
		this.mainMenuId = mainMenuId;
		this.subMenuId = subMenuId;
		this.columnId = columnId;
		this.columnName = columnName;
		this.columnType = columnType;
		this.columnOrder = columnOrder;
		this.columnGroupId = columnGroupId;
		this.columnDefaultValue = columnDefaultValue;
		this.columnPreset = columnPreset;
		this.columnFormat = columnFormat;
		this.columnValidation = columnValidation;
		this.columnVisible = columnVisible;
		this.enableSearch = enableSearch;
		this.columnSearchType = columnSearchType;
		this.columnSearchDefaultValue = columnSearchDefaultValue;
		this.columnSearchValidation = columnSearchValidation;
		this.enableInsert = enableInsert;
		this.enableUpdate = enableUpdate;
		this.formHelpText = formHelpText;
		this.formPlaceholder = formPlaceholder;
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

	public String getColumnId() {
		return columnId;
	}

	public void setColumnId(String columnId) {
		this.columnId = columnId;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public DbFieldColumnType getColumnType() {
		return columnType;
	}

	public void setColumnType(DbFieldColumnType columnType) {
		this.columnType = columnType;
	}

	public Short getColumnOrder() {
		return columnOrder;
	}

	public void setColumnOrder(Short columnOrder) {
		this.columnOrder = columnOrder;
	}

	public String getColumnGroupId() {
		return columnGroupId;
	}

	public void setColumnGroupId(String columnGroupId) {
		this.columnGroupId = columnGroupId;
	}

	public String getColumnDefaultValue() {
		return columnDefaultValue;
	}

	public void setColumnDefaultValue(String columnDefaultValue) {
		this.columnDefaultValue = columnDefaultValue;
	}

	public String getColumnPreset() {
		return columnPreset;
	}

	public void setColumnPreset(String columnPreset) {
		this.columnPreset = columnPreset;
	}

	public String getColumnFormat() {
		return columnFormat;
	}

	public void setColumnFormat(String columnFormat) {
		this.columnFormat = columnFormat;
	}

	public String getColumnValidation() {
		return columnValidation;
	}

	public void setColumnValidation(String columnValidation) {
		this.columnValidation = columnValidation;
	}

	public DbFieldVisible getColumnVisible() {
		return columnVisible;
	}

	public void setColumnVisible(DbFieldVisible columnVisible) {
		this.columnVisible = columnVisible;
	}

	public DbFieldEnable getEnableSearch() {
		return enableSearch;
	}

	public void setEnableSearch(DbFieldEnable enableSearch) {
		this.enableSearch = enableSearch;
	}

	public DbFieldSearchType getColumnSearchType() {
		return columnSearchType;
	}

	public void setColumnSearchType(DbFieldSearchType columnSearchType) {
		this.columnSearchType = columnSearchType;
	}

	public String getColumnSearchDefaultValue() {
		return columnSearchDefaultValue;
	}

	public void setColumnSearchDefaultValue(String columnSearchDefaultValue) {
		this.columnSearchDefaultValue = columnSearchDefaultValue;
	}

	public String getColumnSearchValidation() {
		return columnSearchValidation;
	}

	public void setColumnSearchValidation(String columnSearchValidation) {
		this.columnSearchValidation = columnSearchValidation;
	}

	public DbFieldEnable getEnableInsert() {
		return enableInsert;
	}

	public void setEnableInsert(DbFieldEnable enableInsert) {
		this.enableInsert = enableInsert;
	}

	public DbFieldEnable getEnableUpdate() {
		return enableUpdate;
	}

	public void setEnableUpdate(DbFieldEnable enableUpdate) {
		this.enableUpdate = enableUpdate;
	}

	public String getFormHelpText() {
		return formHelpText;
	}

	public void setFormHelpText(String formHelpText) {
		this.formHelpText = formHelpText;
	}

	public String getFormPlaceholder() {
		return formPlaceholder;
	}

	public void setFormPlaceholder(String formPlaceholder) {
		this.formPlaceholder = formPlaceholder;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DbField other = (DbField) obj;
		return Objects.equals(adminProjectId, other.adminProjectId)
				&& Objects.equals(columnDefaultValue, other.columnDefaultValue)
				&& Objects.equals(columnFormat, other.columnFormat)
				&& Objects.equals(columnGroupId, other.columnGroupId) && Objects.equals(columnId, other.columnId)
				&& Objects.equals(columnName, other.columnName) && Objects.equals(columnOrder, other.columnOrder)
				&& Objects.equals(columnPreset, other.columnPreset)
				&& Objects.equals(columnSearchDefaultValue, other.columnSearchDefaultValue)
				&& columnSearchType == other.columnSearchType
				&& Objects.equals(columnSearchValidation, other.columnSearchValidation)
				&& columnType == other.columnType && Objects.equals(columnValidation, other.columnValidation)
				&& columnVisible == other.columnVisible && enableInsert == other.enableInsert
				&& enableSearch == other.enableSearch && enableUpdate == other.enableUpdate
				&& Objects.equals(formHelpText, other.formHelpText)
				&& Objects.equals(formPlaceholder, other.formPlaceholder)
				&& Objects.equals(mainMenuId, other.mainMenuId) && Objects.equals(projectId, other.projectId)
				&& Objects.equals(subMenuId, other.subMenuId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(adminProjectId, columnDefaultValue, columnFormat, columnGroupId,
				columnId, columnName, columnOrder, columnPreset, columnSearchDefaultValue, columnSearchType,
				columnSearchValidation, columnType, columnValidation, columnVisible, enableInsert, enableSearch,
				enableUpdate, formHelpText, formPlaceholder, mainMenuId, projectId, subMenuId);
		return result;
	}

	@Override
	public String toString() {
		return "DbField [adminProjectId=" + adminProjectId + ", projectId=" + projectId + ", mainMenuId=" + mainMenuId
				+ ", subMenuId=" + subMenuId + ", columnId=" + columnId + ", columnName=" + columnName + ", columnType="
				+ columnType + ", columnOrder=" + columnOrder + ", columnGroupId=" + columnGroupId
				+ ", columnDefaultValue=" + columnDefaultValue + ", columnPreset=" + columnPreset + ", columnFormat="
				+ columnFormat + ", columnValidation=" + columnValidation + ", columnVisible=" + columnVisible
				+ ", enableSearch=" + enableSearch + ", columnSearchType=" + columnSearchType
				+ ", columnSearchDefaultValue=" + columnSearchDefaultValue + ", columnSearchValidation="
				+ columnSearchValidation + ", enableInsert=" + enableInsert + ", enableUpdate=" + enableUpdate
				+ ", formHelpText=" + formHelpText + ", formPlaceholder=" + formPlaceholder + "]";
	}

	public boolean isColumnVisible() {
		return DbFieldVisible.SHOW.equals(columnVisible);
	}

	public boolean isEnableSearch() {
		return DbFieldEnable.ENABLED.equals(enableSearch) || DbFieldEnable.REQUIRED.equals(enableSearch);
	}

	public boolean isEnableSearchRequired() {
		return DbFieldEnable.REQUIRED.equals(enableSearch);
	}

	public boolean isEnableInsert() {
		return DbFieldEnable.ENABLED.equals(enableInsert) || DbFieldEnable.REQUIRED.equals(enableInsert);
	}

	public boolean isEnableInsertRequired() {
		return DbFieldEnable.REQUIRED.equals(enableInsert);
	}

	public boolean isEnableUpdate() {
		return DbFieldEnable.ENABLED.equals(enableUpdate) || DbFieldEnable.REQUIRED.equals(enableUpdate);
	}

	public boolean isEnableUpdateRequired() {
		return DbFieldEnable.REQUIRED.equals(enableUpdate);
	}

	public boolean isEnableEdit(String modalMode) {
		if ("create".equals(modalMode)) {
			return isEnableInsert();
		}

		if ("update".equals(modalMode)) {
			return isEnableUpdate();
		}
		return false;
	}

	public boolean isEnableEditRequired(String modalMode) {
		if ("create".equals(modalMode)) {
			return isEnableInsertRequired();
		}

		if ("update".equals(modalMode)) {
			return isEnableUpdateRequired();
		}
		return false;
	}
}
