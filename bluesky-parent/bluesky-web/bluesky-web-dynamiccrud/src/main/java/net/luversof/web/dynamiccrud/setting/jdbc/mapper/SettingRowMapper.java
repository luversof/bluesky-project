package net.luversof.web.dynamiccrud.setting.jdbc.mapper;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.jdbc.core.RowMapper;

public abstract class SettingRowMapper<T> implements RowMapper<T> {

	protected ZonedDateTime getZonedDateTime(Timestamp timestamp) {
		return timestamp.toLocalDateTime().atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.systemDefault());
	}

}
