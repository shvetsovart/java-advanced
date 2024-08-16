package info.kgeorgiy.ja.shvetsov.bank.person;

import info.kgeorgiy.ja.shvetsov.bank.account.Account;
import info.kgeorgiy.ja.shvetsov.bank.account.RemoteAccount;

import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson {
    /**
     * LocalPerson out of other {@link AbstractPerson}.
     * @param other other {@link AbstractPerson} person.
     */
    public LocalPerson(AbstractPerson other) {
        super(other);
    }

    // :NOTE: точно кидает RemoteException?
    @Override
    public Account createAccount(String subId) throws RemoteException {
        System.out.println("Creating account by subId of a person " + passport + ", subId - " + subId);

        String accountId = getAccountId(subId);
        return accounts.computeIfAbsent(subId, key -> new RemoteAccount(accountId));
    }

    // :NOTE: точно кидает RemoteException?
    @Override
    public Account getAccount(String subId) throws RemoteException {
        System.out.println("Getting account by subId of a person " + passport + ", subId - " + subId);

        return accounts.get(subId);
    }
}
