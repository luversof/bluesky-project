package net.luversof.bookkeeping;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.datetime.standard.DateTimeContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.DefaultMessageCodesResolver;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.util.BookkeepingUtils;
import net.luversof.core.exception.BlueskyException;

@Slf4j
public class SimpleTest {

	@Test
	public void test() {
		log.debug("TEST : {}", BookkeepingErrorCode.NOT_EXIST_ASSET.getClass().getSimpleName());
		log.debug("TEST : {}", String.join(".",  "as", "caa", "hd"));
	}
	
	@Test
	@SneakyThrows
	public void errorCodeTest() {
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
	public void 공백테스트() {
		String a = "공 백";
		log.debug("result : {}", StringUtils.containsWhitespace(a));
		log.debug("result : {}", a.contains(" "));
	}
	
	
	@Test
	public void test2() {
		log.debug("result : {}", BookkeepingUtils.getMonthStartLocalDate(LocalDate.now(), 1));
		log.debug("result : {}", BookkeepingUtils.getMonthEndLocalDate(LocalDate.now(), 1));
	}
	
	@Test
	public void localeTest() {
		TimeZone timeZone = LocaleContextHolder.getTimeZone();
		log.debug("result : {}", timeZone.getID());
		log.debug("result : {}", ZoneId.getAvailableZoneIds());
		log.debug("result : {}", ZoneId.of(LocaleContextHolder.getTimeZone().getID()));
	}
	
	@Test
	public void zoneIdTest() {
		ZoneId timeZone = DateTimeContextHolder.getDateTimeContext().getTimeZone();
		log.debug("zoneId : {}", timeZone);
	}
	
	@Test
	public void UUIDTest() {
		log.debug("result : {}", UUID.fromString("1"));
	}
}
