package info.kgeorgiy.ja.shvetsov.bank.test;

import info.kgeorgiy.ja.shvetsov.bank.Client;
import info.kgeorgiy.ja.shvetsov.bank.account.Account;
import info.kgeorgiy.ja.shvetsov.bank.person.Person;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

@DisplayName("ClientBankTest")
public class ClientBankTest extends BaseBankTest {
    @DisplayName("Test 01: Creating person")
    @Test
    public void test01_createPerson() throws RemoteException {
        String firstName = STRINGS.get(0);
        String lastName = STRINGS.get(1);

        String passport = STRINGS.get(2);
        String subId = STRINGS.get(5);

        int money = INTEGERS.getFirst();

        Client.main(firstName, lastName, passport, subId, Integer.toString(money));
        Person person = bank.getPerson(passport, true);

        Assertions.assertNotNull(person);
        Assertions.assertNotNull(person.getAccount(subId));

        Assertions.assertEquals(passport, person.getPassport());
        Assertions.assertEquals(money, person.getAccount(subId).getAmount());
    }

    @DisplayName("Test 02: Creating multiple persons")
    @Test
    public void test02_multiplePersons() throws RemoteException {
        for (int i = 0; i < STRINGS.size() / 3; i++) {
            String firstName = STRINGS.get(i * 3);
            String lastName = STRINGS.get(i * 3 + 1);

            String passport = STRINGS.get(i * 3 + 2);
            String subId = STRINGS.get((i * 3 + 3));

            int money = INTEGERS.get(i);

            Client.main(firstName, lastName, passport, subId, Integer.toString(money));
            Person person = bank.getPerson(passport, true);

            Assertions.assertNotNull(person);
            Assertions.assertNotNull(person.getAccount(subId));

            Assertions.assertEquals(passport, person.getPassport());
            Assertions.assertEquals(money, person.getAccount(subId).getAmount());
        }
    }

    @DisplayName("Test 03: Updating person account multiple times")
    @Test
    public void test03_multipleUpdatesOfAccount() throws RemoteException {
        String firstName = STRINGS.get(0);
        String lastName = STRINGS.get(1);

        String passport = STRINGS.get(2);
        String subId = STRINGS.get(8);

        int oldMoney = INTEGERS.getFirst();

        Client.main(firstName, lastName, passport, subId, Integer.toString(oldMoney));
        Person person = bank.getPerson(passport, true);

        Assertions.assertNotNull(person);
        Assertions.assertNotNull(person.getAccount(subId));

        Assertions.assertEquals(passport, person.getPassport());
        Assertions.assertEquals(oldMoney, person.getAccount(subId).getAmount());

        int newMoney = INTEGERS.get(2);

        Client.main(firstName, lastName, passport, subId, Integer.toString(newMoney));
        Assertions.assertEquals(oldMoney + newMoney, person.getAccount(subId).getAmount());
    }

    @DisplayName("Test 04: Creating multiple accounts of one person")
    @Test
    public void test04_multipleAccountsOfPerson() throws RemoteException {
        String firstName = STRINGS.get(3);
        String lastName = STRINGS.get(4);
        String passport = STRINGS.get(5);

        Person person = bank.getPerson(passport, true);
        if (person == null) {
            person = bank.createPerson(firstName, lastName, passport);
        }

        Map<String, Integer> accounts = new HashMap<>();

        for (int i = 0; i < 10; ++i) {
            String subId = STRINGS.get((i * 3) % STRINGS.size());
            Integer money = INTEGERS.get(i % INTEGERS.size());

            Account account = person.getAccount(subId);
            if (account != null)
                accounts.put(subId, account.getAmount());

            Client.main(firstName, lastName, passport, subId, Integer.toString(money));
            account = person.getAccount(subId);

            Assertions.assertEquals(accounts.getOrDefault(subId, 0) + money, account.getAmount());
            accounts.put(subId, accounts.getOrDefault(subId, 0) + money);
        }
    }
}
