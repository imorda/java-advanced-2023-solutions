package info.kgeorgiy.ja.belousov.bank;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A local {@link Person} implementation, should be used as a snapshot of any online data, can be serialized.
 */
public class LocalPerson implements Person, Serializable {
    private final String id;
    private final String name;
    private final String surname;
    private final Map<String, LocalAccount> accounts;

    /**
     * Basic constructor that creates a local person snapshot with the specified credentials and accounts snapshot.
     *
     * @param name     name of a person
     * @param surname  surname of a person
     * @param id       passport id of a person
     * @param accounts snapshot map of all accounts associated with the given person at the moment of creation
     */
    public LocalPerson(final String name, final String surname, final String id,
                       final Map<String, LocalAccount> accounts) {
        this.name = name;
        this.surname = surname;
        this.id = id;
        this.accounts = new HashMap<>(accounts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalPerson that)) return false;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getSurname(), that.getSurname()) && Objects.equals(accounts, that.accounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getSurname(), accounts);
    }

    @Override
    public Account createAccount(String subId) {
        final String id = getId() + ":" + subId;
        final LocalAccount account = new LocalAccount(id, 0);
        if (accounts.putIfAbsent(id, account) == null) {
            return account;
        } else {
            return getAccount(id);
        }
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
    public Account getAccount(String subId) throws NoSuchElementException {
        Account result = accounts.get(this.getId() + ":" + subId);
        if (result == null) {
            throw new NoSuchElementException("No account associated with this id");
        }
        return result;
    }

    @Override
    public String getId() {
        return id;
    }
}
