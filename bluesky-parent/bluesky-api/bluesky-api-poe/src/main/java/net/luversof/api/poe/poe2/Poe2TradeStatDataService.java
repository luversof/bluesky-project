package net.luversof.api.poe.poe2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.luversof.api.poe.service.PoeTradeStatDataService;

/**
 * PoE2 거래소 스탯 사전 — tools/poe2-extract/trade-stats2.mjs 가 만든
 * ~/.poe-gamedata/poe2/trade-stats.json(PoE1 과 같은 형식 + 지금 리그). 읽기 · 조회는 PoE1 사전 클래스를 그대로 쓴다(빈으로 두지
 * 않고 감싼다 — 같은 타입 빈이 둘이면 PoE1 주입처가 모호해진다, 10-08).
 */
@Service
public class Poe2TradeStatDataService {

  private final PoeTradeStatDataService dictionary;

  public Poe2TradeStatDataService(
      @Value("${poe2.data-dir:${user.home}/.poe-gamedata/poe2}") String dataDir) {
    this.dictionary = new PoeTradeStatDataService(dataDir);
  }

  public PoeTradeStatDataService dictionary() {
    return dictionary;
  }

  /** 거래소 주소 경로의 리그(/trade2/search/poe2/&lt;리그&gt;) — 사전에 없으면 null. */
  public String league() {
    return dictionary.league();
  }

  /** 거래소가 이 베이스 이름을 아는가 — 모르면 null(PoE1 사전과 같은 규칙). */
  public Boolean tradable(String type) {
    return dictionary.tradable(type);
  }

  public void reload() {
    dictionary.reload();
  }
}
