package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	private String name;

	private String type;
	
	private String preset;
	
	private String format;
	
	private String validation;
	
	private boolean visible;
	
	private String enableSearch;
	
	private String enableEdit;
	
	@Column
	private Short formSize;
	
	@Column
	private Short formOrder;
}
