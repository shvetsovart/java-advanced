package info.kgeorgiy.ja.shvetsov.bank.bank;

import info.kgeorgiy.ja.shvetsov.bank.account.Account;
import info.kgeorgiy.ja.shvetsov.bank.person.Person;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it does not already exist.
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String id) throws RemoteException;

    /**
     * Returns account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    Account getAccount(String id) throws RemoteException;

    /**
     * Creates a new person with specified first name, last name and passport.
     * @param firstName first name of a person
     * @param lastName last name of a person
     * @param passport possport of a person
     * @return created or existing person
     */
    Person createPerson(String firstName, String lastName, String passport) throws RemoteException;

    /**
     * Returns a person by passport.
     * @param passport passport of a person
     * @param remote {@code true} if should be remote, {@code false} otherwise
     * @return person if person exists, {@code null} otherwise
     */
    Person getPerson(String passport, boolean remote) throws RemoteException;
}
