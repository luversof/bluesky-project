package net.luversof.web.gate.stock.config;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 작업에 요청의 {@link LocaleContext} 를 실어 보내는 실행기.
 *
 * <p>{@code LocaleContextHolder} 는 ThreadLocal 이라 다른 스레드에서는 비어 있고, 그러면 메시지가 <b>기본 로케일</b>로 풀린다. 실측
 * 2026-09-11: 영어 화면의 자산성장 매매 이력 제목이 "Trade History <b>전체</b>" 로 나왔다. 같은 값을 요청 스레드에서 만드는 {@code
 * /stock/htmx/trade-history} 는 "Trade History All" 로 맞았다.
 *
 * <p>{@code DelegatingSecurityContextExecutorService} 가 같은 이유로 SecurityContext 를 실어 보내는 것과 짝이다 (그쪽은
 * 빠지면 Authorization 헤더가 조용히 사라진다).
 */
public final class LocaleContextExecutorService implements ExecutorService {

  private final ExecutorService delegate;

  public LocaleContextExecutorService(ExecutorService delegate) {
    this.delegate = delegate;
  }

  private Runnable wrap(Runnable task) {
    LocaleContext context = LocaleContextHolder.getLocaleContext();
    return () -> {
      LocaleContext previous = LocaleContextHolder.getLocaleContext();
      LocaleContextHolder.setLocaleContext(context);
      try {
        task.run();
      } finally {
        LocaleContextHolder.setLocaleContext(previous);
      }
    };
  }

  private <T> Callable<T> wrap(Callable<T> task) {
    LocaleContext context = LocaleContextHolder.getLocaleContext();
    return () -> {
      LocaleContext previous = LocaleContextHolder.getLocaleContext();
      LocaleContextHolder.setLocaleContext(context);
      try {
        return task.call();
      } finally {
        LocaleContextHolder.setLocaleContext(previous);
      }
    };
  }

  private <T> Collection<Callable<T>> wrapAll(Collection<? extends Callable<T>> tasks) {
    return tasks.stream().map(this::wrap).toList();
  }

  @Override
  public void execute(Runnable command) {
    delegate.execute(wrap(command));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return delegate.submit(wrap(task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return delegate.submit(wrap(task), result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return delegate.submit(wrap(task));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    return delegate.invokeAll(wrapAll(tasks));
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return delegate.invokeAll(wrapAll(tasks), timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    return delegate.invokeAny(wrapAll(tasks));
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(wrapAll(tasks), timeout, unit);
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }
}
