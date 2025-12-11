package net.luversof.web.dynamiccrud.setting.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "Project", uniqueConstraints = @UniqueConstraint(columnNames = { "adminProjectId", "projectId" }))
public class Project extends Setting {

	@Column(length = 20)
	private String adminProjectId;

	@Column(length = 20)
	private String projectId;

	@Column(length = 40, nullable = false)
	private String projectName;

	@Column(nullable = false)
	private boolean enableMainMenuUI;

	public Project() {
	}

	public Project(String adminProjectId, String projectId, String projectName, boolean enableMainMenuUI) {
		this.adminProjectId = adminProjectId;
		this.projectId = projectId;
		this.projectName = projectName;
		this.enableMainMenuUI = enableMainMenuUI;
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

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public boolean isEnableMainMenuUI() {
		return enableMainMenuUI;
	}

	public void setEnableMainMenuUI(boolean enableMainMenuUI) {
		this.enableMainMenuUI = enableMainMenuUI;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Project other = (Project) obj;
		return Objects.equals(adminProjectId, other.adminProjectId) && enableMainMenuUI == other.enableMainMenuUI
				&& Objects.equals(projectId, other.projectId) && Objects.equals(projectName, other.projectName);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(adminProjectId, enableMainMenuUI, projectId, projectName);
		return result;
	}

	@Override
	public String toString() {
		return "Project [adminProjectId=" + adminProjectId + ", projectId=" + projectId + ", projectName=" + projectName
				+ ", enableMainMenuUI=" + enableMainMenuUI + "]";
	}

}