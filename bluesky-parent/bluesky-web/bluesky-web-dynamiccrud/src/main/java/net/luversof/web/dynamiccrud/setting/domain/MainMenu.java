package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "MainMenu", uniqueConstraints = @UniqueConstraint(columnNames = { "adminProjectId", "projectId", "mainMenuId" }))
public class MainMenu extends Setting {
	
	@Column(length = 20)
	private String adminProjectId;
	
	@Column(length = 20)
	private String projectId;
	
	@Column(length = 40)
	private String mainMenuId;
	
	@Column(length = 40, nullable = false)
	private String mainMenuName;
	
	@Column(nullable = false)
	private boolean enableDisplay;

}
