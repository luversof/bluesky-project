package net.luversof.bookkeeping.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class BookkeepingUtils {

	/**
	 * targetLocalDate 날짜를 기준으로 월 시작일 계산
	 * 시작일이 targetLocalDate 일자보다 큰 경우 이전 달로 계산
	 * @param targetLocalDate
	 * @param baseDate
	 * @return
	 */
	public static LocalDate getMonthStartLocalDate(LocalDate targetLocalDate, int baseDate) {
		LocalDate startLocalDate = targetLocalDate.withDayOfMonth(baseDate);
		return getStartLocalDate(targetLocalDate, startLocalDate, baseDate, ChronoUnit.MONTHS);
	}
	
	/**
	 * targetLocalDate 날짜를 기준으로 월 종료일 계산
	 * 종료일이 targetLocalDate 일자보다 작은 경우 다음 달로 계산
	 * @param targetLocalDate
	 * @param baseDate
	 * @return
	 */
	public static LocalDate getMonthEndLocalDate(LocalDate targetLocalDate, int baseDate) {
		LocalDate endLocalDate = targetLocalDate.withDayOfMonth(baseDate).minusDays(1);
		return getEndLocalDate(targetLocalDate, endLocalDate, baseDate, ChronoUnit.MONTHS);
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
		return getStartLocalDate(targetLocalDate, startLocalDate, baseDate, ChronoUnit.YEARS);
	}
	
	/**
	 * targetLocalDate 날짜를 기준으로 년 종료일 계산
	 * 종료일이 targetLocalDate 일자보다 작은 경우 다음 년도로 계산
	 * @param targetLocalDate
	 * @param baseDate
	 * @return
	 */
	public static LocalDate getYearEndLocalDate(LocalDate targetLocalDate, int baseDate) {
		LocalDate endLocalDate = targetLocalDate.withMonth(1).withDayOfMonth(baseDate).minusDays(1);
		return getEndLocalDate(targetLocalDate, endLocalDate, baseDate, ChronoUnit.YEARS);
	}
	
	private static LocalDate getStartLocalDate(LocalDate targetLocalDate, LocalDate startLocalDate, int baseDate, ChronoUnit chronoUnit)	{
		if (targetLocalDate.getMonth() == startLocalDate.getMonth() && targetLocalDate.getDayOfMonth() < baseDate) {
			startLocalDate = startLocalDate.minus(1, chronoUnit);
		}
		return startLocalDate;
	}
	
	/**
	 * 이거 분기 처리 이상한데? 뭔가 다른 방식으로 처리를 해야하려나?
	 * @param targetLocalDate
	 * @param endLocalDate
	 * @param baseDate
	 * @param chronoUnit
	 * @return
	 */
	private static LocalDate getEndLocalDate(LocalDate targetLocalDate, LocalDate endLocalDate, int baseDate, ChronoUnit chronoUnit) {
		if (chronoUnit == ChronoUnit.MONTHS && targetLocalDate.getMonth() == endLocalDate.getMonth() && targetLocalDate.getDayOfMonth() >= baseDate) {
			endLocalDate = endLocalDate.plus(1, chronoUnit);
		}
		
		if (chronoUnit == ChronoUnit.YEARS && !(targetLocalDate.getMonth() == endLocalDate.getMonth() && targetLocalDate.getDayOfMonth() < baseDate)) {
			endLocalDate = endLocalDate.plus(1, chronoUnit);
		}
		return endLocalDate;
	}
	
}
