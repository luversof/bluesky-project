package net.luversof.bookkeeping.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookkeepingUtils {

	/**
	 * 현재 날짜를 기준으로 월 시작일 계산
	 * 시작일이 현재 일자보다 큰 경우 이전 달로 계산
	 * @param baseDate
	 * @return
	 */
	public static LocalDateTime getCurrentStartDateTime(int baseDate) {
		LocalDate localDate = LocalDate.now();
		LocalDate startLocalDate = localDate.withDayOfMonth(baseDate);
		if (localDate.getDayOfMonth() < baseDate) {
			startLocalDate = startLocalDate.minusMonths(1);
		}
		return startLocalDate.atStartOfDay();
	}
	
	/**
	 * 현재 날짜를 기준으로 월 종료일 계산
	 * 시작일이 현재 일자보다 작은 경우 다음 달로 계산
	 * @param baseDate
	 * @return
	 */
	public static LocalDateTime getCurrentEndDateTime(int baseDate) {
		LocalDate localDate = LocalDate.now();
		LocalDate endLocalDate = localDate.withDayOfMonth(baseDate).minusDays(1);
		if (localDate.getDayOfMonth() >= baseDate) {
			endLocalDate = endLocalDate.plusMonths(1);
		}		
		return endLocalDate.atStartOfDay();
	}
	
	public static LocalDateTime getStartDateTime(LocalDate localDate, int baseDate) {
		return localDate.withDayOfMonth(baseDate).atStartOfDay();
	}
	
	public static LocalDateTime getEndDateTime(LocalDate localDate, int baseDate) {
		return localDate.withDayOfMonth(baseDate).minusDays(1).plusMonths(1).atStartOfDay();
	}
}
