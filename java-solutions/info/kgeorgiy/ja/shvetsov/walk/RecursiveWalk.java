package info.kgeorgiy.ja.shvetsov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;

import static java.lang.Integer.MAX_VALUE;

public class RecursiveWalk {

    public static void main(String[] args) {
        walk(args, MAX_VALUE);
    }

    public static void walk(String[] args, int maxDepth) {
        char algorithm;

        if ((args == null) || (args.length != 2 && args.length != 3) || (args[0] == null) || (args[1] == null)) {
            System.err.println("Usage: java [Recursive|Advanced]Walk <input> <output>.");
            return;
        } else if (args.length == 2) {
            algorithm = 'j';
        } else if (args[2] != null) {
            if (args[2].equals("jenkins")) {
                algorithm = 'j';
            } else if (args[2].equals("sha-1")) {
                algorithm = 's';
            } else {
                System.err.println("Usage: java [Recursive|Advanced]Walk <input> <output>.");
                return;
            }
        } else {
            System.err.println("Usage: java [Recursive|Advanced]Walk <input> <output>.");
            return;
        }

        try {
            Path input = Path.of(args[0]);

            try {
                Path output = Path.of(args[1]);

                // creating parent directories for the output file
                try {
                    if (output.getParent() != null) {
                        Files.createDirectories(output.getParent());
                    }
                } catch (UnsupportedOperationException e) {
                    System.err.println("One of the attributes can not be set atomically while creating " +
                            "parent directories for the output file: " + e.getMessage() + ".");
                    return;
                } catch (IOException e) {
                    System.err.println("An I/O error occurred while creating parent directories " +
                            "for the output file: " + e.getMessage() + ".");
                    return;
                } catch (SecurityException e) {
                    System.err.println("No access to the system property \"user.dir\" (while " +
                            "creating parent directories for the output file): " + e.getMessage() + ".");
                    return;
                }

                try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
                    try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                        while (reader.ready()) {
                            String line = reader.readLine();

                            try {
                                Path file = Path.of(line);

                                RecursiveWalkFileVisitor visitor = new RecursiveWalkFileVisitor(writer, algorithm);
                                Files.walkFileTree(file, Collections.emptySet(), maxDepth, visitor);
                            } catch (IOException e) {
                                System.err.println("An I/O error was thrown by a visitor method: "
                                        + e.getMessage() + ".");
                                writeZeroes(writer, algorithm, line);
                            } catch (SecurityException e) {
                                System.err.println("The security manager denied access to the starting file " +
                                        "or directory: " + e.getMessage() + ".");
                                writeZeroes(writer, algorithm, line);
                            } catch (InvalidPathException e) {
                                System.err.println("Invalid path for the file the program is trying to hash: "
                                        + e.getMessage() + ".");
                                writeZeroes(writer, algorithm, line);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid combination of options for the BufferedWriter of the output file: "
                                + e.getMessage() + ".");
                    } catch (IOException e) {
                        System.err.println("An I/O error occurred while opening the output file " +
                                "trying to create a BufferedWriter: " + e.getMessage() + ".");
                    } catch (UnsupportedOperationException e) {
                        System.err.println("Unsupported option for the BufferedWriter of the output file: "
                                + e.getMessage() + ".");
                    } catch (SecurityException e) {
                        System.err.println("No write access for the output file: " + e.getMessage() + ".");
                    }
                } catch (IOException e) {
                    System.err.println("An I/O error occurred while opening the input file " +
                            "trying to create a BufferedReader: " + e.getMessage() + ".");
                } catch (SecurityException e) {
                    System.err.println("No read access for the input file: " + e.getMessage() + ".");
                }
            } catch (InvalidPathException e) {
                System.err.println("Couldn't find the output file: " + e.getMessage() + ".");
            }
        } catch (InvalidPathException e) {
            System.err.println("Couldn't find the input file: " + e.getMessage() + ".");
        }
    }

    private static void writeZeroes(BufferedWriter writer, char algorithm, String line) throws IOException {
        if (algorithm == 'j') {
            writer.write(String.format("%08x %s%n", 0, line));
        } else {
            writer.write(String.format("%s %s%n", "0".repeat(40), line));
        }
    }
}