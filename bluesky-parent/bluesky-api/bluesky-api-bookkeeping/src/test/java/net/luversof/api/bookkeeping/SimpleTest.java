package net.luversof.api.bookkeeping;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.datetime.standard.DateTimeContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.DefaultMessageCodesResolver;

import com.github.f4b6a3.uuid.UuidCreator;
import com.github.f4b6a3.uuid.alt.GUID;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.util.BookkeepingUtils;

@Slf4j
class SimpleTest {

	@Test
	void test() {
		log.debug("TEST : {}", BookkeepingErrorCode.NOT_EXIST_ASSET.getClass().getSimpleName());
		log.debug("TEST : {}", String.join(".",  "as", "caa", "hd"));
	}
	
	@Test
	@SneakyThrows
	void errorCodeTest() {
		DefaultMessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
		
		Exception exception  = new BlueskyException("DDDDD");
		String[] errorCodes = messageCodesResolver.resolveMessageCodes(exception.getClass().getSimpleName(), String.valueOf(((BlueskyException) exception).getErrorCode()));
		log.debug("errorCodes : {}", Arrays.asList(errorCodes));
		log.debug("errorCodes : {}", Arrays.deepToString(errorCodes));

		log.debug("test : {}", Arrays.asList(exception.getClass().getDeclaredFields()).stream().anyMatch(o -> o.getName().equals("errorCode")));
		
		log.debug("test : {}", exception instanceof BlueskyException);
		log.debug("test : {}", Arrays.asList(BindException.class.getDeclaredFields()).stream().anyMatch(o-> o.getName().equals("errorCode")));
		log.debug("test : {}", exception instanceof BindException);
		
		log.debug("getField : {}", exception.getClass().getDeclaredField("errorCode"));
	}
	
	@Test
	void 공백테스트() {
		String a = "공 백";
		log.debug("result : {}", StringUtils.containsWhitespace(a));
		log.debug("result : {}", a.contains(" "));
	}
	
	
	@Test
	void test2() {
		log.debug("result : {}", BookkeepingUtils.getMonthStartLocalDate(LocalDate.now(), 1));
		log.debug("result : {}", BookkeepingUtils.getMonthEndLocalDate(LocalDate.now(), 1));
	}
	
	@Test
	void localeTest() {
		TimeZone timeZone = LocaleContextHolder.getTimeZone();
		log.debug("result : {}", timeZone.getID());
		log.debug("result : {}", ZoneId.getAvailableZoneIds());
		log.debug("result : {}", ZoneId.of(LocaleContextHolder.getTimeZone().getID()));
	}
	
	@Test
	void zoneIdTest() {
		ZoneId timeZone = DateTimeContextHolder.getDateTimeContext().getTimeZone();
		log.debug("zoneId : {}", timeZone);
	}
	
	@Test
	void UUIDTest() {
		log.debug("result : {}", UUID.fromString("1"));
		
	}
	
	@RepeatedTest(value = 10)
	void uuidCreatorTest() {
//		System.out.println("UUID Version 1: " + UuidCreator.getTimeBased());
//		System.out.println("UUID Version 6: " + UuidCreator.getTimeOrdered());
		System.out.println("UUID Version 7: " + UuidCreator.getTimeOrderedEpoch());
		System.out.println("GUID Version 7: " + GUID.v7().toUUID());
	}
}
