package info.kgeorgiy.ja.belousov.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * An RMI-ready interface for a bank application
 */
public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it does not already exist.
     *
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String id) throws RemoteException;

    /**
     * Returns account by identifier.
     *
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    Account getAccount(String id) throws RemoteException;

    /**
     * Returns person by identifier.
     *
     * @param id person passport id
     * @return person with specified id or {@code null} if it does not exist.
     */
    Person getPerson(String id) throws RemoteException;

    /**
     * Returns person by identifier and validates given credentials.
     *
     * @param id person passport id
     * @return person with specified id or {@code null} if it does not exist or credentials are invalid.
     */
    Person getPerson(String id, String name, String surname) throws RemoteException;

    /**
     * Returns person local snapshot by identifier.
     *
     * @param id person passport id
     * @return person snapshot with specified id or {@code null} if it does not exist.
     */
    LocalPerson getPersonSnapshot(String id) throws RemoteException;

    /**
     * Returns person local snapshot by identifier and validates given credentials.
     *
     * @param id person passport id
     * @return person snapshot with specified id or {@code null} if it does not exist or credentials are invalid.
     */
    LocalPerson getPersonSnapshot(String id, String name, String surname) throws RemoteException;

    /**
     * Creates a new person with specified identifier if it does not already exist.
     *
     * @param id      person passport id
     * @param name    person name
     * @param surname person surname
     * @return created or existing person.
     */
    Person createPerson(String id, String name, String surname) throws RemoteException;
}
