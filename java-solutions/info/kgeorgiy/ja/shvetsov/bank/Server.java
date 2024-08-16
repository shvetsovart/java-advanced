package info.kgeorgiy.ja.shvetsov.bank;

import info.kgeorgiy.ja.shvetsov.bank.bank.Bank;
import info.kgeorgiy.ja.shvetsov.bank.bank.RemoteBank;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Server {
    public static int DEFAULT_PORT = 1099;

    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final Queue<Remote> remotes = new ConcurrentLinkedQueue<>();

    public synchronized void start(int port) {
        if (isStarted.getAndSet(true)) {
            throw new IllegalStateException("ERROR: Couldn't start server. Server has already started.");
        }

        try {
            Registry registry = LocateRegistry.createRegistry(port);
            remotes.add(registry);

            try {
                Bank bank = new RemoteBank(this::exportObject);
                exportObject(bank);

                registry.rebind("bank", bank);
                System.out.println("Server started.");
            } catch (RemoteException e) {
                System.err.println("ERROR: Failed to create bank: " + e);
            }
        } catch (RemoteException e) {
            System.err.println("ERROR: Failed to create RMI registry: " + e);
        }
    }

    private void exportObject(Remote obj) throws RemoteException {
        if (!isStarted.get()) {
            throw new IllegalStateException("ERROR: Server is not running. Couldn't export " + obj);
        }

        UnicastRemoteObject.exportObject(obj, DEFAULT_PORT);

        remotes.add(obj);
    }

    public synchronized void close() {
        if (!isStarted.getAndSet(false)) {
            throw new IllegalStateException("ERROR: Couldn't close. Server is not running.");
        }

        remotes.forEach(remote -> {
            try {
                UnicastRemoteObject.unexportObject(remote, true);
            } catch (RemoteException ignored) {
            }
        });

        remotes.clear();
    }
}
