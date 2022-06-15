package net.luversof.bookkeeping.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.context.ApplicationContext;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.Setter;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;

public class BookkeepingUtils {
	
	@Setter
	private static ApplicationContext applicationContext;
	
	public static LocalDate getStartLocalDate(LocalDate targetLocalDate, int baseDate, ChronoUnit chronoUnit) {
		if (ChronoUnit.MONTHS == chronoUnit) {
			return getMonthStartLocalDate(targetLocalDate, baseDate);
		} else if (ChronoUnit.YEARS == chronoUnit) {
			return getYearStartLocalDate(targetLocalDate, baseDate);
		} 
		throw new BlueskyException(BookkeepingErrorCode.NOT_SUPPORT_CHRONOUNIT);
	}
	
	public static LocalDate getEndLocalDate(LocalDate targetLocalDate, int baseDate, ChronoUnit chronoUnit) {
		if (ChronoUnit.MONTHS == chronoUnit) {
			return getMonthEndLocalDate(targetLocalDate, baseDate);
		} else if (ChronoUnit.YEARS == chronoUnit) {
			return getYearEndLocalDate(targetLocalDate, baseDate);
		} 
		throw new BlueskyException(BookkeepingErrorCode.NOT_SUPPORT_CHRONOUNIT);
	}

	/**
	 * targetLocalDate 날짜를 기준으로 월 시작일 계산
	 * 시작일이 targetLocalDate 일자보다 큰 경우 이전 달로 계산
	 * @param targetLocalDate
	 * @param baseDate
	 * @return
	 */
	public static LocalDate getMonthStartLocalDate(LocalDate targetLocalDate, int baseDate) {
		LocalDate startLocalDate = targetLocalDate.withDayOfMonth(baseDate);
		if (startLocalDate.isAfter(targetLocalDate)) {
			startLocalDate = startLocalDate.minus(1, ChronoUnit.MONTHS);
		}
		
		return startLocalDate;
	}
	
	/**
	 * targetLocalDate 날짜를 기준으로 월 종료일 계산
	 * 종료일이 targetLocalDate 일자보다 작은 경우 다음 달로 계산
	 * @param targetLocalDate
	 * @param baseDate
	 * @return
	 */
	public static LocalDate getMonthEndLocalDate(LocalDate targetLocalDate, int baseDate) {
		LocalDate endLocalDate = targetLocalDate.withDayOfMonth(baseDate);
		if (endLocalDate.isAfter(targetLocalDate)) {
			endLocalDate = endLocalDate.minusDays(1);
		} else {
			endLocalDate = endLocalDate.plus(1, ChronoUnit.MONTHS).minusDays(1);
		}
		return endLocalDate;
	}
	
	/**
	 * targetLocalDate 날짜를 기준으로 년 시작일 계산
	 * 시작일이 targetLocalDate 일자보다 큰 경우 이전 년도로 계산
	 * @param targetLocalDate
	 * @param baseDate
	 * @return
	 */
	public static LocalDate getYearStartLocalDate(LocalDate targetLocalDate, int baseDate) {
		LocalDate startLocalDate = targetLocalDate.withMonth(1).withDayOfMonth(baseDate);
		if (startLocalDate.isAfter(targetLocalDate)) {
			startLocalDate = startLocalDate.minus(1, ChronoUnit.YEARS);
		}
		return startLocalDate;
	}
	
	/**
	 * targetLocalDate 날짜를 기준으로 년 종료일 계산
	 * 종료일이 targetLocalDate 일자보다 작은 경우 다음 년도로 계산
	 * @param targetLocalDate
	 * @param baseDate
	 * @return
	 */
	public static LocalDate getYearEndLocalDate(LocalDate targetLocalDate, int baseDate) {
		LocalDate endLocalDate = targetLocalDate.withMonth(1).withDayOfMonth(baseDate);
		if (endLocalDate.isAfter(targetLocalDate)) {
			endLocalDate = endLocalDate.minusDays(1);
		} else {
			endLocalDate = endLocalDate.plus(1, ChronoUnit.YEARS).minusDays(1);
		}
		return endLocalDate;
	}
}
