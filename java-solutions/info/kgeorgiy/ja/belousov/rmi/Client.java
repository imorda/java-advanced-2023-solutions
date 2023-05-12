package info.kgeorgiy.ja.belousov.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Objects;

/**
 * Demo class that acts as a client application interacting with RMI bank server
 */
public final class Client {
    private Client() {
    }

    /**
     * Main method for running as a standalone application
     *
     * @param args Required:
     *             - Name of a person
     *             - Surname of a person
     *             - Passport ID of a person
     *             - Account ID of a person
     *             - Amount of money to change
     */
    public static void main(final String... args) {
        if (args.length < 5) {
            System.err.println("Usage: java Client <Name> <Surname> <Id> <AccountId> <BalanceDiff> [port]");
            return;
        }

        final String name = args[0];
        final String surname = args[1];
        final String id = args[2];
        final String subId = args[3];
        int balanceDiff;
        int port = Server.DEFAULT_PORT;
        try {
            Objects.requireNonNull(name);
            Objects.requireNonNull(surname);
            Objects.requireNonNull(id);
            Objects.requireNonNull(subId);

            balanceDiff = Integer.parseInt(args[4]);
            if (args.length >= 6) {
                port = Integer.parseInt(args[5]);
            }
        } catch (NumberFormatException | NullPointerException e) {
            System.err.println("Invalid syntax: " + e.getMessage());
            return;
        }

        final Bank bank;
        try {
            bank = (Bank) Naming.lookup(Server.getHostName(port));
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException ignored) { // URL is const
            return;
        } catch (final RemoteException e) {
            System.err.println("Could not connect to remote bank: " + e.getMessage());
            e.printStackTrace();
            return;
        }


        try {
            Person person = bank.createPerson(id, name, surname);
            if (person == null) {
                System.err.println("Invalid credentials!");
                return;
            }

            Account account = person.createAccount(subId);
            Objects.requireNonNull(account);

            try {
                account.setAmount(account.getAmount() + balanceDiff);

                System.out.println("New balance: " + account.getAmount());
            } catch (IllegalArgumentException e) {
                System.err.println("Unable to update balance: " + e.getMessage());
            }

        } catch (final RemoteException e) {
            System.err.println("Error communicating to the bank: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
