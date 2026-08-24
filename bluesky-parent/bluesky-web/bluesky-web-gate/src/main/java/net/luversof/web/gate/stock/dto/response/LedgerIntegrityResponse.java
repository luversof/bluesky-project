package net.luversof.web.gate.stock.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 원장 점검 결과.
 *
 * <p>이 앱의 원장은 증권사 화면을 사람이 옮겨 담은 것이라 산술적으로 불가능한 값이 실제로 들어와 있다(실측 2026-08-22: 배당 193 건 중 8 건이 세금 &gt;
 * 과세표준). 화면은 그 값을 그대로 더할 뿐이라 사용자가 스스로 눈치채지 못하면 잘못된 값이 계속 합계에 섞인다.
 *
 * <p>사람이 읽을 문구는 화면이 로케일에 맞춰 붙인다. 서버가 한글 문장을 만들어 보내면 영어 화면에도 한글이 나간다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LedgerIntegrityResponse(
    long dividendCount,
    long tradeCount,
    long distinctRowCount,
    List<AccountFindingSummary> accountSummary,
    List<RowFindingSummary> multiReasonRows,
    List<Finding> findings) {

  /**
   * 한 행에 걸린 사유를 모은 것.
   *
   * <p>실측 2026-08-23: 발견 48 건이 30 행이고 그중 10 행이 사유를 2~4 개씩 달고 있었다. 규칙별 목록만 보여 주면 같은 행을 규칙 그룹마다 다시
   * 만나게 되고, KODEX 한국부동산리츠인프라 8 행이 사실 한 원인(과세표준을 옛 수량 77 주로 잡은 것)이라는 것도 드러나지 않는다.
   */
  public record RowFindingSummary(
      String date, String stockItemName, String accountName, List<String> codes) {}

  /**
   * 계좌별 발견 집계. 어느 계좌를 고치면 몇 건이 사라지는지.
   *
   * <p>실측 2026-08-23: 발견 45 건이 계좌 3 개로 갈렸다 &mdash; KB증권 위탁 24(전부 과세표준, 실은 같은 배당 8 행), 동양증권
   * 12(2010~2020 매도의 수수료·거래세 누락), 한국투자증권 위탁 7.
   */
  public record AccountFindingSummary(
      String accountName, long findingCount, long distinctRowCount) {}

  public record Finding(String code, int count, List<Example> examples) {}

  public record Example(String date, String stockItemName, String detail) {}

  /** 사유가 둘 이상 겹친 행이 있는지. 있으면 행 단위로 묶어 보여 준다. */
  public boolean hasMultiReasonRows() {
    return multiReasonRows != null && !multiReasonRows.isEmpty();
  }

  /** 검사는 돌았는데 발견이 없는 상태인지. 응답이 없는 것(검사 자체가 못 돈 것)과 구분한다. */
  public boolean isClean() {
    return findings == null || findings.isEmpty();
  }

  public long checkedCount() {
    return dividendCount + tradeCount;
  }

  /** 규칙 중복을 걷어내기 전 발견 건수. */
  public int totalFindingCount() {
    return findings == null ? 0 : findings.stream().mapToInt(Finding::count).sum();
  }

  /**
   * 건수가 서로 다른 행 수보다 많은지. 그러면 "45건" 이 할 일을 부풀린다.
   *
   * <p>실측 2026-08-23: 45 건이 실제로는 29 개 행이었다 &mdash; KODEX 한국부동산리츠인프라 배당 8 건이 각각 2~4 개 규칙에 걸렸다.
   */
  public boolean hasOverlappingRows() {
    return distinctRowCount > 0 && totalFindingCount() > distinctRowCount;
  }
}
