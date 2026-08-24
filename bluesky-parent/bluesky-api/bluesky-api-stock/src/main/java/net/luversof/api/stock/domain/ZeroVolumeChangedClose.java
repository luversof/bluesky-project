package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 거래량이 0 인데 종가가 직전 행과 다른 시세 행.
 *
 * <p>거래가 없으면 종가가 바뀔 수 없다. 그래서 이런 행은 둘 중 하나다 &mdash; 수집이 잘못됐거나, 액면분할·병합처럼 거래 없이 가격이 조정된 날이다. 어느 쪽인지는
 * 행을 봐야 알 수 있는데, 지금까지는 <b>개수만</b> 알 수 있었다(관리 화면에 "1" 이라고만 떴다). 개수만으로는 "거래량 0 행을 평가에서 빼도 되는가" 라는 판단을
 * 할 수 없다.
 *
 * <p>직전 종가를 함께 담아 얼마나 뛰었는지 바로 보이게 한다.
 */
public record ZeroVolumeChangedClose(
    UUID stockItemId,
    LocalDate tradeDate,
    BigDecimal closePrice,
    /** 직전 거래일의 종가. 이 값과 {@code closePrice} 가 다르다는 것이 이 행이 뽑힌 이유다. */
    BigDecimal previousClosePrice) {}
