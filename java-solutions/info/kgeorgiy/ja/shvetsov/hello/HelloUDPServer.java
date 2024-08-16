package info.kgeorgiy.ja.shvetsov.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements NewHelloServer {
    private static ExecutorService receivePool;
    private static ExecutorService sendPool;

    private final Map<Integer, DatagramSocket> portsSocketsMap = new HashMap<>();

    @Override
    public void start(int threads, Map<Integer, String> ports) {
        if (threads <= 0 || ports.isEmpty()) {
            return;
        }

        receivePool = Executors.newFixedThreadPool(ports.size());
        sendPool = Executors.newFixedThreadPool(threads);

        ports.forEach((port, regex) -> {
            try {
                portsSocketsMap.put(port, new DatagramSocket(port));
            } catch (SocketException e) {
                System.err.println("ERROR: Could not create socket: " + e.getMessage() + ", " + port);
            }

            receivePool.submit(() -> {
                while (!portsSocketsMap.get(port).isClosed() && !Thread.currentThread().isInterrupted()) {
                    try {
                        DatagramPacket receivePacket = HelloUDP.createResponsePacket(portsSocketsMap.get(port).getReceiveBufferSize());
                        portsSocketsMap.get(port).receive(receivePacket);
                        String request = HelloUDP.createResponseString(receivePacket);

                        sendPool.submit(() -> {
                            String response = regex.replace("$", request);
                            try {
                                portsSocketsMap.get(port).send(HelloUDP.createSendPacket(receivePacket.getSocketAddress(), response));
                            } catch (IOException e) {
                                System.err.println("ERROR: I/O (Server, sending): " + e.getMessage());
                            }
                        });
                    } catch (IOException e) {
                        System.err.println("ERROR: I/O (Server, receiving): " + e.getMessage());
                    }
                }
            });
        });
    }

    @Override
    public synchronized void close() {
        portsSocketsMap.forEach((port, socket) -> socket.close());
        portsSocketsMap.clear();

        if (receivePool != null)
            receivePool.close();
        if (sendPool != null)
            sendPool.close();
    }
}