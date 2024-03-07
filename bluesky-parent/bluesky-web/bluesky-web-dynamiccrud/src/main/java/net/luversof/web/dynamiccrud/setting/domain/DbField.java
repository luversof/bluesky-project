package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DbField", uniqueConstraints = @UniqueConstraint(columnNames = { "adminProjectId", "projectId", "mainMenuId", "subMenuId", "columnId" }))
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
