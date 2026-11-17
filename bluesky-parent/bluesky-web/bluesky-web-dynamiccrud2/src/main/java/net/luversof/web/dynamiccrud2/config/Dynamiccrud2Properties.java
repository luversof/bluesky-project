package net.luversof.web.dynamiccrud2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * dynamiccrud2 설정. 실행 한도는 메뉴별로 바꾸지 않는다.
 *
 * @param queryTimeoutSeconds 쿼리 타임아웃(초)
 * @param pageMaxRows 목록 한 페이지 최대 행 수
 * @param excelMaxRows 엑셀 내려받기 최대 행 수
 * @param connectionInfoConnection connectionInfo 저장소 자체를 가리키는 connection 이름(connection 목록 조회용)
 */
@ConfigurationProperties("bluesky.dynamiccrud2")
public record Dynamiccrud2Properties(
    Integer queryTimeoutSeconds,
    Integer pageMaxRows,
    Integer excelMaxRows,
    String connectionInfoConnection) {

  public Dynamiccrud2Properties {
    queryTimeoutSeconds = queryTimeoutSeconds == null ? 30 : queryTimeoutSeconds;
    pageMaxRows = pageMaxRows == null ? 1000 : pageMaxRows;
    excelMaxRows = excelMaxRows == null ? 100_000 : excelMaxRows;
    connectionInfoConnection =
        connectionInfoConnection == null ? "connectioninfo" : connectionInfoConnection;
  }
}
