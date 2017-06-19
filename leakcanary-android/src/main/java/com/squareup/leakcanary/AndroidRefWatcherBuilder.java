package com.squareup.leakcanary;

import android.app.Application;
import android.content.Context;
import java.util.concurrent.TimeUnit;

import static com.squareup.leakcanary.RefWatcher.DISABLED;
import static java.util.concurrent.TimeUnit.SECONDS;

/** A {@link RefWatcherBuilder} with appropriate Android defaults. */
public final class AndroidRefWatcherBuilder extends RefWatcherBuilder<AndroidRefWatcherBuilder> {

  private static final long DEFAULT_WATCH_DELAY_MILLIS = SECONDS.toMillis(5);

  private final Context context;

  AndroidRefWatcherBuilder(Context context) {
    this.context = context.getApplicationContext();
  }

  /**
   * Sets a custom {@link AbstractAnalysisResultService} to listen to analysis results. This
   * overrides any call to {@link #heapDumpListener(HeapDump.Listener)}.
   * 添加监听
   */
  // TODO: 2017/2/10 添加监听 （2）
  public AndroidRefWatcherBuilder listenerServiceClass(
      Class<? extends AbstractAnalysisResultService> listenerServiceClass) {
    return heapDumpListener(new ServiceHeapDumpListener(context, listenerServiceClass));
  }

  /**
   * Sets a custom delay for how long the {@link RefWatcher} should wait until it checks if a
   * tracked object has been garbage collected. This overrides any call to {@link
   * #watchExecutor(WatchExecutor)}.
   * 设置延时,等待跟踪对象呗垃圾回收
   */
  public AndroidRefWatcherBuilder watchDelay(long delay, TimeUnit unit) {
    return watchExecutor(new AndroidWatchExecutor(unit.toMillis(delay)));
  }

  /**
   * Sets the maximum number of heap dumps stored. This overrides any call to {@link
   * #heapDumper(HeapDumper)} as well as any call to
   * {@link LeakCanary#setDisplayLeakActivityDirectoryProvider(LeakDirectoryProvider)})}
   *
   * @throws IllegalArgumentException if maxStoredHeapDumps < 1.
   * 设置HeapDumps文最多数量
   */
  public AndroidRefWatcherBuilder maxStoredHeapDumps(int maxStoredHeapDumps) {
    LeakDirectoryProvider leakDirectoryProvider =
        new DefaultLeakDirectoryProvider(context, maxStoredHeapDumps);
    LeakCanary.setDisplayLeakActivityDirectoryProvider(leakDirectoryProvider);
    return heapDumper(new AndroidHeapDumper(context, leakDirectoryProvider));
  }

  /**
   * Creates a {@link RefWatcher} instance and starts watching activity references (on ICS+).
   * 创建RefWatcher，监控Activity 对象
   */
  // TODO: 2017/2/10 创建RefWatcher，监控Activity对象 （4） 
  public RefWatcher buildAndInstall() {
    RefWatcher refWatcher = build();
    if (refWatcher != DISABLED) {
      LeakCanary.enableDisplayLeakActivity(context);/*设置显示Notification提示*/
      ActivityRefWatcher.installOnIcsPlus((Application) context, refWatcher);/*开始添加监听*/
    }
    return refWatcher;
  }

  /*是否禁用，在分析数据进程时返回true*/
  @Override protected boolean isDisabled() {
    return LeakCanary.isInAnalyzerProcess(context);
  }

  /*创建默认HeapDumper*/
  @Override protected HeapDumper defaultHeapDumper() {
    LeakDirectoryProvider leakDirectoryProvider = new DefaultLeakDirectoryProvider(context);
    return new AndroidHeapDumper(context, leakDirectoryProvider);
  }

  /*创建Debug Control*/
  @Override protected DebuggerControl defaultDebuggerControl() {
    return new AndroidDebuggerControl();
  }
  /*创建默认Listener*/
  @Override protected HeapDump.Listener defaultHeapDumpListener() {
    return new ServiceHeapDumpListener(context, DisplayLeakService.class);
  }
  
  @Override protected ExcludedRefs defaultExcludedRefs() {
    return AndroidExcludedRefs.createAppDefaults().build();
  }

  @Override protected WatchExecutor defaultWatchExecutor() {
    return new AndroidWatchExecutor(DEFAULT_WATCH_DELAY_MILLIS);
  }
}
