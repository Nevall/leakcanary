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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
/*自定义IntentService 监听内存泄露分析结果的Services基类*/
public abstract class AbstractAnalysisResultService extends IntentService {

  private static final String HEAP_DUMP_EXTRA = "heap_dump_extra";
  private static final String RESULT_EXTRA = "result_extra";

  /*将内存泄漏结果通知监听器*/
  // TODO: 2017/2/11 将内存泄漏结果通知监听器 (15) 
  public static void sendResultToListener(Context context, String listenerServiceClassName,
      HeapDump heapDump, AnalysisResult result) {
    Class<?> listenerServiceClass;
    try {
      listenerServiceClass = Class.forName(listenerServiceClassName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    /*启动内存泄漏分析结果监听Service {@link DisplayLeakService}*/
    Intent intent = new Intent(context, listenerServiceClass);
    intent.putExtra(HEAP_DUMP_EXTRA, heapDump);
    intent.putExtra(RESULT_EXTRA, result);
    context.startService(intent);
  }

  public AbstractAnalysisResultService() {
    super(AbstractAnalysisResultService.class.getName());
  }

  // TODO: 2017/2/11 启动Service,子线程处理内存泄漏分析结果 （15-2）
  @Override protected final void onHandleIntent(Intent intent) {
    HeapDump heapDump = (HeapDump) intent.getSerializableExtra(HEAP_DUMP_EXTRA);
    AnalysisResult result = (AnalysisResult) intent.getSerializableExtra(RESULT_EXTRA);
    try {
      onHeapAnalyzed(heapDump, result);/*保存HeapDump文件及内存泄漏分析结果，并根据情况弹出Notification*/
    } finally {
      //noinspection ResultOfMethodCallIgnored
      heapDump.heapDumpFile.delete();/*HeapDump文件及内存泄漏分析结果*/
    }
  }

  /**
   * Called after a heap dump is analyzed, whether or not a leak was found.
   * Check {@link AnalysisResult#leakFound} and {@link AnalysisResult#excludedLeak} to see if there
   * was a leak and if it can be ignored.
   *
   * This will be called from a background intent service thread.
   * <p>
   * It's OK to block here and wait for the heap dump to be uploaded.
   * <p>
   * The heap dump file will be deleted immediately after this callback returns.
   */
  protected abstract void onHeapAnalyzed(HeapDump heapDump, AnalysisResult result);
}
