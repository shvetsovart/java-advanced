package info.kgeorgiy.ja.shvetsov.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer implements NewHelloServer {
    private Selector selector;
    private final Map<DatagramChannel, Queue<MetaInfo>> portsChannelsMap = new HashMap<>();
    private ExecutorService receivePool;
    private ExecutorService pool;

    @Override
    public void start(int threads, Map<Integer, String> ports) {
        try {
            selector = Selector.open();
            receivePool = Executors.newFixedThreadPool(threads);
            pool = Executors.newFixedThreadPool(ports.size());

            for (Map.Entry<Integer, String> entry : ports.entrySet()) {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                int port = entry.getKey();
                channel.register(selector, SelectionKey.OP_READ, new InetSocketAddress(port));
                channel.bind(new InetSocketAddress(port));
                portsChannelsMap.put(channel, new ConcurrentLinkedDeque<>());
                pool.submit(() -> startServerByChannel(channel, entry.getValue()));
            }
        } catch (final IOException e) {
            System.err.printf("Server setup error: %s%n", e.getLocalizedMessage());
        }
    }

    @Override
    public void close() {
        try {
            portsChannelsMap.forEach((channel, queue) -> {
                if (channel != null) {
                    channel.socket().close();
                }
            });
            if (Objects.nonNull(selector)) selector.close();
            if (Objects.nonNull(selector)) receivePool.close();
            if (pool != null) pool.close();
        } catch (final IOException ignored) {
        }
    }

    private void startServerByChannel(DatagramChannel channel, String regex) {
        while (!Thread.interrupted() && !channel.socket().isClosed()) {
            try {
                if (selector.select() > 0) {
                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        final var key = i.next();
                        try {
                            if (key.isValid()) {
                                if (key.isReadable()) {
                                    read(channel, key, regex);
                                } else {
                                    write(channel, key);
                                }
                            }
                        } finally {
                            i.remove();
                        }
                    }
                }
            } catch (final IOException e) {
                System.err.printf("Selector I/O exception: %s%n", e.getLocalizedMessage());
                close();
            }
        }
    }

    private void write(DatagramChannel channel, final SelectionKey key) {
        Queue<MetaInfo> responses = portsChannelsMap.get(channel);
        if (!responses.isEmpty()) {
            final var metaInfo = responses.poll();
            final var buffer = ByteBuffer.wrap(metaInfo.response.getBytes(StandardCharsets.UTF_8));
            try {
                channel.send(buffer, metaInfo.socket);
            } catch (final IOException e) {
                System.err.printf("Write I/O exception: %s", e.getLocalizedMessage());
            }
            key.interestOpsOr(SelectionKey.OP_READ);
        } else {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void read(DatagramChannel channel, final SelectionKey key, String regex) {
        System.out.println(regex);
        try {
            final var buffer = ByteBuffer.allocate(channel.socket().getReceiveBufferSize());
            final var address = channel.receive(buffer);
            receivePool.submit(() -> {
                buffer.flip();
                final var receive = StandardCharsets.UTF_8.decode(buffer).toString();
                final var response = regex.replace("$", receive);
                portsChannelsMap.get(channel).add(MetaInfo.of(address, response));
                key.interestOps(SelectionKey.OP_WRITE);
                selector.wakeup();
            });
        } catch (final IOException e) {
            System.err.printf("Read I/O exception: %s", e.getLocalizedMessage());
        }
    }

    private record MetaInfo(SocketAddress socket, String response) {
        public static MetaInfo of(final SocketAddress address, final String response) {
            return new MetaInfo(address, response);
        }
    }
}
