package info.kgeorgiy.ja.belousov.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Server-side (RMI) {@link Bank} implementation
 */
public class RemoteBank extends UnicastRemoteObject implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();

    /**
     * Basic constructor that exports that newly-created object
     *
     * @param port port for exporting
     * @throws RemoteException if any RMI connection fails.
     */
    public RemoteBank(final int port) throws RemoteException {
        super(port);
        this.port = port;
    }

    @Override
    public Account createAccount(final String id) throws RemoteException {
        System.out.println("Creating account " + id);
        final Account account = new RemoteAccount(id, port);
        if (accounts.putIfAbsent(id, account) == null) {
            return account;
        } else {
            System.out.println("Already exists " + id);
            return getAccount(id);
        }
    }

    @Override
    public Account getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }

    @Override
    public Person getPerson(String id) {
        System.out.println("Retrieving person " + id);
        return persons.get(id);
    }

    @Override
    public Person getPerson(String id, String name, String surname) throws RemoteException {
        Person result = getPerson(id);
        if (result != null && result.getName().equals(name) && result.getSurname().equals(surname)) {
            return result;
        }
        return null;
    }

    @Override
    public LocalPerson getPersonSnapshot(String id) throws RemoteException {
        Person person = getPerson(id);
        if (person == null) {
            return null;
        }
        Map<String, LocalAccount> localAccounts = new HashMap<>();
        for (Map.Entry<String, Account> account : accounts.entrySet()) {
            if (account.getKey().startsWith(id + ":")) {
                localAccounts.put(account.getKey(),
                        new LocalAccount(account.getValue().getId(), account.getValue().getAmount()));
            }
        }
        return new LocalPerson(person.getName(), person.getSurname(), person.getId(), localAccounts);
    }

    @Override
    public LocalPerson getPersonSnapshot(String id, String name, String surname) throws RemoteException {
        LocalPerson result = getPersonSnapshot(id);
        if (result != null && result.getName().equals(name) && result.getSurname().equals(surname)) {
            return result;
        }
        return null;
    }

    @Override
    public Person createPerson(String id, String name, String surname) throws RemoteException {
        System.out.println("Creating person " + id);
        final Person person = new RemotePerson(name, surname, id, this, port);
        if (persons.putIfAbsent(id, person) == null) {
            return person;
        } else {
            System.out.println("Already exists " + id);
            return getPerson(id, name, surname);
        }
    }
}
