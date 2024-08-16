package info.kgeorgiy.ja.shvetsov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    private static final int REQUEST_ATTEMPTS = 100;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        ExecutorService workers = Executors.newFixedThreadPool(threads);

        IntStream.range(0, threads).forEach(threadId -> workers.execute(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(HelloUDP.TIMEOUT_RECEIVE);
                socket.connect(socketAddress);

                DatagramPacket responsePacket = HelloUDP.createResponsePacket(socket.getReceiveBufferSize());

                IntStream.range(0, requests).forEach(requestId -> {
                    String request = String.format("%s%d_%d", prefix, threadId + 1, requestId + 1);
                    DatagramPacket sendPacket = HelloUDP.createSendPacket(socketAddress, request);

                    for (int i = 0; i < REQUEST_ATTEMPTS; i++) {
                        try {
                            socket.send(sendPacket);
                            socket.receive(responsePacket);

                            String response = HelloUDP.createResponseString(responsePacket);
                            if (response.contains(request)) {
                                System.out.println(request + "\n" + response);
                                break;
                            }
                        } catch (SocketTimeoutException e) {
                            System.err.println("ERROR: Timeout while receiving response: " + e.getMessage());
                        } catch (IOException e) {
                            System.err.println("ERROR: I/O (Client): " + e.getMessage());
                        }
                    }
                });
            } catch (SocketException e) {
                System.err.println("ERROR: Could not open socket: " + e.getMessage());
            }
        }));

        workers.close();
    }
}