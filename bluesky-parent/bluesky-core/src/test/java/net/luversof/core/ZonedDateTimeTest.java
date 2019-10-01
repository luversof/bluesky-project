package net.luversof.core;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class ZonedDateTimeTest {

	@Test
	public void test() {
		ZonedDateTime zonedDateTime = ZonedDateTime.parse("2016-11-03T09:36:39.983Z[UTC]");
		ZonedDateTime zonedDateTime2 = ZonedDateTime.parse("2016-11-03T09:36:39.983Z[UTC]").withZoneSameInstant(ZoneId.of("Asia/Seoul"));
		ZonedDateTime zonedDateTime3 = ZonedDateTime.parse("2016-11-03T09:36:39.983Z[UTC]").withZoneSameInstant(ZoneOffset.of("+09:00"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YY. MM. dd. HH:mm");
		DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("YY. MM. dd. HH:mm", Locale.KOREA);
		DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("YY. MM. dd. HH:mm").withLocale(Locale.KOREA);
		log.debug("zonedDateTime : {}", zonedDateTime);
		log.debug("zonedDateTime : {}", zonedDateTime2);
		log.debug("zonedDateTime : {}", zonedDateTime3);
		
		
		log.debug("zonedDateTime : {}", formatter.format(zonedDateTime));
		log.debug("zonedDateTime2 : {}", formatter.format(zonedDateTime2));
		log.debug("zonedDateTime3 : {}", formatter.format(zonedDateTime3));
		
		
		log.debug("zonedDateTime : {}", formatter.format(zonedDateTime));
		log.debug("zonedDateTime2 : {}", formatter2.format(zonedDateTime));
		log.debug("zonedDateTime3 : {}", formatter3.format(zonedDateTime));
		
		Locale a = Locale.KOREA;
		
		log.debug("locale : {}", a);
	}

	@Test
	public void test2() {
		DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM d yyyy  hh:mm a");
		LocalDateTime leaving = LocalDateTime.of(2013, Month.JULY, 20, 19, 30);

		// Leaving from San Francisco on July 20, 2013, at 7:30 p.m.

		ZoneId leavingZone = ZoneId.of("America/Los_Angeles");
		ZonedDateTime departure = ZonedDateTime.of(leaving, leavingZone);

		String out1 = departure.format(format);
		System.out.printf("LEAVING:  %s (%s)%n", out1, leavingZone);

		// Flight is 10 hours and 50 minutes, or 650 minutes
		ZoneId arrivingZone = ZoneId.of("Asia/Tokyo");
		ZonedDateTime arrival = departure.withZoneSameInstant(arrivingZone).plusMinutes(650);

		String out2 = arrival.format(format);
		System.out.printf("ARRIVING: %s (%s)%n", out2, arrivingZone);

	}
}
