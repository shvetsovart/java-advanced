package info.kgeorgiy.ja.shvetsov.bank.person;

import info.kgeorgiy.ja.shvetsov.bank.account.Account;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {
    /**
     * Returns first name of a {@link Person}.
     * @return first name
     */
    String getFirstName() throws RemoteException;

    /**
     * Returns last name of a {@link Person}.
     * @return last name
     */
    String getLastName() throws RemoteException;

    /**
     * Returns passport of a {@link Person}.
     * @return passport
     */
    String getPassport() throws RemoteException;

    /**
     * Creates account with {@code subId} identifier.
     * @param subId identifier of an account
     * @return created or existing account of a person with
     */
    Account createAccount(String subId) throws RemoteException;

    /**
     * Returns account with {@code subId} identifier.
     * @param subId {@code subId} part of bank account id ({@code passport:subId})
     * @return account or {@code null} if account doesn't exist
     */
    Account getAccount(String subId) throws RemoteException;
}
