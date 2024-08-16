package info.kgeorgiy.ja.shvetsov.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HelloUDP {
    public static final int TIMEOUT_RECEIVE = 300;
    private static final Charset UTF_8_CHARSET = StandardCharsets.UTF_8;

    static DatagramPacket createResponsePacket(int size) {
        byte[] buffer = new byte[size];
        return new DatagramPacket(buffer, buffer.length);
    }

    static DatagramPacket createSendPacket(SocketAddress socketAddress, String data) {
        byte[] buffer = data.getBytes(StandardCharsets.UTF_8);

        return new DatagramPacket(buffer, 0, buffer.length, socketAddress);
    }

    static String createResponseString(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), UTF_8_CHARSET);
    }

//    static int getChannelSize(DatagramChannel channel) throws IOException {
//        int receive = channel.getOption(StandardSocketOptions.SO_RCVBUF);
//        int send = channel.getOption(StandardSocketOptions.SO_SNDBUF);
//
//        return Math.max(receive, send);
//    }
//
//    static String createResponseString(ByteBuffer buffer) {
//        return UTF_8_CHARSET.decode(buffer).toString();
//    }
//
//    static byte[] getBytes(String string) {
//        return string.getBytes(UTF_8_CHARSET);
//    }
//
//    static boolean isCorrectResponse(String response, int threadIndex, int requestIndex) {
//        String regex = String.format("^\\D*%d\\D+%d\\D*$", threadIndex, requestIndex);
//        return Pattern.matches(response, regex);
//    }
}
