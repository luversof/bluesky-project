package net.luversof.api.bookkeeping;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.BitSet;
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
import net.luversof.api.bookkeeping.constant.AssetInitialData;

@Slf4j
class SimpleTest {

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
	
	@Test
	void bitSetTest() {
		var bitSetIndexList =  AssetInitialData.getNormalBitConfigList();
		
		var bitSet = new BitSet();
		bitSetIndexList.forEach(bitSet::set);
		bitSet.set(5);
		bitSet.set(15, false);
		bitSet.set(24);
		bitSet.set(61);
		bitSet.set(63);
		bitSet.set(64);
		
		log.debug("test1 : {}", bitSet.toString());
		log.debug("test2 : {}", bitSet.toByteArray());
		log.debug("test3 : {}", BitSet.valueOf(bitSet.toByteArray())); 
		log.debug("test4 : {}", bitSet.toLongArray());
		log.debug("test5 : {}", BitSet.valueOf(bitSet.toLongArray()));
		
		long bitMask = 0;
		bitSet.stream().forEach(index -> {
//			log.debug("bit : {}", index);
//			bitMask |= (1L << index);
		});
		
		log.debug("bitset size : {}", bitSet.stream().toArray());
		for (var index : bitSet.stream().toArray()) { 
			log.debug("bit index : {}", index);
			bitMask |= (1L << index);
		}
		
		log.debug("test6 : {}", Long.toBinaryString(bitMask));	// 2진수 출력
		log.debug("test6 : {}", bitMask);	// 10진수 출력
		
		log.debug("bitCount : {}", Long.bitCount(bitMask));
		
		// long 값이 64bit 크기이기 때문에 64이상의 bit index를 처리하지 못함
		int index = 0;
		for (int i = 0; i < 64; i++) {
			if ((bitMask & (1L << i)) != 0) {
				log.debug("bit index : {}, {}", i, index++);
			}
		}
		
	}
}
