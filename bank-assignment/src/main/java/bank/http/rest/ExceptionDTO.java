package bank.http.rest;

public class ExceptionDTO {
    public String name;
    public String message;
    
    public ExceptionDTO() {
    }

    public ExceptionDTO(Exception e) {
        this.name = e.getClass().getName();
        this.message = e.getMessage();
    }
}