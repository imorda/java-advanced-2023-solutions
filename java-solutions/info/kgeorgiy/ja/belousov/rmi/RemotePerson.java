package info.kgeorgiy.ja.belousov.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Server-side (RMI) {@link Person} implementation.
 */
public class RemotePerson extends UnicastRemoteObject implements Person {
    private final String id;
    private final String name;
    private final String surname;
    private final RemoteBank bank;

    /**
     * Basic constructor that exports a newly-created {@link Person} object with
     * the specified name, surname, id, associated bank.
     *
     * @param name    name of a person
     * @param surname surname of a person
     * @param id      passport id of a person
     * @param bank    associated bank of a person record
     * @param port    port for exporting
     * @throws RemoteException if any RMI connection fails
     */
    public RemotePerson(final String name, final String surname,
                        final String id, final RemoteBank bank, final int port) throws RemoteException {
        super(port);
        this.name = name;
        this.surname = surname;
        this.id = id;
        this.bank = bank;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemotePerson that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getSurname(), that.getSurname()) && Objects.equals(bank, that.bank);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId(), getName(), getSurname(), bank);
    }

    @Override
    public Account getAccount(String subId) throws NoSuchElementException {
        Account result = bank.getAccount(this.getId() + ":" + subId);
        if (result == null) {
            throw new NoSuchElementException("No account associated with this id");
        }
        return result;
    }

    @Override
    public Account createAccount(String subId) throws RemoteException {
        return bank.createAccount(getId() + ":" + subId);
    }

    @Override
    public String getId() {
        return id;
    }
}
