package info.kgeorgiy.ja.belousov.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI-ready interface for an account inside a bank application.
 */
public interface Account extends Remote {
    /**
     * Returns account identifier.
     */
    String getId() throws RemoteException;

    /**
     * Returns amount of money in the account.
     */
    int getAmount() throws RemoteException;

    /**
     * Sets amount of money in the account.
     *
     * @throws IllegalArgumentException when {@code amount < 0}
     */
    void setAmount(int amount) throws RemoteException, IllegalArgumentException;
}
