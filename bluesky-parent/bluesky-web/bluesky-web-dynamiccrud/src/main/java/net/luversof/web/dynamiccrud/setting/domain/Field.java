package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Fields")
@IdClass(FieldId.class)
public class Field extends Setting {
	
	@Id
	@Column(length = 20)
	private String product;
	
	@Id
	@Column(length = 40)
	private String mainMenu;
	
	@Id
	@Column(length = 40)
	private String subMenu;
	
	@Id
	@Column(length = 40)
	private String column;

	@Column(length = 40, nullable = false)
	private String name;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private FieldType type;		// BOOLEAN, DATE, INT, LINK, LONG, STRING, TEXT
	
	private String preset;
	
	private String format;
	
	private String validation;
	
	@Column(nullable = false)
	private boolean visible;
	
	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private FieldEnable enableSearch;	// DISABLED, ENABLED, REQUIRED
	
	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private FieldEnable enableEdit;	// DISABLED, ENABLED, REQUIRED
	
	@Column
	private Short formSize;
	
	@Column
	private Short formOrder;
	
	public boolean isEnableSearch() {
		return FieldEnable.ENABLED.equals(enableSearch) || FieldEnable.REQUIRED.equals(enableSearch);
	}
	
	public boolean isEnableSearchRequired() {
		return FieldEnable.REQUIRED.equals(enableSearch);
	}
	
	public boolean isEnableEdit() {
		return FieldEnable.ENABLED.equals(enableEdit) || FieldEnable.REQUIRED.equals(enableEdit);
	}
	
	public boolean isEnableEditRequired() {
		return FieldEnable.REQUIRED.equals(enableEdit);
	}
}
