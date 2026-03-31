package net.luversof.web.dynamiccrud.setting.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "AdminProject")
public class AdminProject extends Setting {

    @Column(length = 20, unique = true)
    private String adminProjectId;

    @Column(length = 40, nullable = false)
    private String adminProjectName;

    private String defaultGrantAuthority;

    private String roleHierarchy;

    public AdminProject() {}

    public AdminProject(
            String adminProjectId,
            String adminProjectName,
            String defaultGrantAuthority,
            String roleHierarchy) {
        this.adminProjectId = adminProjectId;
        this.adminProjectName = adminProjectName;
        this.defaultGrantAuthority = defaultGrantAuthority;
        this.roleHierarchy = roleHierarchy;
    }

    public String getAdminProjectId() {
        return adminProjectId;
    }

    public void setAdminProjectId(String adminProjectId) {
        this.adminProjectId = adminProjectId;
    }

    public String getAdminProjectName() {
        return adminProjectName;
    }

    public void setAdminProjectName(String adminProjectName) {
        this.adminProjectName = adminProjectName;
    }

    public String getDefaultGrantAuthority() {
        return defaultGrantAuthority;
    }

    public void setDefaultGrantAuthority(String defaultGrantAuthority) {
        this.defaultGrantAuthority = defaultGrantAuthority;
    }

    public String getRoleHierarchy() {
        return roleHierarchy;
    }

    public void setRoleHierarchy(String roleHierarchy) {
        this.roleHierarchy = roleHierarchy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        AdminProject other = (AdminProject) obj;
        return Objects.equals(adminProjectId, other.adminProjectId)
                && Objects.equals(adminProjectName, other.adminProjectName)
                && Objects.equals(defaultGrantAuthority, other.defaultGrantAuthority)
                && Objects.equals(roleHierarchy, other.roleHierarchy);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result =
                prime * result
                        + Objects.hash(
                                adminProjectId,
                                adminProjectName,
                                defaultGrantAuthority,
                                roleHierarchy);
        return result;
    }

    @Override
    public String toString() {
        return "AdminProject [adminProjectId="
                + adminProjectId
                + ", adminProjectName="
                + adminProjectName
                + ", defaultGrantAuthority="
                + defaultGrantAuthority
                + ", roleHierarchy="
                + roleHierarchy
                + "]";
    }
}
