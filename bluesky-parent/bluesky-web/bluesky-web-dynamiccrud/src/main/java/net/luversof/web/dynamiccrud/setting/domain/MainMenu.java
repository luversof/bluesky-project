package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(
        name = "MainMenu",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"adminProjectId", "projectId", "mainMenuId"}))
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

    public MainMenu() {}

    public MainMenu(
            String adminProjectId,
            String projectId,
            String mainMenuId,
            String mainMenuName,
            boolean enableDisplay) {
        this.adminProjectId = adminProjectId;
        this.projectId = projectId;
        this.mainMenuId = mainMenuId;
        this.mainMenuName = mainMenuName;
        this.enableDisplay = enableDisplay;
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

    public String getMainMenuId() {
        return mainMenuId;
    }

    public void setMainMenuId(String mainMenuId) {
        this.mainMenuId = mainMenuId;
    }

    public String getMainMenuName() {
        return mainMenuName;
    }

    public void setMainMenuName(String mainMenuName) {
        this.mainMenuName = mainMenuName;
    }

    public boolean isEnableDisplay() {
        return enableDisplay;
    }

    public void setEnableDisplay(boolean enableDisplay) {
        this.enableDisplay = enableDisplay;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        MainMenu other = (MainMenu) obj;
        return Objects.equals(adminProjectId, other.adminProjectId)
                && enableDisplay == other.enableDisplay
                && Objects.equals(mainMenuId, other.mainMenuId)
                && Objects.equals(mainMenuName, other.mainMenuName)
                && Objects.equals(projectId, other.projectId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result =
                prime * result
                        + Objects.hash(
                                adminProjectId, enableDisplay, mainMenuId, mainMenuName, projectId);
        return result;
    }

    @Override
    public String toString() {
        return "MainMenu [adminProjectId="
                + adminProjectId
                + ", projectId="
                + projectId
                + ", mainMenuId="
                + mainMenuId
                + ", mainMenuName="
                + mainMenuName
                + ", enableDisplay="
                + enableDisplay
                + "]";
    }
}
