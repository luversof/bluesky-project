package net.luversof.web.gate.stock.domain;

/**
 * 상세 화면 위쪽의 <b>전환기</b> 한 줄.
 *
 * <p>종목 상세에서 다른 종목으로, 계좌 상세에서 다른 계좌로 바로 가기 위한 것이다. 예전에는 뒤로 가서 목록을 다시 찾아야 했다.
 *
 * @param label 화면에 보이는 이름(종목명 · 계좌명)
 * @param detail 이름 오른쪽의 짧은 보조 문구(평가액 등). 없으면 빈 문자열
 * @param href 이동할 주소
 * @param current 지금 보고 있는 대상인지
 */
public record DetailNavEntry(String label, String detail, String href, boolean current) {}
