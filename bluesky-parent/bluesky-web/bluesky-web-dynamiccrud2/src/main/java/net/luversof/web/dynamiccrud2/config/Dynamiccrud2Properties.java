package net.luversof.web.dynamiccrud2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * dynamiccrud2 설정. 실행 한도는 메뉴별로 바꾸지 않는다.
 *
 * @param queryTimeoutSeconds 쿼리 타임아웃(초)
 * @param pageMaxRows 목록 한 페이지 최대 행 수
 * @param excelMaxRows 엑셀 내려받기 최대 행 수
 * @param connectionInfoConnection connectionInfo 저장소 자체를 가리키는 connection 이름(connection 목록 조회용)
 * @param rowSignKey 목록 행 서명 키(RowSigner). 비우면 시작할 때마다 새로 만든다 - 재시작 뒤 열려 있던 목록은 다시 열어야 하고 replica 를
 *     2개 이상 두면 꼭 넣어야 한다
 */
@ConfigurationProperties("bluesky.dynamiccrud2")
public record Dynamiccrud2Properties(
    Integer queryTimeoutSeconds,
    Integer pageMaxRows,
    Integer excelMaxRows,
    String connectionInfoConnection,
    String rowSignKey) {

  public Dynamiccrud2Properties {
    queryTimeoutSeconds = queryTimeoutSeconds == null ? 30 : queryTimeoutSeconds;
    pageMaxRows = pageMaxRows == null ? 1000 : pageMaxRows;
    excelMaxRows = excelMaxRows == null ? 100_000 : excelMaxRows;
    connectionInfoConnection =
        connectionInfoConnection == null ? "connectioninfo" : connectionInfoConnection;
  }
}
