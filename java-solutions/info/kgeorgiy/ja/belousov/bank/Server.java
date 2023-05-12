package info.kgeorgiy.ja.belousov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 * RMI online backend of a bank application (if localhost can be called "online" lol)
 */
public final class Server {
    final static int DEFAULT_PORT = 8080;
    private static final String HOST_NAME = "//localhost:%d/bank";

    static void init(int port) throws RemoteException {
        final Bank bank = new RemoteBank(port);
        try {
            Naming.rebind(getHostName(port), bank);
        } catch (MalformedURLException ignored) {
        } // Hostname is const
    }


    /**
     * Main method for running as a standalone application
     *
     * @param args Optional:
     *             - Port number (default 8080)
     */
    public static void main(final String... args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NullPointerException | NumberFormatException e) {
                System.err.println("Usage: java Server [port]");
                return;
            }
        }

        try {
            LocateRegistry.createRegistry(port);
            init(port);
            System.out.println("Server started");
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static String getHostName(int port) {
        return String.format(HOST_NAME, port);
    }
}
