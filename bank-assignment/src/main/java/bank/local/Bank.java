package bank.local;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import bank.BankDriver2.UpdateHandler;
import bank.InactiveException;
import bank.OverdrawException;

public class Bank implements bank.Bank {
    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final List<UpdateHandler> updateHandlers = new ArrayList<>();	// XXX hier sollte vielleicht auch etwas threadsicheres verwendet werden, denn die registerUpdateHandler könnten ja auch aus mehreren Threads erfolgen (nicht in der WebSocket lösung)

    public void registerUpdateHandler(UpdateHandler handler) {
        updateHandlers.add(handler);
    }

    protected void notify(String number) {
        updateHandlers.forEach(uH -> {
            try {
                uH.accountChanged(number);
            } catch (IOException e) {
                System.err.println("UpdateHandler throwed exception " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public Set<String> getAccountNumbers() {
        return accounts.entrySet().stream()
                .filter(e -> e.getValue().isActive())
                .map(e -> e.getKey())
                .collect(Collectors.toSet());
    }

    @Override
    public String createAccount(String owner) {
        // ensure account number is unique
        Account account;
        do {
            account = new Account(UUID.randomUUID().toString(), owner);
        } while (accounts.putIfAbsent(account.getNumber(), account) != null);

        notify(account.getNumber());
        return account.getNumber();
    }

    @Override
    public boolean closeAccount(String number) {
        Account acc = accounts.get(number);
        if (acc != null) {
            notify(number);	// XXX Notifikation eigentlich nur wenn das folgende acc.close() true zurückgibt.
            return acc.close();
        } else {
            return false;
        }
    }

    @Override
    public Account getAccount(String number) throws IOException {
        return accounts.get(number);
    }

    @Override
    public void transfer(bank.Account from, bank.Account to, double amount)
            throws IOException, InactiveException, OverdrawException {

        bank.Account first, second;
        // in order to prevent a deadlock, a "global order" must be established
        if (from.getNumber().compareTo(to.getNumber()) == -1) {
            first = from;
            second = to;
        } else {
            first = to;
            second = from;
        }

        synchronized (first) {
            synchronized (second) {
                from.withdraw(amount);
                try {
                    to.deposit(amount);
                    notify(from.getNumber());
                    notify(to.getNumber());
                } catch (Exception e) {
                    from.deposit(amount);
                    throw e;
                }
            }
        }
    }

    /**
     * Account which is thread-safe
     *
     */
    public class Account implements bank.Account {
        private final String number;
        private final String owner;
        private double balance;
        private boolean active = true;

        Account(String number, String owner) {
            this.owner = owner;
            this.number = number;
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
        public synchronized double getBalance() {
            return balance;
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
            Bank.this.notify(number);
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
            Bank.this.notify(number);
        }

        public synchronized boolean close() {
            if (getBalance() == 0.0 && isActive()) {
                active = false;
                Bank.this.notify(number);
                return true;
            }
            return false;
        }

        // XXX Muss equals und hashCode überhaupt überschrieben werde? Ich meinte nein.
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getEnclosingInstance().hashCode();
            result = prime * result + (active ? 1231 : 1237);
            long temp;
            temp = Double.doubleToLongBits(balance);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + ((number == null) ? 0 : number.hashCode());
            result = prime * result + ((owner == null) ? 0 : owner.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Account other = (Account) obj;
            if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
                return false;
            if (active != other.active)
                return false;
            if (Double.doubleToLongBits(balance) != Double.doubleToLongBits(other.balance))
                return false;
            if (number == null) {
                if (other.number != null)
                    return false;
            } else if (!number.equals(other.number))
                return false;
            if (owner == null) {
                if (other.owner != null)
                    return false;
            } else if (!owner.equals(other.owner))
                return false;
            return true;
        }

        private Bank getEnclosingInstance() {
            return Bank.this;
        }
    }
}
