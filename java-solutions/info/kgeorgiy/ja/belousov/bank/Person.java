package info.kgeorgiy.ja.belousov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.NoSuchElementException;

/**
 * RMI-ready interface for a person entity inside a bank application
 */
public interface Person extends Remote {
    /**
     * Returns person name.
     */
    String getName() throws RemoteException;

    /**
     * Returns person surname.
     */
    String getSurname() throws RemoteException;

    /**
     * Returns person's id.
     */
    String getId() throws RemoteException;

    /**
     * Returns bank account associated with this person.
     *
     * @param subId account ID
     * @return instance of an {@link Account}
     * @throws NoSuchElementException if no such account exists.
     */
    Account getAccount(String subId) throws RemoteException, NoSuchElementException;

    /**
     * Creates a bank account with the given {@code subId}
     *
     * @param subId account ID
     * @return created or existing account instance.
     */
    Account createAccount(final String subId) throws RemoteException;
}
