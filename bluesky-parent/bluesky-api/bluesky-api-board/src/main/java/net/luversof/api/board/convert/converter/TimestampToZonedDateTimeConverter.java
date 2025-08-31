package net.luversof.api.board.convert.converter;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class TimestampToZonedDateTimeConverter implements Converter<Timestamp, ZonedDateTime> {
	
	@Override
	public ZonedDateTime convert(Timestamp source) {
		return ZonedDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
		// 또는 원하는 ZoneId로 고정 가능
	}

}