package info.kgeorgiy.ja.shvetsov.bank.test;

import info.kgeorgiy.ja.shvetsov.bank.Server;
import info.kgeorgiy.ja.shvetsov.bank.bank.Bank;
import org.junit.jupiter.api.*;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

// :NOTE: нет каталога lib в твоем репозитории. По идее это вообще не компилируется.
// :NOTE: также на паре НВ просил, чтобы были скрипты для запуска тестов, сервера и клиента: их нет. Если бы были, то ты заметил бы отсутствие lib
public class BaseBankTest {
    private static Registry registry;
    protected static Bank bank;
    private static Server server;

    protected static final List<Integer> INTEGERS = List.of(
            1234, -1234, 12345678, -12345678, 0
    );

    protected static final List<String> STRINGS = List.of(
            "Artyom", "Shvetsov", "SPB5678",
            "Артём", "Швецов", "Валиднопаспортович1234",
            "จอร์จี้", "عبد المنابوفيتش", "บูรุนซู4554ยาน",
            "{870", "#$%^&*()", "(-123+`~",
            "阿爾喬姆", "什韋佐夫", "護2照",
            "."
    );

    @BeforeAll
    public static void start() throws RemoteException {
        server = new Server();
        server.start(Server.DEFAULT_PORT);

        try {
            registry = LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            fail("FAIL: Couldn't create registry.");
        }

        try {
            bank = (Bank) registry.lookup("bank");
        } catch (NotBoundException e) {
            fail("FAIL: couldn't find bank.");
        }
    }

    @AfterAll
    public static void close() {
        server.close();
    }
}
