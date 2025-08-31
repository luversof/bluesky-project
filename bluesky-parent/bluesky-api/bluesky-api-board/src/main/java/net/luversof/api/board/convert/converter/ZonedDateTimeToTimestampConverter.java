package net.luversof.api.board.convert.converter;

import java.sql.Timestamp;
import java.time.ZonedDateTime;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class ZonedDateTimeToTimestampConverter implements Converter<ZonedDateTime, Timestamp> {
	
	@Override
	public Timestamp convert(ZonedDateTime source) {
		return Timestamp.from(source.toInstant()); // UTC 기준으로 저장
	}

}
