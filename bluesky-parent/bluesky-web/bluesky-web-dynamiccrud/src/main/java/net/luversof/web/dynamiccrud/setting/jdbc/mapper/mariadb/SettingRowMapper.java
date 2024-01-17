package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.jdbc.core.RowMapper;

import net.luversof.web.dynamiccrud.setting.domain.Setting;

public abstract class SettingRowMapper<T extends Setting> implements RowMapper<T> {
	
	
	protected void setCommon(T t, ResultSet rs) throws SQLException {
		t.setWriter(rs.getString("writer"));
		t.setCreateDate(getZonedDateTime(rs.getTimestamp("createDate")));
		t.setUpdateDate(getZonedDateTime(rs.getTimestamp("updateDate")));
	}

	protected ZonedDateTime getZonedDateTime(Timestamp timestamp) {
		return timestamp.toLocalDateTime().atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.systemDefault());
	}

}
