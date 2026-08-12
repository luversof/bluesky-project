package net.luversof.api.poe.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

/**
 * 데이터 서비스들이 <b>마지막으로 파일을 읽어 들인 시각</b>.
 *
 * <p>각 데이터 서비스는 생성자에서 파일을 읽고 메모리에 캐시한다. 원클릭 갱신(PoeExtractService)으로 파이프라인을 돌리면 체인이 reload() 를 불러
 * 반영되지만, <b>앱 밖에서</b> 파이프라인을 돌리면(터미널에서 run-all.mjs) 실행 중인 API 는 재기동 전까지 옛 데이터를 그대로 쓴다. 화면에는 파일 갱신
 * 시각만 보여 "갱신했는데 왜 그대로냐" 또는 반대로 "재기동했더니 결과가 달라졌다"가 원인 불명으로 남는다.
 *
 * <p>실제로 2026-08-11 에 그 사고가 났다: 09:30 에 파이프라인이 3.29.2.1 로 전 산출물을 새로 만들었는데 API 는 그대로 돌고 있었고, 무관한
 * 작업으로 재기동한 뒤에야 최적화 기준선이 -0.97% 이동해 원인 추적에 시간을 썼다. 그래서 <b>로드 시각</b>을 남겨 화면이 파일 시각과 비교할 수 있게 한다.
 */
@Service
public class PoeDataLoadStamp {

  private volatile Instant loadedAt = Instant.now();

  /** 기동 또는 원클릭 갱신 체인이 데이터를 다시 읽은 시각. */
  public Instant loadedAt() {
    return loadedAt;
  }

  /** 원클릭 갱신 체인이 reload() 를 모두 마친 뒤 호출한다. */
  public void markReloaded() {
    this.loadedAt = Instant.now();
  }
}
