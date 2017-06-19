package com.squareup.leakcanary;

/**
 * A {@link WatchExecutor} is in charge of executing a {@link Retryable} in the future, and retry
 * later if needed.
 * 自定义Watch执行类，用于异步执行retryable；类似于线程池管理
 */
public interface WatchExecutor {
  WatchExecutor NONE = new WatchExecutor() {
    @Override public void execute(Retryable retryable) {
    }
  };

  void execute(Retryable retryable);
}
