package net.luversof.web.dynamiccrud.setting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import net.luversof.web.dynamiccrud.setting.service.admin.AdminConstant;

@Entity
@Table(
        name = "SubMenu",
        uniqueConstraints =
                @UniqueConstraint(
                        columnNames = {"adminProjectId", "projectId", "mainMenuId", "subMenuId"}))
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
    private SubMenuDbType dbType; // MsSql, MySql

    @Column(nullable = false)
    private Short displayOrder;

    @Column private Short pageSize;

    @Column(nullable = false)
    private boolean enableExcel;

    @Column(nullable = false)
    private boolean enableInsert;

    @Column(nullable = false)
    private boolean enableUpdate;

    @Column(nullable = false)
    private boolean enableDelete;

    @Column(nullable = false)
    private boolean enableDisplay;

    @Transient private boolean enableImport;

    @Transient private boolean enableExport;

    @Column(length = 20)
    private String authority;

    public SubMenu() {}

    public SubMenu(
            String adminProjectId,
            String projectId,
            String mainMenuId,
            String subMenuId,
            String subMenuName,
            SubMenuDbType dbType,
            Short displayOrder,
            Short pageSize,
            boolean enableExcel,
            boolean enableInsert,
            boolean enableUpdate,
            boolean enableDelete,
            boolean enableDisplay,
            boolean enableImport,
            boolean enableExport,
            String authority) {
        this.adminProjectId = adminProjectId;
        this.projectId = projectId;
        this.mainMenuId = mainMenuId;
        this.subMenuId = subMenuId;
        this.subMenuName = subMenuName;
        this.dbType = dbType;
        this.displayOrder = displayOrder;
        this.pageSize = pageSize;
        this.enableExcel = enableExcel;
        this.enableInsert = enableInsert;
        this.enableUpdate = enableUpdate;
        this.enableDelete = enableDelete;
        this.enableDisplay = enableDisplay;
        this.enableImport = enableImport;
        this.enableExport = enableExport;
        this.authority = authority;
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

    public String getSubMenuId() {
        return subMenuId;
    }

    public void setSubMenuId(String subMenuId) {
        this.subMenuId = subMenuId;
    }

    public String getSubMenuName() {
        return subMenuName;
    }

    public void setSubMenuName(String subMenuName) {
        this.subMenuName = subMenuName;
    }

    public SubMenuDbType getDbType() {
        return dbType;
    }

    public void setDbType(SubMenuDbType dbType) {
        this.dbType = dbType;
    }

    public Short getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Short displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Short getPageSize() {
        return pageSize;
    }

    public void setPageSize(Short pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isEnableExcel() {
        return enableExcel;
    }

    public void setEnableExcel(boolean enableExcel) {
        this.enableExcel = enableExcel;
    }

    public boolean isEnableInsert() {
        return enableInsert;
    }

    public void setEnableInsert(boolean enableInsert) {
        this.enableInsert = enableInsert;
    }

    public boolean isEnableUpdate() {
        return enableUpdate;
    }

    public void setEnableUpdate(boolean enableUpdate) {
        this.enableUpdate = enableUpdate;
    }

    public boolean isEnableDelete() {
        return enableDelete;
    }

    public void setEnableDelete(boolean enableDelete) {
        this.enableDelete = enableDelete;
    }

    public boolean isEnableDisplay() {
        return enableDisplay;
    }

    public void setEnableDisplay(boolean enableDisplay) {
        this.enableDisplay = enableDisplay;
    }

    public boolean isEnableImport() {
        return enableImport;
    }

    public void setEnableImport(boolean enableImport) {
        this.enableImport = enableImport;
    }

    public boolean isEnableExport() {
        return enableExport;
    }

    public void setEnableExport(boolean enableExport) {
        this.enableExport = enableExport;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        SubMenu other = (SubMenu) obj;
        return Objects.equals(adminProjectId, other.adminProjectId)
                && Objects.equals(authority, other.authority)
                && dbType == other.dbType
                && Objects.equals(displayOrder, other.displayOrder)
                && enableDelete == other.enableDelete
                && enableDisplay == other.enableDisplay
                && enableExcel == other.enableExcel
                && enableExport == other.enableExport
                && enableImport == other.enableImport
                && enableInsert == other.enableInsert
                && enableUpdate == other.enableUpdate
                && Objects.equals(mainMenuId, other.mainMenuId)
                && Objects.equals(pageSize, other.pageSize)
                && Objects.equals(projectId, other.projectId)
                && Objects.equals(subMenuId, other.subMenuId)
                && Objects.equals(subMenuName, other.subMenuName);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result =
                prime * result
                        + Objects.hash(
                                adminProjectId,
                                authority,
                                dbType,
                                displayOrder,
                                enableDelete,
                                enableDisplay,
                                enableExcel,
                                enableExport,
                                enableImport,
                                enableInsert,
                                enableUpdate,
                                mainMenuId,
                                pageSize,
                                projectId,
                                subMenuId,
                                subMenuName);
        return result;
    }

    @Override
    public String toString() {
        return "SubMenu [adminProjectId="
                + adminProjectId
                + ", projectId="
                + projectId
                + ", mainMenuId="
                + mainMenuId
                + ", subMenuId="
                + subMenuId
                + ", subMenuName="
                + subMenuName
                + ", dbType="
                + dbType
                + ", displayOrder="
                + displayOrder
                + ", pageSize="
                + pageSize
                + ", enableExcel="
                + enableExcel
                + ", enableInsert="
                + enableInsert
                + ", enableUpdate="
                + enableUpdate
                + ", enableDelete="
                + enableDelete
                + ", enableDisplay="
                + enableDisplay
                + ", enableImport="
                + enableImport
                + ", enableExport="
                + enableExport
                + ", authority="
                + authority
                + "]";
    }

    public String getUrl() {
        if (AdminConstant.ADMIN_PROJECT_ID_VALUE.equals(getAdminProjectId())) {
            return String.format(
                    "/%s/setting/%s/%s", getProjectId(), getMainMenuId(), getSubMenuId());
        } else {
            return String.format(
                    "/%s/use/%s/%s/%s",
                    getAdminProjectId(), getProjectId(), getMainMenuId(), getSubMenuId());
        }
    }

    public boolean isShowImportButton() {
        return (enableImport && enableInsert);
    }

    public boolean isShowExportButton() {
        return dbType != SubMenuDbType.Mongo && (enableExport && enableInsert);
    }

    /**
     * Row CheckBox 표시 여부
     *
     * @return
     */
    public boolean isShowRowCheckBox() {
        return dbType != SubMenuDbType.Mongo && (enableInsert && enableExport) || enableDelete;
    }
}
