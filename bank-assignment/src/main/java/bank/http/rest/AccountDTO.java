package bank.http.rest;

public class AccountDTO {
    public String number;
    public String owner;
    public double balance;
    public boolean active;

    public AccountDTO() {

    }

    public AccountDTO(String owner) {
        super();
        this.owner = owner;
    }

    public AccountDTO(String number, String owner, double balance, boolean active) {
        super();
        this.number = number;
        this.owner = owner;
        this.balance = balance;
        this.active = active;
    }

    public String getNumber() {
        return number;
    }

    public String getOwner() {
        return owner;
    }

    public double getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }
}
