/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.leakcanary;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.MessageQueue;

import static com.squareup.leakcanary.Retryable.Result.RETRY;

/**
 * {@link WatchExecutor} suitable for watching Android reference leaks. This executor waits for the
 * main thread to be idle then posts to a serial background thread with a delay of
 * {@link R.integer#leak_canary_watch_delay_millis} seconds.
 * AndroidWatch执行类，用于异步执行程序，类似线程池管理类
 */
public final class AndroidWatchExecutor implements WatchExecutor {

  static final String LEAK_CANARY_THREAD_NAME = "LeakCanary-Heap-Dump";
  private final Handler mainHandler;
  private final Handler backgroundHandler;
  private final long initialDelayMillis;
  private final long maxBackoffFactor;

  public AndroidWatchExecutor(long initialDelayMillis) {
    mainHandler = new Handler(Looper.getMainLooper());/*主线程*/
    HandlerThread handlerThread = new HandlerThread(LEAK_CANARY_THREAD_NAME);
    handlerThread.start();
    backgroundHandler = new Handler(handlerThread.getLooper());/*后台线程*/
    this.initialDelayMillis = initialDelayMillis;/*延时间隔*/
    maxBackoffFactor = Long.MAX_VALUE / initialDelayMillis;
  }

  /*后台执行Retryable
  * 1、当主线程空闲时，在主线程中添加一个空闲后台线程*/
  // TODO: 2017/2/10 当主线程空闲时，在主线程中添加一个空闲后台线程（10），执行程序
  @Override public void execute(Retryable retryable) {
    if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
      waitForIdle(retryable, 0);/*等待线程主线程空闲*/
    } else {
      postWaitForIdle(retryable, 0);
    }
  }

  /*在主线程空闲时添加后台线程*/
  private void postWaitForIdle(final Retryable retryable, final int failedAttempts) {
    mainHandler.post(new Runnable() {
      @Override public void run() {
        waitForIdle(retryable, failedAttempts);
      }
    });
  }

  void waitForIdle(final Retryable retryable, final int failedAttempts) {
    // This needs to be called from the main thread.
    Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
      @Override public boolean queueIdle() {
        postToBackgroundWithDelay(retryable, failedAttempts);
        return false;
      }
    });
  }

  /*添加后台线程执行Retryable 的run()方法
  * 若RETRY 则延时重新执行*/
  private void postToBackgroundWithDelay(final Retryable retryable, final int failedAttempts) {
    long exponentialBackoffFactor = (long) Math.min(Math.pow(2, failedAttempts), maxBackoffFactor);
    long delayMillis = initialDelayMillis * exponentialBackoffFactor;
    backgroundHandler.postDelayed(new Runnable() {
      @Override public void run() {
        Retryable.Result result = retryable.run();
        if (result == RETRY) {
          postWaitForIdle(retryable, failedAttempts + 1);
        }
      }
    }, delayMillis);
  }
}
