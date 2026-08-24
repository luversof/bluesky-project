package net.luversof.web.gate.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import gg.jte.TemplateEngine;

/**
 * (측정용) jte 의 제어 구조 들여쓰기 제거. 스타터가 이 옵션을 프로퍼티로 노출하지 않아 빈 후처리로 켠다.
 *
 * <p>실측: 렌더된 HTML 의 30~61% 가 들여쓰기 공백이다(캘린더 뷰 1946KB 중 1257KB).
 */
@Configuration
public class JteTrimConfig implements BeanPostProcessor {

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof TemplateEngine templateEngine) {
      templateEngine.setTrimControlStructures(true);
    }
    return bean;
  }
}
