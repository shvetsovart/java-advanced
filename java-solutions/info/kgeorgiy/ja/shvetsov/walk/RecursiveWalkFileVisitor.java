package info.kgeorgiy.ja.shvetsov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class RecursiveWalkFileVisitor extends SimpleFileVisitor<Path> {
    private final static int BUFFER_SIZE = 1024;
    private final BufferedWriter writer;
    private final char algorithm;

    public RecursiveWalkFileVisitor(BufferedWriter writer, char algorithm) {
        this.writer = writer;
        this.algorithm = algorithm;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (algorithm == 'j') {
            writer.write(String.format("%08x %s%n", makeJenkinsHashOfFile(file), file));
        } else {
            writer.write(String.format("%s %s%n", makeSHAHashOfFile(file), file));
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        if (algorithm == 'j') {
            writer.write(String.format("%08x %s%n", 0, file));
        } else {
            writer.write(String.format("%s %s%n", "0".repeat(40), file));
        }
        return FileVisitResult.CONTINUE;
    }

    private static int makeJenkinsHashOfFile(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int hash = 0;

            while (inputStream.available() > 0) {
                int length = inputStream.read(buffer);

                for (int i = 0; i < length; ++i) {
                    hash += (buffer[i] & 0xff);
                    hash += (hash << 10);
                    hash ^= (hash >>> 6);
                }
            }

            hash += (hash << 3);
            hash ^= (hash >>> 11);
            hash += (hash << 15);

            return hash;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid combination of options for the InputStream of the file " +
                    "the program is trying to hash (Jenkins): "
                    + e.getMessage() + ".");
        } catch (UnsupportedOperationException e) {
            System.err.println("Unsupported option for the InputStream of the file " +
                    "the program is trying to hash (Jenkins): "
                    + e.getMessage() + ".");
        } catch (IOException e) {
            System.err.println("An I/O error occurred while opening the file " +
                    "the program is trying to hash (Jenkins): " + e.getMessage() + ".");
        } catch (SecurityException e) {
            System.err.println("No read access for the file the program is trying to hash (Jenkins): "
                    + e.getMessage() + ".");
        }

        return 0;
    }

    private static String makeSHAHashOfFile(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] buffer = new byte[BUFFER_SIZE];

                while (inputStream.available() > 0) {
                    int length = inputStream.read(buffer);

                    digest.update(buffer, 0, length);
                }

                return HexFormat.of().formatHex(digest.digest());
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Couldn't find SHA-1 algorithm: " + e.getMessage() + ".");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid combination of options for the InputStream of the file " +
                    "the program is trying to hash (SHA-1): "
                    + e.getMessage() + ".");
        } catch (UnsupportedOperationException e) {
            System.err.println("Unsupported option for the InputStream of the file " +
                    "the program is trying to hash (SHA-1): "
                    + e.getMessage() + ".");
        } catch (IOException e) {
            System.err.println("An I/O error occurred while opening the file " +
                    "the program is trying to hash (SHA-1): " + e.getMessage() + ".");
        } catch (SecurityException e) {
            System.err.println("No read access for the file the program is trying to hash (SHA-1): "
                    + e.getMessage() + ".");
        }

        return "0".repeat(40);
    }
}
