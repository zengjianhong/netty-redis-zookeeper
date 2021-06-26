package com.jehon.java;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jehon
 */
public class MultiThreadEchoServerReactor {

    ServerSocketChannel serverSocket;
    AtomicInteger next = new AtomicInteger(0);

    // 选择器集合，引入多个选择器
    Selector[] selectors = new Selector[2];
    // 引入多个子反应器
    SubReactor[] subReactors = null;

    MultiThreadEchoServerReactor() throws IOException {
        // 初始化多个选择器
        selectors[0] = Selector.open();
        selectors[1] = Selector.open();
        serverSocket = ServerSocketChannel.open();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8888);
        serverSocket.socket().bind(address);
        //非阻塞
        serverSocket.configureBlocking(false);
        // 第一个选择器，负责监控新连接事件
        SelectionKey sk = serverSocket.register(selectors[0], SelectionKey.OP_ACCEPT);
        // 绑定Handler: attach新连接监控handler处理器到SelectionKey（选择键）
        sk.attach(new AcceptorHandler());
        // 第一个子反应器，一子反应器负责一个选择器
        SubReactor subReactor1 = new SubReactor(selectors[0]);
        // 第二个反应器，一子反应器负责一个选择器
        SubReactor subReactor2 = new SubReactor(selectors[1]);
        subReactors = new SubReactor[]{subReactor1, subReactor2};
    }

    private void startService() {
        // 一个子反应器对应一个线程
        new Thread(subReactors[0]).start();
        new Thread(subReactors[1]).start();
    }

    class SubReactor implements Runnable {

        // 每个线程负责一个选择器的查询和选择
        final Selector selector;

        SubReactor(Selector selector) {
            this.selector = selector;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    selector.select();
                    Set<SelectionKey> keySet = selector.selectedKeys();
                    Iterator<SelectionKey> it = keySet.iterator();
                    while (it.hasNext()) {
                        // 反应器负责dispathc收到的事件
                        SelectionKey sk = it.next();
                        dispatch(sk);
                    }
                    keySet.clear();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        void dispatch(SelectionKey sk) {
            Runnable handler = (Runnable) sk.attachment();
            // 调用之前attach绑定到选择键的handler处理器对象
            if (handler != null) {
                handler.run();
            }
        }
    }

    // Handler: 新连接处理器
    class AcceptorHandler implements Runnable {

        @Override
        public void run() {
            try {
                SocketChannel channel = serverSocket.accept();
                if (channel != null) {
                    new MultiThreadEchoHandler(selectors[next.get()],channel);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (next.incrementAndGet() == selectors.length) {
                next.set(0);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        MultiThreadEchoServerReactor server = new MultiThreadEchoServerReactor();
        server.startService();
    }
}
