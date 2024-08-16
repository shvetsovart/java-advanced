package info.kgeorgiy.ja.shvetsov.bank;

import info.kgeorgiy.ja.shvetsov.bank.account.Account;
import info.kgeorgiy.ja.shvetsov.bank.bank.Bank;
import info.kgeorgiy.ja.shvetsov.bank.person.Person;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public final class Client {
    /** Utility class. */
    private Client() {}

    public static void main(final String... args) throws RemoteException {
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }

        if (args.length != 5) {
            System.err.println("Incorrect arguments. Usage: Client firstName secondName passport subId change_amount");
            return;
        }

        if (args[0] == null || args[1] == null || args[2] == null || args[3] == null || args[4] == null) {
            System.err.println("Non-null arguments are required.");
            return;
        }

        String firstName = args[0];
        String lastName = args[1];
        String passport = args[2];
        String subId = args[3];
        int change_amount;

        try {
            change_amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("Amount to change (args[4]) should be parseable number.");
            return;
        }

        Person person = bank.getPerson(passport, true);

        if (person == null) {
            System.out.println("Creating person");
            person = bank.createPerson(firstName, lastName, passport);
        }

        Account account = bank.getAccount(subId);
        if (account == null) {
            System.out.println("Creating account");
            account = person.createAccount(subId);
        } else {
            System.out.println("Account already exists");
        }

        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + change_amount);
        System.out.println("Money: " + account.getAmount());
    }
}
