package bank.http.rest;

public class BalanceDTO {
    public double balance;
    public BalanceDTO() {
        
    }
    public BalanceDTO(double balance) {
        this.balance = balance;
    }
    
    public double getBalance() {
        return balance;
    }
}