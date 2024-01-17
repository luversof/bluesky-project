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
import net.luversof.web.dynamiccrud.setting.constant.SettingConstant;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SubMenu", uniqueConstraints = @UniqueConstraint(columnNames = { "adminProjectId", "projectId", "mainMenuId", "subMenuId" }))
public class SubMenu extends Setting {
	
	@Column(length = 20)
	private String adminProjectId;
	
	@Column(length = 20)
	private String projectId;
	
	@Column(length = 40)
	private String mainMenuId;
	
	@Column(length = 40)
	private String subMenuId;
	
	@Column(length = 40, nullable = false)
	private String subMenuName;
	
	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private SubMenuDbType dbType;	//MsSql, MySql
	
	@Column(nullable = false)
	private Short displayOrder;
	
	@Column
	private Short pageSize;

	@Column(nullable = false)
	private boolean enableExcel;
	
	@Column(nullable = false)
	private boolean enableInsert;
	
	@Column(nullable = false)
	private boolean enableUpdate;
	
	@Column(nullable = false)
	private boolean enableDelete;
	
	public String getUrl() {
		if (SettingConstant.KEY_ADMIN_PROJECT.equals(getAdminProjectId())) {
			return String.format("/%s/use/%s/%s/%s", getAdminProjectId(), getProjectId(), getMainMenuId(), getSubMenuId());
		} else {
			return String.format("/%s/use/%s/%s/%s", getAdminProjectId(), getProjectId(), getMainMenuId(), getSubMenuId());
		}
	}

}
