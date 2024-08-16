package info.kgeorgiy.ja.shvetsov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Implements interfaces
 * {@link info.kgeorgiy.java.advanced.implementor.Impler},
 * {@link info.kgeorgiy.java.advanced.implementor.JarImpler}.
 *
 * @author Artyom Shvetsov
 */
public class Implementor implements Impler, JarImpler {
    private static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * Generates .jar file implementing interface specified by provided token (second argument).
     * The implementation is generated on the target .jar file path (third argument).
     * Usage: {@code -jar <classname> <output path>}
     *
     * @param args three command line arguments: "-jar", classname, output path of target -jar file.
     */
    public static void main(String[] args) {
        try {
            if (args == null || args.length != 3 || !"-jar".equals(args[0])) {
                throw new ImplerException("Usage: -jar <classname> <output path>");
            }

            try {
                new Implementor().implementJar(Class.forName(args[1]), Path.of(args[2]));
            } catch (ClassNotFoundException e) {
                throw new ImplerException("ERROR: Class not found", e);
            } catch (InvalidPathException e) {
                throw new ImplerException("ERROR: Invalid path", e);
            }
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage().getName().replace('.', File.separatorChar));
        }
        root = root.resolve(getImplName(token) + ".java");

        if (root.getParent() != null) {
            try {
                Files.createDirectories(root.getParent());
            } catch (IOException e) {
                throw new ImplerException("ERROR: Could not create directories for root", e);
            }
        }

