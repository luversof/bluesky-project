package net.luversof.web.gate.stock.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 화면의 날짜 선택기 하한(= 그 화면이 덮는 데이터의 최초일).
 *
 * <p>'가장 이른 기간으로'(«) 는 이 값을 알아야 목표 창을 정한다. 없으면 눌러도 아무 일이 일어나지 않아 고장으로 읽힌다 - 실측 2026-09-13: 상세 두 화면이
 * 이 값을 안 넘겨 « 가 죽은 버튼이었다.
 *
 * <p>매매와 배당 중 <b>이른 쪽</b>을 쓴다. 배당만 있고 매매가 없는 종목이 있어(실측 2026-09-13: 사용자 43 종목 중 1 종목) 매매만 보면 하한이 비어
 * 버린다.
 */
public final class StockFirstDateUtil {

  private StockFirstDateUtil() {}

  /**
   * 둘 중 이른 쪽을 그 화면의 시간대로 바꾼 날짜. 둘 다 없으면 빈 문자열(선택기는 하한 없음으로 읽는다).
   *
   * @param tradeFirst 최초 매매 시각, 없으면 null
   * @param dividendFirst 최초 배당 시각, 없으면 null
   * @param zone 화면이 쓰는 시간대
   */
  public static String earliestLocalDate(Instant tradeFirst, Instant dividendFirst, ZoneId zone) {
    Instant first = tradeFirst;
    if (dividendFirst != null && (first == null || dividendFirst.isBefore(first))) {
      first = dividendFirst;
    }
    if (first == null || zone == null) {
      return "";
    }
    return LocalDate.ofInstant(first, zone).toString();
  }

  /**
   * 화면이 덮은 구간의 시작. 시계열의 첫 점과 데이터의 최초일 중 <b>이른 쪽</b>이다.
   *
   * <p>'전체' 를 고르면 날짜가 안 실려 배지가 빌 수밖에 없어, 대신 이 값을 적는다. 그런데 시계열은 평가액이 잡히는 날부터라 첫 거래보다 늦게 시작할 수 있다 -
   * 실측 2026-09-13: 삼성전자 상세 배지가 2020-03-23 인데 표에는 2020-03-04 매매가 있었다 (계좌도 한국투자증권 위탁 13 일 · 연금저축1 2 일
   * 늦었다). 배지가 화면 내용보다 좁으면 거짓말이 된다.
   *
   * @param seriesStart 시계열에서 뽑은 시작일, 없으면 null
   * @param dataFirstDate 이 종목/계좌의 최초일({@code yyyy-MM-dd}), 없으면 빈 문자열
   */
  public static LocalDate coveredStart(LocalDate seriesStart, String dataFirstDate) {
    if (dataFirstDate == null || dataFirstDate.isBlank()) {
      return seriesStart;
    }
    LocalDate parsed;
    try {
      parsed = LocalDate.parse(dataFirstDate);
    } catch (java.time.format.DateTimeParseException ex) {
      return seriesStart;
    }
    if (seriesStart == null) {
      return parsed;
    }
    return parsed.isBefore(seriesStart) ? parsed : seriesStart;
  }

  /**
   * 화면이 덮은 구간의 끝. 시계열의 마지막 점과 화면이 그린 자료의 마지막 날 중 <b>늦은 쪽</b>이다.
   *
   * <p>시계열이 아예 없는 종목(배당만 있고 매매가 없는 경우)은 끝을 못 정해 배지가 <b>물음표</b>로 나갔다 - 실측 2026-09-13: 하나금융지주 상세가 "전체
   * · 2020-04-08 ~ ?" 였다. 화면에 그려진 자료로 메운다.
   */
  public static LocalDate coveredEnd(LocalDate seriesEnd, LocalDate contentLast) {
    if (contentLast == null) {
      return seriesEnd;
    }
    if (seriesEnd == null) {
      return contentLast;
    }
    return contentLast.isAfter(seriesEnd) ? contentLast : seriesEnd;
  }

  /** 두 날짜 중 늦은 쪽(둘 다 없으면 null). 매매·배당처럼 자료가 여러 갈래일 때 합칠 용도. */
  public static LocalDate later(LocalDate one, LocalDate other) {
    return coveredEnd(one, other);
  }
}
