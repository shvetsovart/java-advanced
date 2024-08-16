package info.kgeorgiy.ja.shvetsov.bank.bank;

import info.kgeorgiy.ja.shvetsov.bank.account.RemoteAccount;
import info.kgeorgiy.ja.shvetsov.bank.person.AbstractPerson;
import info.kgeorgiy.ja.shvetsov.bank.person.LocalPerson;
import info.kgeorgiy.ja.shvetsov.bank.person.Person;
import info.kgeorgiy.ja.shvetsov.bank.person.RemotePerson;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final Exporter<Remote, RemoteException> exporter;
    private final ConcurrentMap<String, RemoteAccount> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AbstractPerson> persons = new ConcurrentHashMap<>();

    public RemoteBank(Exporter<Remote, RemoteException> exporter) {
        this.exporter = exporter;
    }

    @Override
    public RemoteAccount createAccount(final String id) throws RemoteException {
        System.out.println("Creating account " + id);

        final RemoteAccount account = new RemoteAccount(id);

        if (accounts.putIfAbsent(id, account) == null) {
            exporter.export(account);
            return account;
        } else {
            return getAccount(id);
        }
    }

    @Override
    public RemoteAccount getAccount(final String id) {
        System.out.println("Retrieving account " + id);

        return accounts.get(id);
    }

    @Override
    public Person createPerson(String firstName, String lastName, String passport) throws RemoteException {
        System.out.println("Creating person " + "(" + firstName + ", " + lastName + ", " + passport + ")");

        final AbstractPerson person = new RemotePerson(this, firstName, lastName, passport);
        exporter.export(person);
        persons.putIfAbsent(passport, person);

        return person;
    }

    @Override
    public Person getPerson(String passport, boolean remote) throws RemoteException {
        System.out.println("Retrieving person " + passport);

        AbstractPerson person = persons.get(passport);

        return remote || person == null ? person : new LocalPerson(person);
    }
}
