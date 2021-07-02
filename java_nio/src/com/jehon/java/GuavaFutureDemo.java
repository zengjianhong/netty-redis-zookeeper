package com.jehon.java;

import com.google.common.util.concurrent.*;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author jehon
 */
public class GuavaFutureDemo {

    public static final int SLEEP_GAP = 500;

    public static String getCurThreadName() {
        return Thread.currentThread().getName();
    }

    // 业务逻辑：烧水
    static class HotWaterJob implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            //
            return null;
        }
    }

    // 业务逻辑：清洗
    static class WashJob implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            return null;
        }
    }

    // 新创建一个异步业务类型，作为泡茶喝主线程类
    static class MainJob implements Runnable {

        boolean waterOk = false;
        boolean cupOk = false;
        int gap = SLEEP_GAP / 10;

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(gap);
                    System.out.println("读书中……");
                } catch (InterruptedException e) {
                    System.out.println(getCurThreadName() + "发生异常被中断。");
                    e.printStackTrace();
                }
                if (waterOk && cupOk) {
                    drinkTea(waterOk, cupOk);
                }
            }
        }

        public void drinkTea(Boolean wOk, Boolean cOk) {
            if (wOk && cOk) {
                System.out.println("泡茶喝，茶喝完");
                this.waterOk = false;
                this.gap = SLEEP_GAP * 100;
            } else if (!wOk) {
                System.out.println("烧水失败，没有茶喝了");
            } else if (!cOk) {
                System.out.println("杯子洗不了，没有茶喝了");
            }
        }
    }

    public static void main(String[] args) {
        // 创建一个新的线程实例，作为泡茶主线程
        MainJob mainJob = new MainJob();
        Thread mainThread = new Thread(mainJob);
        mainThread.setName("主线程");
        mainThread.start();

        // 烧水的业务逻辑实例
        Callable<Boolean> hotJob = new HotWaterJob();
        // 清洗的业务逻辑实例
        Callable<Boolean> washJob = new WashJob();

        // 创建java线程池
        ExecutorService jPool = Executors.newFixedThreadPool(10);
        // 包装java线程池，构件Guava线程池
        ListeningExecutorService gPool = MoreExecutors.listeningDecorator(jPool);

        // 提交烧水的业务逻辑实例，到Guava线程池获取异步任务
        ListenableFuture<Boolean> hotFuture = gPool.submit(hotJob);
        // 绑定异步回调，烧水完成后，把喝水任务的waterOk标志设置为true
        Futures.addCallback(hotFuture, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean r) {
                if (r) {
                    mainJob.waterOk = true;
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                System.out.println("烧水失败，没有茶喝了");
            }
        });

        // 提交清洗的业务逻辑实例，到Guava线程池获取异步任务
        ListenableFuture<Boolean> washFuture = gPool.submit(washJob);
        // 绑定任务执行完成后的回调逻辑到异步任务
        Futures.addCallback(washFuture, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean r) {
                if (r) {
                    mainJob.cupOk = true;
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                System.out.println("杯子洗不了，没有茶喝了");
            }
        });
    }
}
