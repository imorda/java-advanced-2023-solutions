package info.kgeorgiy.ja.belousov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;

/**
 * Server-side (RMI) {@link Account} implementation
 */
public class RemoteAccount extends UnicastRemoteObject implements Account {
    private final String id;
    private int amount;

    /**
     * Basic constructor that exports a newly created object and creates an account
     * with the specified id number and empty balance
     *
     * @param id   an account id number
     * @param port the port for exporting
     * @throws RemoteException if any RMI connection fails
     */
    public RemoteAccount(final String id, final int port) throws RemoteException {
        super(port);
        this.id = id;
        amount = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Negative balance!");
        }
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteAccount that)) return false;
        if (!super.equals(o)) return false;
        return getAmount() == that.getAmount() && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId(), getAmount());
    }
}
