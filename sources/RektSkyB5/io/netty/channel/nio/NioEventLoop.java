/*
 * Decompiled with CFR 0.152.
 */
package io.netty.channel.nio;

import io.netty.channel.ChannelException;
import io.netty.channel.EventLoopException;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.nio.AbstractNioChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.nio.NioTask;
import io.netty.channel.nio.SelectedSelectionKeySet;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NioEventLoop
extends SingleThreadEventLoop {
    private static final InternalLogger logger;
    private static final int CLEANUP_INTERVAL = 256;
    private static final boolean DISABLE_KEYSET_OPTIMIZATION;
    private static final int MIN_PREMATURE_SELECTOR_RETURNS = 3;
    private static final int SELECTOR_AUTO_REBUILD_THRESHOLD;
    Selector selector;
    private SelectedSelectionKeySet selectedKeys;
    private final SelectorProvider provider;
    private final AtomicBoolean wakenUp = new AtomicBoolean();
    private volatile int ioRatio = 50;
    private int cancelledKeys;
    private boolean needsToSelectAgain;

    NioEventLoop(NioEventLoopGroup parent, ThreadFactory threadFactory, SelectorProvider selectorProvider) {
        super(parent, threadFactory, false);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        this.provider = selectorProvider;
        this.selector = this.openSelector();
    }

    private Selector openSelector() {
        AbstractSelector selector;
        try {
            selector = this.provider.openSelector();
        }
        catch (IOException e2) {
            throw new ChannelException("failed to open a new selector", e2);
        }
        if (DISABLE_KEYSET_OPTIMIZATION) {
            return selector;
        }
        try {
            SelectedSelectionKeySet selectedKeySet = new SelectedSelectionKeySet();
            Class<?> selectorImplClass = Class.forName("sun.nio.ch.SelectorImpl", false, PlatformDependent.getSystemClassLoader());
            if (!selectorImplClass.isAssignableFrom(selector.getClass())) {
                return selector;
            }
            Field selectedKeysField = selectorImplClass.getDeclaredField("selectedKeys");
            Field publicSelectedKeysField = selectorImplClass.getDeclaredField("publicSelectedKeys");
            selectedKeysField.setAccessible(true);
            publicSelectedKeysField.setAccessible(true);
            selectedKeysField.set(selector, selectedKeySet);
            publicSelectedKeysField.set(selector, selectedKeySet);
            this.selectedKeys = selectedKeySet;
            logger.trace("Instrumented an optimized java.util.Set into: {}", (Object)selector);
        }
        catch (Throwable t2) {
            this.selectedKeys = null;
            logger.trace("Failed to instrument an optimized java.util.Set into: {}", (Object)selector, (Object)t2);
        }
        return selector;
    }

    @Override
    protected Queue<Runnable> newTaskQueue() {
        return PlatformDependent.newMpscQueue();
    }

    public void register(SelectableChannel ch, int interestOps, NioTask<?> task) {
        if (ch == null) {
            throw new NullPointerException("ch");
        }
        if (interestOps == 0) {
            throw new IllegalArgumentException("interestOps must be non-zero.");
        }
        if ((interestOps & ~ch.validOps()) != 0) {
            throw new IllegalArgumentException("invalid interestOps: " + interestOps + "(validOps: " + ch.validOps() + ')');
        }
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (this.isShutdown()) {
            throw new IllegalStateException("event loop shut down");
        }
        try {
            ch.register(this.selector, interestOps, task);
        }
        catch (Exception e2) {
            throw new EventLoopException("failed to register a channel", e2);
        }
    }

    public int getIoRatio() {
        return this.ioRatio;
    }

    public void setIoRatio(int ioRatio) {
        if (ioRatio <= 0 || ioRatio > 100) {
            throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
        }
        this.ioRatio = ioRatio;
    }

    public void rebuildSelector() {
        int nChannels;
        block14: {
            Selector newSelector;
            if (!this.inEventLoop()) {
                this.execute(new Runnable(){

                    @Override
                    public void run() {
                        NioEventLoop.this.rebuildSelector();
                    }
                });
                return;
            }
            Selector oldSelector = this.selector;
            if (oldSelector == null) {
                return;
            }
            try {
                newSelector = this.openSelector();
            }
            catch (Exception e2) {
                logger.warn("Failed to create a new Selector.", e2);
                return;
            }
            nChannels = 0;
            while (true) {
                try {
                    for (SelectionKey key : oldSelector.keys()) {
                        Object a2 = key.attachment();
                        try {
                            if (!key.isValid() || key.channel().keyFor(newSelector) != null) continue;
                            int interestOps = key.interestOps();
                            key.cancel();
                            SelectionKey newKey = key.channel().register(newSelector, interestOps, a2);
                            if (a2 instanceof AbstractNioChannel) {
                                ((AbstractNioChannel)a2).selectionKey = newKey;
                            }
                            ++nChannels;
                        }
                        catch (Exception e3) {
                            logger.warn("Failed to re-register a Channel to the new Selector.", e3);
                            if (a2 instanceof AbstractNioChannel) {
                                AbstractNioChannel ch = (AbstractNioChannel)a2;
                                ch.unsafe().close(ch.unsafe().voidPromise());
                                continue;
                            }
                            NioTask task = (NioTask)a2;
                            NioEventLoop.invokeChannelUnregistered(task, key, e3);
                        }
                    }
                }
                catch (ConcurrentModificationException e4) {
                    continue;
                }
                break;
            }
            this.selector = newSelector;
            try {
                oldSelector.close();
            }
            catch (Throwable t2) {
                if (!logger.isWarnEnabled()) break block14;
                logger.warn("Failed to close the old Selector.", t2);
            }
        }
        logger.info("Migrated " + nChannels + " channel(s) to the new Selector.");
    }

    @Override
    protected void run() {
        while (true) {
            boolean oldWakenUp = this.wakenUp.getAndSet(false);
            try {
                if (this.hasTasks()) {
                    this.selectNow();
                } else {
                    this.select(oldWakenUp);
                    if (this.wakenUp.get()) {
                        this.selector.wakeup();
                    }
                }
                this.cancelledKeys = 0;
                this.needsToSelectAgain = false;
                int ioRatio = this.ioRatio;
                if (ioRatio == 100) {
                    this.processSelectedKeys();
                    this.runAllTasks();
                } else {
                    long ioStartTime = System.nanoTime();
                    this.processSelectedKeys();
                    long ioTime = System.nanoTime() - ioStartTime;
                    this.runAllTasks(ioTime * (long)(100 - ioRatio) / (long)ioRatio);
                }
                if (!this.isShuttingDown()) continue;
                this.closeAll();
                if (!this.confirmShutdown()) continue;
            }
            catch (Throwable t2) {
                logger.warn("Unexpected exception in the selector loop.", t2);
                try {
                    Thread.sleep(1000L);
                }
                catch (InterruptedException interruptedException) {}
                continue;
            }
            break;
        }
    }

    private void processSelectedKeys() {
        if (this.selectedKeys != null) {
            this.processSelectedKeysOptimized(this.selectedKeys.flip());
        } else {
            this.processSelectedKeysPlain(this.selector.selectedKeys());
        }
    }

    @Override
    protected void cleanup() {
        try {
            this.selector.close();
        }
        catch (IOException e2) {
            logger.warn("Failed to close a selector.", e2);
        }
    }

    void cancel(SelectionKey key) {
        key.cancel();
        ++this.cancelledKeys;
        if (this.cancelledKeys >= 256) {
            this.cancelledKeys = 0;
            this.needsToSelectAgain = true;
        }
    }

    @Override
    protected Runnable pollTask() {
        Runnable task = super.pollTask();
        if (this.needsToSelectAgain) {
            this.selectAgain();
        }
        return task;
    }

    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) {
        if (selectedKeys.isEmpty()) {
            return;
        }
        Iterator<SelectionKey> i2 = selectedKeys.iterator();
        while (true) {
            SelectionKey k2 = i2.next();
            Object a2 = k2.attachment();
            i2.remove();
            if (a2 instanceof AbstractNioChannel) {
                NioEventLoop.processSelectedKey(k2, (AbstractNioChannel)a2);
            } else {
                NioTask task = (NioTask)a2;
                NioEventLoop.processSelectedKey(k2, task);
            }
            if (!i2.hasNext()) break;
            if (!this.needsToSelectAgain) continue;
            this.selectAgain();
            selectedKeys = this.selector.selectedKeys();
            if (selectedKeys.isEmpty()) break;
            i2 = selectedKeys.iterator();
        }
    }

    private void processSelectedKeysOptimized(SelectionKey[] selectedKeys) {
        SelectionKey k2;
        int i2 = 0;
        while ((k2 = selectedKeys[i2]) != null) {
            selectedKeys[i2] = null;
            Object a2 = k2.attachment();
            if (a2 instanceof AbstractNioChannel) {
                NioEventLoop.processSelectedKey(k2, (AbstractNioChannel)a2);
            } else {
                NioTask task = (NioTask)a2;
                NioEventLoop.processSelectedKey(k2, task);
            }
            if (this.needsToSelectAgain) {
                while (selectedKeys[i2] != null) {
                    selectedKeys[i2] = null;
                    ++i2;
                }
                this.selectAgain();
                selectedKeys = this.selectedKeys.flip();
                i2 = -1;
            }
            ++i2;
        }
    }

    private static void processSelectedKey(SelectionKey k2, AbstractNioChannel ch) {
        AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        if (!k2.isValid()) {
            unsafe.close(unsafe.voidPromise());
            return;
        }
        try {
            int readyOps = k2.readyOps();
            if ((readyOps & 0x11) != 0 || readyOps == 0) {
                unsafe.read();
                if (!ch.isOpen()) {
                    return;
                }
            }
            if ((readyOps & 4) != 0) {
                ch.unsafe().forceFlush();
            }
            if ((readyOps & 8) != 0) {
                int ops = k2.interestOps();
                k2.interestOps(ops &= 0xFFFFFFF7);
                unsafe.finishConnect();
            }
        }
        catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void processSelectedKey(SelectionKey k2, NioTask<SelectableChannel> task) {
        int state = 0;
        try {
            task.channelReady(k2.channel(), k2);
            state = 1;
        }
        catch (Exception e2) {
            k2.cancel();
            NioEventLoop.invokeChannelUnregistered(task, k2, e2);
            state = 2;
        }
        finally {
            switch (state) {
                case 0: {
                    k2.cancel();
                    NioEventLoop.invokeChannelUnregistered(task, k2, null);
                    break;
                }
                case 1: {
                    if (k2.isValid()) break;
                    NioEventLoop.invokeChannelUnregistered(task, k2, null);
                }
            }
        }
    }

    private void closeAll() {
        this.selectAgain();
        Set<SelectionKey> keys = this.selector.keys();
        ArrayList<AbstractNioChannel> channels = new ArrayList<AbstractNioChannel>(keys.size());
        for (SelectionKey k2 : keys) {
            Object a2 = k2.attachment();
            if (a2 instanceof AbstractNioChannel) {
                channels.add((AbstractNioChannel)a2);
                continue;
            }
            k2.cancel();
            NioTask task = (NioTask)a2;
            NioEventLoop.invokeChannelUnregistered(task, k2, null);
        }
        for (AbstractNioChannel ch : channels) {
            ch.unsafe().close(ch.unsafe().voidPromise());
        }
    }

    private static void invokeChannelUnregistered(NioTask<SelectableChannel> task, SelectionKey k2, Throwable cause) {
        try {
            task.channelUnregistered(k2.channel(), cause);
        }
        catch (Exception e2) {
            logger.warn("Unexpected exception while running NioTask.channelUnregistered()", e2);
        }
    }

    @Override
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && this.wakenUp.compareAndSet(false, true)) {
            this.selector.wakeup();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void selectNow() throws IOException {
        try {
            this.selector.selectNow();
        }
        finally {
            if (this.wakenUp.get()) {
                this.selector.wakeup();
            }
        }
    }

    private void select(boolean oldWakenUp) throws IOException {
        block10: {
            Selector selector = this.selector;
            try {
                int selectCnt = 0;
                long currentTimeNanos = System.nanoTime();
                long selectDeadLineNanos = currentTimeNanos + this.delayNanos(currentTimeNanos);
                while (true) {
                    long timeoutMillis;
                    if ((timeoutMillis = (selectDeadLineNanos - currentTimeNanos + 500000L) / 1000000L) <= 0L) {
                        if (selectCnt != 0) break;
                        selector.selectNow();
                        selectCnt = 1;
                        break;
                    }
                    int selectedKeys = selector.select(timeoutMillis);
                    ++selectCnt;
                    if (selectedKeys != 0 || oldWakenUp || this.wakenUp.get() || this.hasTasks() || this.hasScheduledTasks()) break;
                    if (Thread.interrupted()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Selector.select() returned prematurely because Thread.currentThread().interrupt() was called. Use NioEventLoop.shutdownGracefully() to shutdown the NioEventLoop.");
                        }
                        selectCnt = 1;
                        break;
                    }
                    long time = System.nanoTime();
                    if (time - TimeUnit.MILLISECONDS.toNanos(timeoutMillis) >= currentTimeNanos) {
                        selectCnt = 1;
                    } else if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 && selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
                        logger.warn("Selector.select() returned prematurely {} times in a row; rebuilding selector.", (Object)selectCnt);
                        this.rebuildSelector();
                        selector = this.selector;
                        selector.selectNow();
                        selectCnt = 1;
                        break;
                    }
                    currentTimeNanos = time;
                }
                if (selectCnt > 3 && logger.isDebugEnabled()) {
                    logger.debug("Selector.select() returned prematurely {} times in a row.", (Object)(selectCnt - 1));
                }
            }
            catch (CancelledKeyException e2) {
                if (!logger.isDebugEnabled()) break block10;
                logger.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector - JDK bug?", e2);
            }
        }
    }

    private void selectAgain() {
        this.needsToSelectAgain = false;
        try {
            this.selector.selectNow();
        }
        catch (Throwable t2) {
            logger.warn("Failed to update SelectionKeys.", t2);
        }
    }

    static {
        block5: {
            logger = InternalLoggerFactory.getInstance(NioEventLoop.class);
            DISABLE_KEYSET_OPTIMIZATION = SystemPropertyUtil.getBoolean("io.netty.noKeySetOptimization", false);
            String key = "sun.nio.ch.bugLevel";
            try {
                String buglevel = SystemPropertyUtil.get(key);
                if (buglevel == null) {
                    System.setProperty(key, "");
                }
            }
            catch (SecurityException e2) {
                if (!logger.isDebugEnabled()) break block5;
                logger.debug("Unable to get/set System Property: {}", (Object)key, (Object)e2);
            }
        }
        int selectorAutoRebuildThreshold = SystemPropertyUtil.getInt("io.netty.selectorAutoRebuildThreshold", 512);
        if (selectorAutoRebuildThreshold < 3) {
            selectorAutoRebuildThreshold = 0;
        }
        SELECTOR_AUTO_REBUILD_THRESHOLD = selectorAutoRebuildThreshold;
        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.noKeySetOptimization: {}", (Object)DISABLE_KEYSET_OPTIMIZATION);
            logger.debug("-Dio.netty.selectorAutoRebuildThreshold: {}", (Object)SELECTOR_AUTO_REBUILD_THRESHOLD);
        }
    }
}