        try (Writer writer = new EncodingWriter(Files.newBufferedWriter(root, StandardCharsets.UTF_8))) {
            writer.write(generateHeader(token));

            for (Method method : token.getMethods()) {
                writer.write(generateMethod(method));
            }

            writer.write('}' + LINE_SEPARATOR);
        } catch (IOException e) {
            throw new ImplerException("ERROR: Could not create the file to write implementation due to I/O issues", e);
        } catch (SecurityException e) {
            throw new ImplerException("ERROR: Could not create the file to write implementation due to security issues", e);
        } catch (InvalidPathException e) {
            throw new ImplerException("ERROR: Invalid path to implementation file", e);
        }
    }

    /**
     * Generates implementation of provided {@code method}.
     * The implementation consists of method signature, return line,
     * opening and closing curly braces.
     *
     * @param method {@link Method method} to implement.
     * @return {@link String} object of implementation for {@code method}.
     */
    private String generateMethod(Method method) {
        return generateSignature(method) +
                "{" + LINE_SEPARATOR + "    " +
                generateReturn(method.getReturnType()) +
                LINE_SEPARATOR + "    " + "}" + LINE_SEPARATOR;
    }

    /**
     * Generates return line for provided {@code method}.
     *
     * @param method {@link Method method} to generate return line for.
     * @return {@link String} object of return line for provided {@code method}.
     */
    private String generateReturn(Class<?> method) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("    return");

        if (method.equals(boolean.class)) {
            stringBuilder.append(" false;");
        } else if (method.equals(void.class)) {
            stringBuilder.append(";");
        } else if (method.isPrimitive()) {
            stringBuilder.append(" 0;");
        } else {
            stringBuilder.append(" null;");
        }

        return stringBuilder.toString();
    }

    /**
     * Generates signature for provided {@code method}.
     *
     * @param method {@link Method method} to generate signature for.
     * @return {@link String} object of signature for provided {@code method}.
     */
    private String generateSignature(Method method) {
        return "    " + generateModifiers(method) + " " +
                method.getReturnType().getCanonicalName() + " " + method.getName() +
                generateParameters(method) + generateExceptions(method) + " ";
    }

    /**
     * Generates exceptions for signature of provided {@code method}.
     *
     * @param method {@link Method method} to generate exceptions for.
     * @return {@link String} object of exceptions for signature of provided {@code method}.
     */
    private String generateExceptions(Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        Class<?>[] exceptions = method.getExceptionTypes();

        if (exceptions.length != 0) {
            stringBuilder.append(" throws ").append(
                    Arrays.stream(exceptions)
                            .map(Class::getCanonicalName)
                            .collect(Collectors.joining(", "))
            );
        }

        return stringBuilder.toString();
    }

    /**
     * Generates parameters for signature of provided {@code method}.
     *
     * @param method {@link Method method} to generate parameters for.
     * @return {@link String} object of parameters for signature of provided {@code method}.
     */
    private String generateParameters(Method method) {
        return Arrays.stream(method.getParameters())
                .map(parameter -> parameter.getType().getCanonicalName() + " " + parameter.getName())
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Generates modifiers for signature of provided {@code method}.
     *
     * @param method {@link Method method} to generate parameters for.
     * @return {@link String} object of modifiers for signature of provided {@code method}.
     */
    private String generateModifiers(Method method) {
        return Modifier.toString(method.getModifiers()
                & ~Modifier.NATIVE & ~Modifier.TRANSIENT & ~Modifier.ABSTRACT);
    }

    /**
     * Generates header for interface specified by provided token.
     * Generated header consists of package line, signature of class line.
     *
     * @param token {@link Class} token of interface specified by provided token.
     * @return {@link String} object of implementation for interface specified by provided token.
     * @throws ImplerException if incorrect token is given (array or enum) or interface specified by provided token
     *                         is private or final.
     */
    private String generateHeader(Class<?> token) throws ImplerException {
        StringBuilder stringBuilder = new StringBuilder();
        String packageName = token.getPackageName();

        if (token.isArray()) {
            throw new ImplerException("ERROR: Can not inherit from an array");
        }
        if (token.isEnum()) {
            throw new ImplerException("ERROR: Can not inherit from enum");
        }

        int modifiers = token.getModifiers();

        if (Modifier.isPrivate(modifiers)) {
            throw new ImplerException("ERROR: Can not implement private interface");
        }
        if (Modifier.isFinal(modifiers)) {
            throw new ImplerException("ERROR: Can not inherit from final interface");
        }

        if (!packageName.isEmpty()) {
            stringBuilder.append("package ")
                    .append(packageName)
                    .append(";")
                    .append(LINE_SEPARATOR)
                    .append(LINE_SEPARATOR);
        }

        stringBuilder.append("public class ")
                .append(getImplName(token))
                .append(" implements ")
                .append(getInterfaceName(token))
                .append(" {")
                .append(LINE_SEPARATOR);

        return stringBuilder.toString();
    }

    /**
     * Returns name of interface specified by provided token even if it is nested.
     *
     * @param token interface token.
     * @return {@link String} object of name of interface specified by provided token.
     */
    private String getInterfaceName(Class<?> token) {
        Class<?> declaringClass = token.getDeclaringClass();
        if (declaringClass == null) {
            return token.getSimpleName();
        } else {
            return declaringClass.toString().split(" ")[1] + "." + token.getSimpleName();
        }
    }

    /**
     * Return implementation name of interface specified by provided token.
     *
     * @param token interface token.
     * @return {@link String} object of implementation name of interface specified by provided token.
     */
    private String getImplName(final Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path root;

        try {
            root = Files.createTempDirectory(jarFile.getParent(), "temp");
        } catch (IOException e) {
            throw new ImplerException("ERROR: Could not create temporary folder", e);
        }

        implement(token, root);

        Path packagePath = root.resolve(getPackagePath(token));
        File javaFile = packagePath.resolve(getImplName(token) + ".java").toFile();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String classpath = getClasspath(token);

        if (compiler.run(null, null, null, "-cp", classpath, javaFile.toString()) != 0) {
            throw new ImplerException("ERROR: Compilation error");
        }

        try {
            Files.createDirectories(jarFile.getParent());
        } catch (IOException e) {
            throw new ImplerException("ERROR: Could not create output directories", e);
        }

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        File classFile = packagePath.resolve(getImplName(token) + ".class").toFile();

        try (InputStream inputStream = new FileInputStream(classFile);
             JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile.toFile()), manifest)
        ) {
            String jarEntryName = toJarEntry(getPackagePath(token).resolve(getImplName(token) + ".class"));
            jarOutputStream.putNextEntry(new JarEntry(jarEntryName));

            inputStream.transferTo(jarOutputStream);
        } catch (IOException e) {
            throw new ImplerException("ERROR: Could not create .jar file", e);
        }
    }

    /**
     * Returns package {@link Path} for interface specified by provided token.
     *
     * @param token interface token.
     * @return {@link Path} of package containing interface specified by provided token.
     */
    private Path getPackagePath(final Class<?> token) {
        return Path.of(token.getPackageName().replace(".", File.separator));
    }

    /**
     * Converts {@link Path} to {@link String} object of .jar-style path to target
     * file (converts all separators to forward slashes).
     *
     * @param path {@link Path} to .jar target file.
     * @return {@link String} of correct .jar-style path to target file.
     */
    private String toJarEntry(final Path path) {
        return path.toString().replace(File.separator, "/");
    }

    /**
     * Returns classpath of interface specified by provided token.
     *
     * @param token {@link Class} object specifying interface.
     * @return classpath for target interface specified by provided token.
     */
    private String getClasspath(final Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError("Provided token as a string could not be parsed as a URI reference", e);
        }
    }

    /**
     * FilterWriter converting Unicode characters to ASCII characters with escape sequences.
     */
    private static class EncodingWriter extends FilterWriter {
        /**
         * Default constructor of {@link EncodingWriter} class.
         *
         * @param writer a {@link Writer} object.
         */
        public EncodingWriter(final Writer writer) {
            super(writer);
        }

        /**
         * Converts Unicode code point to escape sequence.
         *
         * @param codepoint Unicode's character code point.
         * @return {@link String} of escape sequence.
         */
        private String escape(int codepoint) {
            return String.format("\\u%04x", codepoint);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            // :NOTE: долго по одному символу
            for (int i = 0; i < len; i++) {
                write(cbuf[i + off]);
            }
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                write(str.charAt(i + off));
            }
        }

        @Override
        public void write(int c) throws IOException {
            if (0 <= c && c <= 128) {
                out.write(c);
            } else {
                out.write(escape(c));
            }
        }
    }
}
