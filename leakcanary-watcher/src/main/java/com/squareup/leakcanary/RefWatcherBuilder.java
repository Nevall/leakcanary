package com.squareup.leakcanary;

/**
 * Responsible for building {@link RefWatcher} instances. Subclasses should provide sane defaults
 * for the platform they support.
 */
public class RefWatcherBuilder<T extends RefWatcherBuilder> {

  private ExcludedRefs excludedRefs;
  private HeapDump.Listener heapDumpListener;
  private DebuggerControl debuggerControl;
  private HeapDumper heapDumper;
  private WatchExecutor watchExecutor;
  private GcTrigger gcTrigger;

  /** @see HeapDump.Listener */
  public final T heapDumpListener(HeapDump.Listener heapDumpListener) {
    this.heapDumpListener = heapDumpListener;
    return self();
  }

  /** @see ExcludedRefs */
  public final T excludedRefs(ExcludedRefs excludedRefs) {
    this.excludedRefs = excludedRefs;
    return self();
  }

  /** @see HeapDumper */
  public final T heapDumper(HeapDumper heapDumper) {
    this.heapDumper = heapDumper;
    return self();
  }

  /** @see DebuggerControl */
  public final T debuggerControl(DebuggerControl debuggerControl) {
    this.debuggerControl = debuggerControl;
    return self();
  }

  /** @see WatchExecutor */
  public final T watchExecutor(WatchExecutor watchExecutor) {
    this.watchExecutor = watchExecutor;
    return self();
  }

  /** @see GcTrigger */
  public final T gcTrigger(GcTrigger gcTrigger) {
    this.gcTrigger = gcTrigger;
    return self();
  }

  /** Creates a {@link RefWatcher}.
   * 创建一个Activity 的RefWatcher 
   * 初始化数据*/
  // TODO: 2017/2/10 创建一个Activity 的RefWatcher （5）
  public final RefWatcher build() {
    if (isDisabled()) {
      return RefWatcher.DISABLED;
    }

    ExcludedRefs excludedRefs = this.excludedRefs;/*内存泄露白名单:过滤ASOP引发的内存泄露*/
    if (excludedRefs == null) {
      excludedRefs = defaultExcludedRefs();
    }

    HeapDump.Listener heapDumpListener = this.heapDumpListener;/*DumpHeap监听*/
    if (heapDumpListener == null) {
      heapDumpListener = defaultHeapDumpListener();
    }

    DebuggerControl debuggerControl = this.debuggerControl;/*Debugger 控制类*/
    if (debuggerControl == null) {
      debuggerControl = defaultDebuggerControl();
    }

    HeapDumper heapDumper = this.heapDumper;/*HeapDumper*/
    if (heapDumper == null) {
      heapDumper = defaultHeapDumper();
    }

    WatchExecutor watchExecutor = this.watchExecutor;
    if (watchExecutor == null) {
      watchExecutor = defaultWatchExecutor();
    }

    GcTrigger gcTrigger = this.gcTrigger;/*GC触发器*/
    if (gcTrigger == null) {
      gcTrigger = defaultGcTrigger();
    }
    /*创建RefWatcher*/
    return new RefWatcher(watchExecutor, debuggerControl, gcTrigger, heapDumper, heapDumpListener,
        excludedRefs);
  }

  protected boolean isDisabled() {
    return false;
  }

  protected GcTrigger defaultGcTrigger() {
    return GcTrigger.DEFAULT;
  }

  protected DebuggerControl defaultDebuggerControl() {
    return DebuggerControl.NONE;
  }

  protected ExcludedRefs defaultExcludedRefs() {
    return ExcludedRefs.builder().build();
  }

  protected HeapDumper defaultHeapDumper() {
    return HeapDumper.NONE;
  }

  protected HeapDump.Listener defaultHeapDumpListener() {
    return HeapDump.Listener.NONE;
  }

  protected WatchExecutor defaultWatchExecutor() {
    return WatchExecutor.NONE;
  }

  protected final T self() {
    //noinspection unchecked
    return (T) this;
  }
}
