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
@Table(name = "SubMenus")
@IdClass(SubMenuId.class)
public class SubMenu extends Setting {
	
	@Id
	@Column(length = 20)
	private String product;
	
	@Id
	@Column(length = 40)
	private String mainMenu;
	
	@Id
	@Column(length = 40)
	private String subMenu;
	
	private String subMenuName;
	
	private String template;
	
	private int displayOrder;
	
	@Column
	private Short groupNo;
	
	private String groupTemplate;
	
	@Column
	private Short pageSize;
	
	private boolean enableCount;
	
	private boolean enableExcel;
	
	private boolean enableInsert;
	
	private boolean enableUpdate;
	
	private boolean enableDelete;

}
