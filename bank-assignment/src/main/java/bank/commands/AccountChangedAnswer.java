package bank.commands;

public class AccountChangedAnswer extends BankAnswer<String> {
    private static final long serialVersionUID = 1L;

    public AccountChangedAnswer(String number) {
        super(number);
    }
}
