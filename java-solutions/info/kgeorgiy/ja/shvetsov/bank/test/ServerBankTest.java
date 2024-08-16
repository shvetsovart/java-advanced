package info.kgeorgiy.ja.shvetsov.bank.test;

import java.rmi.RemoteException;

import info.kgeorgiy.ja.shvetsov.bank.account.Account;
import info.kgeorgiy.ja.shvetsov.bank.person.Person;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ServerBankTest")
public class ServerBankTest extends BaseBankTest {
    @DisplayName("Test 01: Creating multiple bank accounts")
    @Test
    public void test01_createAccounts() throws RemoteException {
        for (String id : STRINGS) {
            Account account = bank.createAccount(id);

            Assertions.assertEquals(0, account.getAmount());
            Assertions.assertEquals(id, account.getId());
            Assertions.assertEquals(account, bank.getAccount(id));
        }
    }

    @DisplayName("Test 02: Checking multiple account operations")
    @Test
    public void test02_accountMultipleOperations() throws RemoteException {
        for (int i = 0; i < STRINGS.size(); i++) {
            String id = STRINGS.get(i);

            Account account = bank.getAccount(id);
            if (account == null) {
                account = bank.createAccount(id);
            }

            int money = account.getAmount();
            for (int j = 0; j < 5; j++) {
                int change_amount = INTEGERS.get((i + j) % INTEGERS.size());

                money += change_amount;
                account.setAmount(account.getAmount() + change_amount);

                Assertions.assertEquals(money, account.getAmount());
            }
        }
    }

    @DisplayName("Test 03: Creating multiple persons (remote and local)")
    @Test
    public void test03_createPersons() throws RemoteException {
        for (int i = 0; i < STRINGS.size() / 3; i++) {
            String firstName = STRINGS.get(i * 3);
            String lastName = STRINGS.get(i * 3 + 1);
            String passport = STRINGS.get(i * 3 + 2);

            Person remotePerson = bank.getPerson(passport, true);

            if (remotePerson == null) {
                remotePerson = bank.createPerson(firstName, lastName, passport);
            }
            Person localPerson = bank.getPerson(passport, false);

            Assertions.assertNotNull(remotePerson);
            Assertions.assertNotNull(localPerson);

            Assertions.assertEquals(remotePerson, bank.getPerson(passport, true));
            Assertions.assertEquals(remotePerson.getFirstName(), localPerson.getFirstName());
            Assertions.assertEquals(remotePerson.getLastName(), localPerson.getLastName());
            Assertions.assertEquals(remotePerson.getPassport(), localPerson.getPassport());
        }
    }

    @DisplayName("Test 04: Checking operations a person makes (creating an account and setting amount)")
    @Test
    public void test04_personCreatingAccountAndSettingAmount() throws RemoteException {
        String firstName = STRINGS.get(0);
        String lastName = STRINGS.get(1);
        String passport = STRINGS.get(2);

        Person person = bank.createPerson(firstName, lastName, passport);

        for (int i = 0; i < STRINGS.size(); i++) {
            String subId = STRINGS.get(i);

            Account account = person.createAccount(subId);

            Assertions.assertNotNull(account);
            Assertions.assertEquals(account, person.getAccount(subId));

            int money = INTEGERS.get(i % INTEGERS.size());
            account.setAmount(money);

            Assertions.assertEquals(money, account.getAmount());
        }
    }

    // :NOTE: На самом деле этого теста не хватает, чтобы показать разницу между remote и local person.
    // Нужно проверять, что изменения в remote person отражаются в новосозданных local person и не отражаются в старых,
    // а изменения в local person не отражаются в remote person вообще никогда.
    @DisplayName("Test 05: Checking difference between remote and local account (person)")
    @Test
    public void test05_remoteAndLocalDifference() throws RemoteException {
        String firstName = STRINGS.getFirst();
        String lastName = STRINGS.get(1);
        String passport = STRINGS.get(2);

        Person person = bank.getPerson(passport, true);
        if (person == null) {
            person = bank.createPerson(firstName, lastName, passport);
        }

        String subId = STRINGS.get(7);
        Account account = person.getAccount(subId);
        if (account == null) {
            account = person.createAccount(subId);
        }

        for (int newMoney : INTEGERS) {
            int oldMoney = account.getAmount();
            Person localPerson = bank.getPerson(passport, false);

            account.setAmount(newMoney);
            Account localAccount = localPerson.getAccount(subId);
            Assertions.assertEquals(oldMoney, localAccount.getAmount());
            Assertions.assertNotEquals(account.getAmount(), localAccount.getAmount());

            localAccount.setAmount(newMoney + 100);
            Assertions.assertNotEquals(account.getAmount(), localAccount.getAmount());
        }
    }
}
