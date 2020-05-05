package bank.graphql.server;

import java.io.IOException;

import bank.InactiveException;
import bank.OverdrawException;
import graphql.kickstart.tools.GraphQLMutationResolver;

public class MutationResolver implements GraphQLMutationResolver {

    private final GraphQLBank bank;

    public MutationResolver(GraphQLBank bank) {
        this.bank = bank;
    }

    public String createAccount(String owner) {
        return bank.createAccount(owner);
    }

    public boolean closeAccount(String number) {
        return bank.closeAccount(number);
    }

    public void transfer(String from, String to, double amount) throws IOException, InactiveException, OverdrawException, IllegalArgumentException {
        try {
            bank.transfer(bank.getAccount(from), bank.getAccount(to), amount);

        } catch (OverdrawException | InactiveException | IllegalArgumentException e) {
        	// XXX hier könnte auch eine NPE geworfen werden (falls zur Kontonummer kein Konto existiert).
        	//     => ich würde direkt Exception (oder Trowable) abfangen.
            throw new ApplicationException(e);
        }
    }

    public void deposit(String number, double amount) throws InactiveException, IOException, IllegalArgumentException {
        try {
            bank.getAccount(number).deposit(amount);
        } catch (InactiveException | IllegalArgumentException e) {
            throw new ApplicationException(e);
        }
    }

    public void withdraw(String number, double amount) throws InactiveException, OverdrawException, IOException, IllegalArgumentException {
        try {
            bank.getAccount(number).withdraw(amount);
        } catch (OverdrawException | InactiveException | IllegalArgumentException e) {
            throw new ApplicationException(e);
        }
    }
}
