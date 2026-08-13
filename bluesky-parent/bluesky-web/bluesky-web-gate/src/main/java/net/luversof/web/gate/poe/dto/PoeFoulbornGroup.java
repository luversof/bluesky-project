package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 한 유니크의 삿된(Foulborn) 옵션 묶음 — API {@code /api/poe/foulborn} 응답 매핑용.
 *
 * <p>인게임에선 "삿된 붉은 꿈" 처럼 아이템마다 붙을 수 있는 옵션이 정해져 있고, 그 아이템의 <b>어느 모드가 무엇으로 바뀌는지</b>도 정해져 있다.
 *
 * @param token GGG 내부 식별자 — 이름 해석 실패 시의 예비 표기
 * @param uniqueName 이 옵션이 붙는 유니크(영문). null 이면 이름 해석 실패 → 화면은 토큰만 보여준다(추측 금지)
 * @param uniqueSlug 상세 링크용 slug(우리 고유 데이터와 이어졌을 때만)
 */
public record PoeFoulbornGroup(
    String token,
    String category,
    String categoryKo,
    String uniqueName,
    String uniqueNameKo,
    String uniqueSlug,
    List<FoulbornMod> mods) {

  /**
   * 삿된 옵션 하나 — 최대 롤(en/ko), 최소 롤(enMin/koMin), 그리고 이 옵션이 <b>밀어내는 원본 모드</b>(origEn/origKo).
   *
   * <p>원본이 null 이면 대체 대상이 확인되지 않은 것이다(지도 밖 신규 모드). 추측해서 짝지어 주지 않는다.
   */
  public record FoulbornMod(
      String id,
      List<String> en,
      List<String> ko,
      List<String> enMin,
      List<String> koMin,
      String origId,
      List<String> origEn,
      List<String> origKo) {}
}
