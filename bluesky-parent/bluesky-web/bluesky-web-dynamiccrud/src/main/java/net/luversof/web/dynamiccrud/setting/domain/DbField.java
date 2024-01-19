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
	private DbFieldColumnType columnType;		// BOOLEAN, DATE, INT, LINK, LONG, STRING, TEXT
	
	private String columnPreset;
	
	private String columnFormat;
	
	private String columnValidation;
	
	@Column(nullable = false)
	private boolean columnVisible;
	
	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private DbFieldEnable enableSearch;	// DISABLED, ENABLED, REQUIRED
	
	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private DbFieldEnable enableEdit;	// DISABLED, ENABLED, REQUIRED
	
	@Column
	private Short formSize;
	
	@Column
	private Short formOrder;	// 입력 폼 순서를 지정하면서 동시에 목록의 순서로도 사용됨
	
	public boolean isEnableSearch() {
		return DbFieldEnable.ENABLED.equals(enableSearch) || DbFieldEnable.REQUIRED.equals(enableSearch);
	}
	
	public boolean isEnableSearchRequired() {
		return DbFieldEnable.REQUIRED.equals(enableSearch);
	}
	
	public boolean isEnableEdit() {
		return DbFieldEnable.ENABLED.equals(enableEdit) || DbFieldEnable.REQUIRED.equals(enableEdit);
	}
	
	public boolean isEnableEditRequired() {
		return DbFieldEnable.REQUIRED.equals(enableEdit);
	}
}
