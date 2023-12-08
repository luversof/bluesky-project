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
	
	@Column(length = 40, nullable = false)
	private String subMenuName;
	
	@Column(length = 40, nullable = false)
	private String template;
	
	@Column(nullable = false)
	private Short displayOrder;
	
	@Column
	private Short groupNo;
	
	@Column(length = 40)
	private String groupTemplate;
	
	@Column
	private Short pageSize;
	
	@Column(nullable = false)
	private boolean enableCount;
	
	@Column(nullable = false)
	private boolean enableExcel;
	
	@Column(nullable = false)
	private boolean enableInsert;
	
	@Column(nullable = false)
	private boolean enableUpdate;
	
	@Column(nullable = false)
	private boolean enableDelete;
	
	public String getUrl() {
		return String.format("/use/%s/%s/%s", getProduct(), getMainMenu(), getSubMenu());
	}

}
