package info.kgeorgiy.ja.belousov.rmi;

import java.io.Serializable;
import java.util.Objects;

/**
 * A local {@link Account} implementation, should be used as a snapshot of any online data, can be serialized.
 */
public class LocalAccount implements Account, Serializable {
    private final String id;
    private int amount;

    /**
     * Basic constructor that creates an account with the specified account id and {@code amount} balance
     *
     * @param id     account id
     * @param amount default balance
     */
    public LocalAccount(final String id, final int amount) {
        this.id = id;
        this.amount = amount;
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
        if (!(o instanceof LocalAccount that)) return false;
        return getAmount() == that.getAmount() && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getAmount());
    }
}
