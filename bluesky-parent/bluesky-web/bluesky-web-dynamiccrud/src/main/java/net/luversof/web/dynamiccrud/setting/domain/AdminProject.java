package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "AdminProject")
public class AdminProject extends Setting {

	@Column(length = 20, unique = true)
	private String adminProjectId;
	
	@Column(length = 40, nullable = false)
	private String adminProjectName;
	
	private String defaultGrantAuthority;
	
	private String roleHierarchy;

}
