package info.kgeorgiy.ja.shvetsov.bank.bank;

@FunctionalInterface
public interface Exporter<T, E extends Exception> {
    void export(T t) throws E;
}
