package bank.udp.server;

import java.util.Set;

import bank.Account;
import bank.Bank;
import bank.udp.shared.RichDataInputStream;
import bank.udp.shared.RichDataOutputStream;

/**
 * 
 * @author skeeks
 *
 */
public interface RequestHandler {
    void handle(Bank bank, RichDataInputStream in, RichDataOutputStream out) throws Exception;

    public static class GetAccountNumbersRequestHandler implements RequestHandler {
        @Override
        public void handle(Bank bank, RichDataInputStream in, RichDataOutputStream out) throws Exception {
            Set<String> accountNumbers = bank.getAccountNumbers();
            out.writeInt(accountNumbers.size());
            for (String str : accountNumbers) {
                out.writeString(str);
            }
        }
    }

    public static class CreateAccountRequestHandler implements RequestHandler {
        @Override
        public void handle(Bank bank, RichDataInputStream in, RichDataOutputStream out) throws Exception {
            String accNumber = bank.createAccount(in.readString());
            out.writeString(accNumber);
        }
    }

    /**
     * Thrown by request handler indicating that NULL is returned
     *
     */
    public static class ResultIsNullException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public static class GetAccountOwnerRequestHandler implements RequestHandler {
        @Override
        public void handle(Bank bank, RichDataInputStream in, RichDataOutputStream out) throws Exception {
            Account acc = bank.getAccount(in.readString());
            if (acc != null) {
                out.writeString(acc.getOwner());
            } else {
                throw new ResultIsNullException();
            }
        }
    }

    public static class GetAccountBalanceRequestHandler implements RequestHandler {
        @Override
        public void handle(Bank bank, RichDataInputStream in, RichDataOutputStream out) throws Exception {
            Account acc = bank.getAccount(in.readString());
            if (acc != null) {
                out.writeDouble(acc.getBalance());
            } else {
                throw new ResultIsNullException();
            }
        }
    }

    public static class GetAccountActiveRequestHandler implements RequestHandler {
        @Override
        public void handle(Bank bank, RichDataInputStream in, RichDataOutputStream out) throws Exception {
            Account acc = bank.getAccount(in.readString());
            if (acc != null) {
                out.writeBoolean(acc.isActive());
            } else {
                throw new ResultIsNullException();
            }
        }
    }

    public static class DepositRequestHandler implements RequestHandler {
        @Override
        public void handle(Bank bank, RichDataInputStream in, RichDataOutputStream out) throws Exception {
            Account acc = bank.getAccount(in.readString());
            if (acc == null)
                throw new ResultIsNullException();
            acc.deposit(in.readDouble());
        }
    }

    public static class WithdrawRequestHandler implements RequestHandler {
        @Override
        public void handle(Bank bank, RichDataInputStream in, RichDataOutputStream out) throws Exception {
            Account acc = bank.getAccount(in.readString());
            if (acc == null)
                throw new ResultIsNullException();
            acc.withdraw(in.readDouble());
        }
    }

    public static class CloseAccountRequestHandler implements RequestHandler {
        @Override
        public void handle(Bank bank, RichDataInputStream in, RichDataOutputStream out) throws Exception {
            boolean worked = bank.closeAccount(in.readString());
            out.writeBoolean(worked);
        }
    }

    public static class TransferRequestHandler implements RequestHandler {
        @Override
        public void handle(Bank bank, RichDataInputStream in, RichDataOutputStream out) throws Exception {
            Account from = bank.getAccount(in.readString());
            Account to = bank.getAccount(in.readString());
            double amount = in.readDouble();
            bank.transfer(from, to, amount);
        }
    }
}
