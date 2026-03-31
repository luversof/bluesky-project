package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.SubMenu;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;

public class SubMenuRowMapper extends SettingRowMapper<SubMenu> {

    @Override
    public SubMenu mapRow(ResultSet rs, int rowNum) throws SQLException {
        var subMenu =
                new SubMenu(
                        rs.getString("adminProjectId"),
                        rs.getString("projectId"),
                        rs.getString("mainMenuId"),
                        rs.getString("subMenuId"),
                        rs.getString("subMenuName"),
                        SubMenuDbType.valueOf(rs.getString("dbType")),
                        rs.getShort("displayOrder"),
                        rs.getShort("pageSize"),
                        rs.getBoolean("enableExcel"),
                        rs.getBoolean("enableInsert"),
                        rs.getBoolean("enableUpdate"),
                        rs.getBoolean("enableDelete"),
                        rs.getBoolean("enableDisplay"),
                        true,
                        true,
                        rs.getString("authority"));
        setCommon(subMenu, rs);
        return subMenu;
    }
}
