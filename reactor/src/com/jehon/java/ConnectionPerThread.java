package com.jehon.java;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author jehon
 */
public class ConnectionPerThread implements Runnable{

    @Override
    public void run() {
        try {
            // 服务器箭筒socket
            ServerSocket serverSocket = new ServerSocket(88888);
            while (!Thread.interrupted()) {
                Socket socket = serverSocket.accept();
                // 接收一个连接后，为socket连接，新建一个专属的处理器对象
                Handler handler = new Handler(socket);
                // 创建新线程，专门负责一个连接的处理
                new Thread(handler).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    static class Handler implements Runnable {
        final Socket socket;
        Handler(Socket s) {
            this.socket = s;
        }

        @Override
        public void run() {
            while(true) {
                try {
                    byte[] input = new byte[1024];
                    // 读取数据
                    socket.getInputStream().read(input);
                    // 处理业务逻辑，获取处理结果
                    byte[] output = null;
                    // 写入结果
                    socket.getOutputStream().write(output);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
