package net.luversof.api.stock.web.dto.response;

import java.util.List;

/**
 * 원장에서 산술적으로 불가능한 기록 한 종류.
 *
 * <p>{@code code} 는 규칙 식별자다. 사람이 읽을 문구는 화면이 로케일에 맞춰 붙인다 - 여기서 한글 문장을 만들어 보내면 영어 화면에도 한글이 나간다.
 *
 * <p>{@code examples} 는 사용자가 원장에서 그 줄을 찾아갈 수 있을 만큼만 담는다(일자·종목·문제가 된 수치). 전체 목록을 실어 보내면 응답이 원장 크기를
 * 따라가므로 상한을 둔다.
 */
public record LedgerIntegrityFinding(String code, int count, List<Example> examples) {

  /** 원장에서 그 줄을 찾아갈 단서. {@code detail} 은 숫자만 담고 문장은 만들지 않는다. */
  public record Example(String date, String stockItemName, String detail) {}
}
