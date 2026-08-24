package net.luversof.api.stock.web.dto.response;

import java.util.List;

/**
 * 원장 점검 결과.
 *
 * <p>왜 필요한가: 이 앱의 원장은 증권사 화면을 사람이 옮겨 담은 것이라 산술적으로 불가능한 값이 실제로 들어와 있다(실측: 배당 193 건 중 8 건이 세금 > 과세표준
 * - KODEX 한국부동산리츠인프라, 과세표준이 77 주 기준인데 기록 수량은 10,256 주). 그런데 화면은 그 값을 그대로 더해 보여줄 뿐이라, 사용자가 스스로 눈치채지
 * 못하면 잘못된 값이 계속 합계에 섞인다.
 *
 * <p>발견 건수가 0 이어도 검사한 건수를 함께 돌려준다. "이상 0 건"과 "검사 자체가 안 돌았다"를 구분하지 못하면 안심할 근거가 못 된다.
 */
/**
 * @param distinctRowCount 규칙 중복을 걷어낸 <b>서로 다른 행</b>의 수.
 *     <p>한 행이 여러 규칙에 걸린다. 실측 2026-08-23: 발견 45 건이 실제로는 29 개 행이었고, KODEX 한국부동산리츠인프라 배당 8 건이 각각 2~4 개
 *     규칙에 걸려 16 건이 중복이었다. 건수만 보면 할 일이 실제보다 커 보인다 &mdash; 그 8 건을 고치면 45 건 중 24 건이 한꺼번에 사라진다.
 */
public record LedgerIntegrityResponse(
    long dividendCount,
    long tradeCount,
    long distinctRowCount,
    List<AccountFindingSummary> accountSummary,
    List<RowFindingSummary> multiReasonRows,
    List<LedgerIntegrityFinding> findings) {

  /**
   * 한 행에 걸린 사유를 모아 둔 것. 규칙별 목록만 있으면 같은 행을 규칙 그룹마다 다시 만나게 된다.
   *
   * <p>실측 2026-08-23: 발견 48 건이 30 행이고 그중 10 행이 사유를 2~4 개씩 달고 있었다. KODEX 한국부동산리츠인프라 배당 8 행이 전부 여기
   * 속하는데, 사실 원인은 하나다 &mdash; 과세표준을 옛 수량 77 주로 잡은 것. 행으로 묶어 보여 주면 "고칠 곳은 8 군데" 라는 것이 바로 보인다.
   */
  public record RowFindingSummary(
      String date, String stockItemName, String accountName, List<String> codes) {}

  /**
   * 계좌별 발견 집계. 어느 계좌를 고치면 몇 건이 사라지는지 보여 준다.
   *
   * <p>실측 2026-08-23: 발견 45 건이 계좌 3 개로 깨끗하게 갈렸다 &mdash; KB증권 위탁 24 건(전부 과세표준 관련이고 실은 같은 배당 8 행),
   * 동양증권 12 건(2010~2020 매도의 수수료·거래세 누락), 한국투자증권 위탁 7 건(기준일·매매없음·수수료 이상치). 계좌를 모르면 45 건이 뒤섞여 보여 어디부터
   * 손대야 할지 알 수 없다.
   *
   * @param findingCount 규칙 중복을 포함한 발견 수
   * @param distinctRowCount 그중 서로 다른 행 수
   */
  public record AccountFindingSummary(
      String accountName, long findingCount, long distinctRowCount) {}
}
