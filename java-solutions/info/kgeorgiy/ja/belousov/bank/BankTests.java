package info.kgeorgiy.ja.belousov.bank;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runners.MethodSorters;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Unit tests for RMI Bank application
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BankTests {
    private static final int PERSONS_PER_TEST = 20;
    private static final int ACCOUNTS_PER_PERSON = 5;
    private static final int THREADS = 10;

    private static Bank bank;

    /**
     * Initialization for tests, launches server and RMI registry
     *
     * @throws MalformedURLException if the specified port produces invalid URL
     * @throws NotBoundException     if server has failed to bind bank class
     * @throws RemoteException       if any of RMI connections failed
     */
    @BeforeClass
    public static void initialize() throws MalformedURLException, NotBoundException, RemoteException {
        Server.main();
        bank = (Bank) Naming.lookup(Server.getHostName(Server.DEFAULT_PORT));
    }

    /**
     * Main method for running tests as a standalone application
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        JUnitCore junit = new JUnitCore();
        Result result = junit.run(BankTests.class);
        System.exit(result.wasSuccessful() ? 0 : 1);
    }

    private void iterate(BiConsumer<Integer, Integer> func) {
        for (int i = 0; i < PERSONS_PER_TEST; i++) {
            for (int j = 0; j < ACCOUNTS_PER_PERSON; j++) {
                func.accept(i, j);
            }
        }
    }

    private void createPersons(String prefix) {
        try {
            for (int i = 0; i < PERSONS_PER_TEST; i++) {
                String id = prefix + i + "id";
                String name = prefix + i + "name";
                String surname = prefix + i + "surname";
                Assert.assertNull(bank.getPerson(id));
                bank.createPerson(id, name, surname);
            }
        } catch (RemoteException e) {
            Assert.fail();
        }
    }

    private void createAccounts(String prefix) {
        iterate((i, j) -> {
            try {
                String id = prefix + i + "id:" + prefix + j;
                Assert.assertNull(bank.getAccount(id));
                bank.createAccount(id);
            } catch (RemoteException e) {
                Assert.fail();
            }
        });
    }

    private void testAccountMath(String id) {
        try {
            Account account = bank.getAccount(id);
            Assert.assertNotNull(account);
            Assert.assertEquals(account.getId(), id);

            testAccountMath(account);
        } catch (RemoteException e) {
            Assert.fail();
        }
    }

    private void testAccountMath(final Account account) {
        try {
            int expected = 0;
            Assert.assertEquals(account.getAmount(), expected);
            account.setAmount(account.getAmount() + 1000);
            expected += 1000;
            Assert.assertEquals(account.getAmount(), expected);

            account.setAmount(account.getAmount() + 300);
            expected += 300;
            Assert.assertEquals(account.getAmount(), expected);
            account.setAmount(account.getAmount() + 30);
            expected += 30;
            Assert.assertEquals(account.getAmount(), expected);
            account.setAmount(account.getAmount() + 7);
            expected += 7;
            Assert.assertEquals(account.getAmount(), expected);

            account.setAmount(0);
            expected = 0;
            Assert.assertEquals(account.getAmount(), expected);

            try {
                account.setAmount(-1337);
                Assert.fail("Error expected for negative balance");
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(account.getAmount(), expected);
            } catch (final Exception e) {
                Assert.fail("Unexpected exception");
            }
        } catch (RemoteException e) {
            Assert.fail();
        }
    }

    private void parallel(Consumer<Integer> task) {
        try (ExecutorService executorService = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                final int iFinal = i;
                executorService.submit(() -> task.accept(iFinal));
            }
        }
    }

    private void testPersons(String prefix) throws RemoteException {
        for (int i = 0; i < PERSONS_PER_TEST; i++) {
            String id = prefix + i + "id";
            String name = prefix + i + "name";
            String surname = prefix + i + "surname";

            Person person = bank.getPerson(id);

            Assert.assertNotNull(bank.getPerson(id));
            Assert.assertNotNull(bank.getPerson(id, name, surname));
            Assert.assertNull(bank.getPerson(id, surname, name));

            Assert.assertEquals(person.getSurname(), surname);
            Assert.assertEquals(person.getName(), name);
            Assert.assertEquals(person.getId(), id);
        }
    }

    /**
     * Tests {@link Person} creation and get obtaining.
     */
    @Test
    public void test01_createAndGet() throws RemoteException {
        for (int i = 0; i < PERSONS_PER_TEST; i++) {
            String id = "test01" + i + "id";
            String name = "test01" + i + "name";
            String surname = "test01" + i + "surname";
            Assert.assertNull(bank.getPerson(id));
            bank.createPerson(id, name, surname);
        }

        testPersons("test01");
    }

    /**
     * Tests random-named bank accounts logic (not bound to any person)
     */
    @Test
    public void test05_anonAccounts() throws RemoteException {
        for (int i = 0; i < ACCOUNTS_PER_PERSON * PERSONS_PER_TEST; i++) {
            Assert.assertNull(bank.getAccount("test05" + i));
        }
        for (int i = 0; i < ACCOUNTS_PER_PERSON * PERSONS_PER_TEST; i++) {
            bank.createAccount("test05" + i);
        }
        for (int i = 0; i < ACCOUNTS_PER_PERSON * PERSONS_PER_TEST; i++) {
            testAccountMath("test05" + i);
        }
    }

    /**
     * Tests random bank accounts (bound to random persons) logic.
     */
    @Test
    public void test10_namedAccounts() {
        createPersons("test10");
        createAccounts("test10");
        iterate((i, j) -> testAccountMath("test10" + i + "id:test10" + j));
    }

    /**
     * Parallel tests for random bank accounts (bound to random persons) logic.
     */
    @Test
    public void test15_accountsParallel() throws RemoteException {
        parallel((x) -> createPersons(x + "test15"));
        for (int i = 0; i < THREADS; i++) {
            testPersons(i + "test15");
        }

        parallel((x) -> createAccounts(x + "test15"));

        parallel((x) -> iterate((i, j) -> testAccountMath(x + "test15" + i + "id:" + x + "test15" + j)));
    }

    /**
     * Tests for local and remote {@link Person} and {@link Account} implementation logic.
     * Local implementation should store a snapshot of accounts state at the moment of its creation,
     * not using network after that and keeping its state independently of the remote state changes.
     * And vice versa.
     */
    @Test
    public void test20_localRemoteConsistency() throws RemoteException {
        createPersons("test20");
        createAccounts("test20");


        List<ArrayList<Integer>> expectedLocal = new ArrayList<>();
        List<ArrayList<Integer>> expectedRemote = new ArrayList<>();

        for (int i = 0; i < PERSONS_PER_TEST; i++) {
            expectedLocal.add(new ArrayList<>());
            expectedRemote.add(new ArrayList<>());
        }

        iterate((i, j) -> {
            try {
                Account account = bank.getPerson("test20" + i + "id").getAccount("test20" + j);
                account.setAmount(i * j);
                expectedLocal.get(i).add(i * j);
                expectedRemote.get(i).add(i * j);
            } catch (RemoteException e) {
                Assert.fail();
            }
        });

        List<LocalPerson> localPersons = new ArrayList<>();

        for (int i = 0; i < PERSONS_PER_TEST; i++) {
            String id = "test20" + i + "id";
            String name = "test20" + i + "name";
            String surname = "test20" + i + "surname";

            LocalPerson snapshot = bank.getPersonSnapshot(id);
            localPersons.add(snapshot);
            Assert.assertNotNull(snapshot);

            Assert.assertEquals(snapshot, bank.getPersonSnapshot(id, name, surname));
            Assert.assertNull(bank.getPersonSnapshot(id, surname, name));
        }

        iterate((i, j) -> {
            try {
                Account account = bank.getPerson("test20" + i + "id").getAccount("test20" + j);
                account.setAmount(i + j);
                expectedRemote.get(i).set(j, i + j);
            } catch (RemoteException e) {
                Assert.fail();
            }
        });

        iterate((i, j) -> Assert.assertEquals((int) expectedLocal.get(i).get(j),
                ((LocalAccount) localPersons.get(i).getAccount("test20" + j)).getAmount()));


        iterate((i, j) -> {
            LocalAccount account = (LocalAccount) localPersons.get(i).getAccount("test20" + j);
            account.setAmount((i + j) + (i * j));
            expectedLocal.get(i).set(j, (i + j) + (i * j));
        });

        iterate((i, j) -> Assert.assertEquals((int) expectedLocal.get(i).get(j),
                ((LocalAccount) localPersons.get(i).getAccount("test20" + j)).getAmount()));

        iterate((i, j) -> {
            try {
                Assert.assertEquals((int) expectedRemote.get(i).get(j),
                        bank.getPerson("test20" + i + "id").getAccount("test20" + j).getAmount());
            } catch (RemoteException e) {
                Assert.fail();
            }
        });
    }

    /**
     * Parallel tests for local {@link Account} implementation
     */
    @Test
    public void test25_localParallel() {
        parallel((x) -> createPersons(x + "test25"));
        parallel((x) -> createAccounts(x + "test25"));

        parallel((x) -> iterate((i, j) -> {
            try {
                testAccountMath(bank.getPersonSnapshot(x + "test25" + i + "id").getAccount(x + "test25" + j));
            } catch (RemoteException e) {
                Assert.fail();
            }
        }));
    }
}