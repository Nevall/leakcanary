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
package com.squareup.leakcanary.internal;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import com.squareup.leakcanary.AbstractAnalysisResultService;
import com.squareup.leakcanary.AnalysisResult;
import com.squareup.leakcanary.CanaryLog;
import com.squareup.leakcanary.HeapAnalyzer;
import com.squareup.leakcanary.HeapDump;

/**
 * This service runs in a separate process to avoid slowing down the app process or making it run
 * out of memory.
 */
public final class HeapAnalyzerService extends IntentService {

  private static final String LISTENER_CLASS_EXTRA = "listener_class_extra";
  private static final String HEAPDUMP_EXTRA = "heapdump_extra";

  /*内存泄露，启动分析内存泄露的Service */
  // TODO: 2017/2/10 内存泄露，启动分析内存泄露的Service (14-1)
  public static void runAnalysis(Context context, HeapDump heapDump,
      Class<? extends AbstractAnalysisResultService> listenerServiceClass) {
    Intent intent = new Intent(context, HeapAnalyzerService.class);
    intent.putExtra(LISTENER_CLASS_EXTRA, listenerServiceClass.getName());
    intent.putExtra(HEAPDUMP_EXTRA, heapDump);
    context.startService(intent);
  }

  public HeapAnalyzerService() {
    super(HeapAnalyzerService.class.getSimpleName());
  }

  // TODO: 2017/2/10 20:52 分析内存泄露 （14-2）
  @Override protected void onHandleIntent(Intent intent) {
    if (intent == null) {
      CanaryLog.d("HeapAnalyzerService received a null intent, ignoring.");
      return;
    }
    String listenerClassName = intent.getStringExtra(LISTENER_CLASS_EXTRA);/*内存泄漏分析结果回调监听*/
    HeapDump heapDump = (HeapDump) intent.getSerializableExtra(HEAPDUMP_EXTRA);/*内存泄漏的HeapDump文件*/

    HeapAnalyzer heapAnalyzer = new HeapAnalyzer(heapDump.excludedRefs);/*创建内存分析类*/

    AnalysisResult result = heapAnalyzer.checkForLeak(heapDump.heapDumpFile, heapDump.referenceKey);/*内存泄露结果*/
    /*回调监听器，返回内存泄漏分析结果*/
    AbstractAnalysisResultService.sendResultToListener(this, listenerClassName, heapDump, result);
  }
}
