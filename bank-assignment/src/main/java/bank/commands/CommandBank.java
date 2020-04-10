package bank.commands;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import bank.Bank;
import bank.InactiveException;
import bank.OverdrawException;
import bank.commands.BankAnswer.BooleanAnswer;
import bank.commands.BankAnswer.DoubleAnswer;
import bank.commands.BankAnswer.HashSetAnswer;
import bank.commands.BankAnswer.OkAnswer;
import bank.commands.BankAnswer.StringAnswer;
import bank.commands.BankCommand.BankAccountCloseCommand;
import bank.commands.BankCommand.BankAccountExistsCommand;
import bank.commands.BankCommand.BankAccountGetActiveCommand;
import bank.commands.BankCommand.BankAccountGetBalanceCommand;
import bank.commands.BankCommand.BankAccountGetOwnerCommand;
import bank.commands.BankCommand.BankAccountModifyBalanceCommand;
import bank.commands.BankCommand.BankCreateAccountCommand;
import bank.commands.BankCommand.BankGetAccountNumbersCommand;
import bank.commands.BankCommand.BankTransferCommand;
import bank.commands.BankCommand.BankAccountModifyBalanceCommand.Type;

/**
 * A bank that can be used as a proxy. Each bank method will result in a call of
 * {@link #remoteCall(BankCommand, Class)}.
 * 
 * Subclasses should override {@link #remoteCall(BankCommand, Class)} to use a
 * custom transport protocol.
 *
 */
public abstract class CommandBank implements Bank {

    protected abstract <T extends BankCommand<? extends BankAnswer<? extends Serializable>>, R extends BankAnswer<? extends Serializable>> R remoteCall(
            T command, Class<R> answer) throws Exception;

    @Override
    public bank.Account getAccount(String number) throws IOException {
        try {
            boolean isActive = remoteCall(new BankAccountExistsCommand(number), BooleanAnswer.class).primitive();
            if (isActive) {
                return new RemoteAccount(number, remoteCall(new BankAccountGetOwnerCommand(number), StringAnswer.class).getData());
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getAccountNumbers() throws IOException {
        try {
            return (Set<String>) remoteCall(new BankGetAccountNumbersCommand(), HashSetAnswer.class).getData();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public String createAccount(String owner) throws IOException {
        try {
            return remoteCall(new BankCreateAccountCommand(owner), StringAnswer.class).getData();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean closeAccount(String number) throws IOException {
        try {
            return remoteCall(new BankAccountCloseCommand(number), BooleanAnswer.class).primitive();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void transfer(bank.Account from, bank.Account to, double amount)
            throws IOException, OverdrawException, InactiveException {
        try {
            remoteCall(new BankTransferCommand(from.getNumber(), to.getNumber(), amount), OkAnswer.class);
        } catch (InactiveException | OverdrawException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private class RemoteAccount implements bank.Account {
        private final String number;
        private final String owner;

        private RemoteAccount(String number, String owner) {
            this.number = number;
            this.owner = owner;
        }

        @Override
        public String getNumber() {
            return number;
        }
        
        @Override
        public String getOwner() {
            return owner;
        }

        @Override
        public double getBalance() throws IOException {
            try {
                return remoteCall(new BankAccountGetBalanceCommand(number), DoubleAnswer.class).primitive();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }


        @Override
        public boolean isActive() throws IOException {
            try {
                return remoteCall(new BankAccountGetActiveCommand(number), BooleanAnswer.class).primitive();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void deposit(double amount) throws InactiveException, IOException {
            try {
                remoteCall(new BankAccountModifyBalanceCommand(number, Type.DEPOSIT, amount), OkAnswer.class);
            } catch (InactiveException | IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void withdraw(double amount) throws InactiveException, OverdrawException, IOException {
            try {
                remoteCall(new BankAccountModifyBalanceCommand(number, Type.WITHDRAW, amount), OkAnswer.class);
            } catch (InactiveException | IllegalArgumentException | OverdrawException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }
}
