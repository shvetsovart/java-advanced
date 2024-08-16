package info.kgeorgiy.ja.shvetsov.bank.person;

import info.kgeorgiy.ja.shvetsov.bank.account.RemoteAccount;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public abstract class AbstractPerson implements Person, Serializable {
    protected final String firstName;
    protected final String lastName;
    protected final String passport;

    protected final ConcurrentMap<String, RemoteAccount> accounts;

    /**
     * Constructor out of first name, last name and passport.
     * @param firstName first name of a person
     * @param lastName last name of a person
     * @param passport passport of a person
     */
    protected AbstractPerson(String firstName, String lastName, String passport) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passport = passport;
        this.accounts = new ConcurrentHashMap<>();
    }

    /**
     * Constructor out of other {@link AbstractPerson}.
     * @param other other {@link AbstractPerson} person
     */
    protected AbstractPerson(AbstractPerson other) {
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.passport = other.passport;
        this.accounts = other.accounts.entrySet().stream().collect(
                Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        entry -> new RemoteAccount(entry.getValue())
                ));
    }

    /**
     * Returns account id of the form: {@code passport:subId}.
     * @param subId subId of the person's account
     * @return account id
     */
    protected String getAccountId(String subId) {
        return String.format("%s:%s", passport, subId);
    }


    @Override
    public String getFirstName() throws RemoteException {
        return firstName;
    }

    @Override
    public String getLastName() throws RemoteException {
        return lastName;
    }

    @Override
    public String getPassport() throws RemoteException {
        return passport;
    }
}
