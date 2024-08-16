package info.kgeorgiy.ja.shvetsov.bank.person;

import info.kgeorgiy.ja.shvetsov.bank.account.Account;
import info.kgeorgiy.ja.shvetsov.bank.account.RemoteAccount;
import info.kgeorgiy.ja.shvetsov.bank.bank.RemoteBank;

import java.rmi.RemoteException;

public class RemotePerson extends AbstractPerson {
    private final transient RemoteBank bank;

    /**
     * Constructor out of {@link RemoteBank} and first name, last name, passport of a person.
     * @param bank {@link RemoteBank} to which a person is attached
     * @param firstName first name of a person
     * @param lastName last name of a person
     * @param passport passport of a person
     */
    public RemotePerson(RemoteBank bank, String firstName, String lastName, String passport) {
        super(firstName, lastName, passport);
        this.bank = bank;
    }

    @Override
    public Account createAccount(String subId) throws RemoteException {
        System.out.println("Creating account by subId of a person " + passport + ", subId - " + subId);

        String accountId = getAccountId(subId);
        RemoteAccount account = bank.createAccount(accountId);
        accounts.putIfAbsent(subId, account);

        return account;
    }

    // :NOTE: точно кидает RemoteException?
    @Override
    public Account getAccount(String subId) throws RemoteException {
        System.out.println("Getting account by subId of a person " + passport + ", subId - " + subId);

        RemoteAccount account = accounts.get(subId);
        if (account == null) {
            String accountId = getAccountId(subId);
            account = bank.getAccount(accountId);
        }

        return account;
    }
}
