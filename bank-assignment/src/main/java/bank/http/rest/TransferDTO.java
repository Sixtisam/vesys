package bank.http.rest;

public class TransferDTO {
    public double amount;
    public String from;
    public String to;
    
    public TransferDTO() {
        
    }

    public TransferDTO(String from, String to, double amount) {
        this.amount = amount;
        this.from = from;
        this.to = to;
    }

    public double getAmount() {
        return amount;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}