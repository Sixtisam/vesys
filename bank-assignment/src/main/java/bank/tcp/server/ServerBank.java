package bank.tcp.server;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import bank.Account;
import bank.InactiveException;
import bank.OverdrawException;

public class ServerBank implements bank.Bank {
    private final Object ACCOUNTS_LOCK = new Object();
    private final Object CLOSE_LOCK = new Object();
    private final ConcurrentHashMap<String, ServerAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public Set<String> getAccountNumbers() {
        synchronized (ACCOUNTS_LOCK) {
            return accounts.entrySet().stream().filter(e -> e.getValue().isActive()).map(e -> e.getKey())
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public String createAccount(String owner) {
        // ensure account number is unique
        ServerAccount account;
        synchronized (ACCOUNTS_LOCK) {
            do {
                account = new ServerAccount(UUID.randomUUID().toString(), owner);
            } while (accounts.putIfAbsent(account.getNumber(), account) != null);
        }

        return account.getNumber();
    }

    @Override
    public boolean closeAccount(String number) {
        ServerAccount acc;
        synchronized (ACCOUNTS_LOCK) {
            acc = accounts.get(number);
        }
        if (acc != null) {
            synchronized (CLOSE_LOCK) {
                return acc.close();
            }
        } else {
            return false;
        }
    }

    @Override
    public Account getAccount(String number) throws IOException {
        synchronized (ACCOUNTS_LOCK) {
            return accounts.get(number);
        }
    }

    @Override
    public void transfer(bank.Account from, bank.Account to, double amount)
            throws IOException, InactiveException, OverdrawException {
        synchronized (CLOSE_LOCK) {
            from.withdraw(amount);
            try {
                to.deposit(amount);
            } catch (Exception e) {
                from.deposit(amount);
                throw e;
            }
        }
    }

    class ServerAccount implements bank.Account {
        private final String number;
        private final String owner;
        private double balance;
        private boolean active = true;

        ServerAccount(String number, String owner) {
            this.owner = owner;
            this.number = number;
        }

        @Override
        public synchronized double getBalance() {
            return balance;
        }

        @Override
        public String getOwner() {
            return owner;
        }

        @Override
        public String getNumber() {
            return number;
        }

        @Override
        public synchronized boolean isActive() {
            return active;
        }

        @Override
        public synchronized void deposit(double amount) throws InactiveException {
            if (!isActive()) {
                throw new InactiveException();
            }
            if (amount < 0) {
                throw new IllegalArgumentException("not possible to deposit negative");
            }

            balance += amount;
        }

        @Override
        public synchronized void withdraw(double amount) throws InactiveException, OverdrawException {
            if (!isActive()) {
                throw new InactiveException();
            }
            if (amount < 0) {
                throw new IllegalArgumentException("not allowed to withdraw negative");
            }
            if (getBalance() < amount) {
                throw new OverdrawException();
            }
            balance -= amount;
        }

        public synchronized boolean close() {
            if (getBalance() == 0.0 && isActive()) {
                active = false;
                return true;
            }
            return false;
        }

    }
}