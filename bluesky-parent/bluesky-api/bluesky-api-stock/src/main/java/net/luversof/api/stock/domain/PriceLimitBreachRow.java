package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 하루 만에 가격제한폭을 넘어 움직인 시세 행.
 *
 * <p>한국 시장의 일일 가격제한폭은 &plusmn;30% 다. 그래서 직전 거래일 종가 대비 30% 를 넘는 변동은 거래로는 생길 수 없고, 액면분할·병합·감자 같은
 * 기업행위이거나 수집 오류다. 어느 쪽이든 <b>가격 이력이 소급 조정되지 않았다</b>는 뜻이라, 그 날 이전 구간의 평가액이 그 배율만큼 통째로 어긋난다.
 *
 * <p>기존 점검({@link ZeroVolumeChangedClose})은 <b>거래량이 0 인 행</b>만 본다. 분할은 보통 거래가 재개되면서 거래량이 붙으므로 그
 * 그물에는 걸리지 않는다 &mdash; 지금까지 걸린 유일한 행(쌍방울 2025-05-08, 정확히 1/5)은 거래량이 우연히 0 이었을 뿐이다.
 *
 * <p>직전 거래일도 함께 담는다. 상장폐지·거래정지로 오래 쉰 뒤의 첫 거래는 제한폭 판정 대상이 아니므로, 며칠 만의 변동인지 보여야 판단할 수 있다.
 */
public record PriceLimitBreachRow(
    UUID stockItemId,
    LocalDate tradeDate,
    BigDecimal closePrice,
    BigDecimal previousClosePrice,
    LocalDate previousTradeDate) {}
