package bank.http.rest.server;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import bank.InactiveException;
import bank.OverdrawException;

public class RestServerBank implements bank.Bank {

    private final ConcurrentHashMap<String, RestServerAccount> accounts = new ConcurrentHashMap<>();

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
        RestServerAccount account;
        do {
            account = new RestServerAccount(UUID.randomUUID().toString(), owner);
        } while (accounts.putIfAbsent(account.getNumber(), account) != null);

        return account.getNumber();
    }

    @Override
    public boolean closeAccount(String number) {
        RestServerAccount acc = accounts.get(number);
        if (acc != null) {
            return acc.close();
        } else {
            return false;
        }
    }

    @Override
    public RestServerAccount getAccount(String number) throws IOException {
        return accounts.get(number);
    }

    @Override
    public void transfer(bank.Account from, bank.Account to, double amount)
            throws IOException, InactiveException, OverdrawException {
        from.withdraw(amount);
        try {
            to.deposit(amount);
        } catch (Exception e) {
            from.deposit(amount);
            throw e;
        }
    }

    public static class RestServerAccount implements bank.Account {
        private final String number;
        private final String owner;
        private double balance;
        private boolean active = true;

        RestServerAccount(String number, String owner) {
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

        public synchronized void setBalance(double balance) throws IllegalArgumentException, InactiveException {
            if (balance < 0)
                throw new IllegalArgumentException();
            if (!isActive())
                throw new InactiveException();
            this.balance = balance;
        }

        public synchronized boolean close() {
            if (getBalance() == 0.0 && isActive()) {
                active = false;
                return true;
            }
            return false;
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
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
            RestServerAccount other = (RestServerAccount) obj;
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

        
        public String getETag() {
            return "\"" + hashCode() + "\"";
        }
    }
}