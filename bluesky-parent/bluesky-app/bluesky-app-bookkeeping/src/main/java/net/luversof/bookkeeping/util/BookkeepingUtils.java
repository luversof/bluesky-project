package net.luversof.bookkeeping.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookkeepingUtils {

	public static LocalDateTime getStartDateTime(LocalDate targetLocalDate, int baseDate) {
		LocalDate startLocalDate = targetLocalDate.withDayOfMonth(baseDate);
		if (targetLocalDate.getDayOfMonth() < baseDate) {
			startLocalDate = startLocalDate.minusMonths(1);
		}
		return startLocalDate.atStartOfDay();
	}
	
	public static LocalDateTime getEndDateTime(LocalDate targetLocalDate, int baseDate) {
		LocalDate endLocalDate = targetLocalDate.withDayOfMonth(baseDate).minusDays(1);
		if (targetLocalDate.getDayOfMonth() >= baseDate) {
			endLocalDate = endLocalDate.plusMonths(1);
		}
		return endLocalDate.atStartOfDay();
	}
	
	/**
	 * 현재 날짜를 기준으로 월 시작일 계산
	 * 시작일이 현재 일자보다 큰 경우 이전 달로 계산
	 * @param baseDate
	 * @return
	 */
	public static LocalDateTime getNowStartDateTime(int baseDate) {
		return getStartDateTime(LocalDate.now(), baseDate);
	}
	
	/**
	 * 현재 날짜를 기준으로 월 종료일 계산
	 * 시작일이 현재 일자보다 작은 경우 다음 달로 계산
	 * @param baseDate
	 * @return
	 */
	public static LocalDateTime getNowEndDateTime(int baseDate) {
		return getEndDateTime(LocalDate.now(), baseDate);
	}
}
