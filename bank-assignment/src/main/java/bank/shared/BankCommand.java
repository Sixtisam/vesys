package bank.shared;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;

import bank.Account;
import bank.InactiveException;
import bank.OverdrawException;
import bank.server.ServerBank;
import bank.shared.BankAnswer.BooleanAnswer;
import bank.shared.BankAnswer.DoubleAnswer;
import bank.shared.BankAnswer.HashSetAnswer;
import bank.shared.BankAnswer.OkAnswer;
import bank.shared.BankAnswer.StringAnswer;

/**
 * A command is sent to the server where it's executed.
 *
 * @param <T>
 */
public interface BankCommand<T extends BankAnswer<? extends Serializable>> extends Serializable {

    /*
     * executes this command on the server
     */
    public T execute(ServerBank bank) throws Exception;

    public static class BankGetAccountNumbersCommand implements BankCommand<BankAnswer<HashSet<String>>> {
        private static final long serialVersionUID = 1L;

        @Override
        public BankAnswer<HashSet<String>> execute(ServerBank bank) throws IOException {
            return new HashSetAnswer<String>(new HashSet<>(bank.getAccountNumbers()));
        }
    }

    public static class BankAccountCloseCommand implements BankCommand<BooleanAnswer> {
        private static final long serialVersionUID = 1L;
        private String number;

        public BankAccountCloseCommand(String number) {
            this.number = number;
        }

        @Override
        public BooleanAnswer execute(ServerBank bank) throws Exception {
            return new BooleanAnswer(bank.closeAccount(number));
        }
    }

    public static class BankCreateAccountCommand implements BankCommand<StringAnswer> {
        private static final long serialVersionUID = 1L;
        private final String owner;

        public BankCreateAccountCommand(String owner) {
            this.owner = owner;
        }

        @Override
        public StringAnswer execute(ServerBank bank) throws IOException {
            return new StringAnswer(bank.createAccount(owner));
        }
    }

    public static class BankTransferCommand implements BankCommand<OkAnswer> {
        private static final long serialVersionUID = 1L;
        private final String from;
        private final String to;
        private final double amount;

        public BankTransferCommand(String from, String to, double amount) {
            super();
            this.from = from;
            this.to = to;
            this.amount = amount;
        }

        @Override
        public OkAnswer execute(ServerBank bank) throws InactiveException, OverdrawException, IOException {
            Account fromAcc = bank.getAccount(from);
            Account toAcc = bank.getAccount(to);
            bank.transfer(fromAcc, toAcc, amount);
            return new OkAnswer();
        }
    }

    public static abstract class BankAccountCommand<T extends BankAnswer<? extends Serializable>>
            implements BankCommand<T> {
        private static final long serialVersionUID = 1L;
        protected final String number;

        public BankAccountCommand(String number) {
            this.number = number;
        }

        public String getNumber() {
            return number;
        }

        @Override
        public final T execute(ServerBank bank) throws Exception {
            return execute(bank, bank.getAccount(number));
        }

        protected abstract T execute(ServerBank bank, Account account) throws Exception;
    }

    public static class BankAccountGetBalanceCommand extends BankAccountCommand<DoubleAnswer> {
        private static final long serialVersionUID = 1L;

        public BankAccountGetBalanceCommand(String number) {
            super(number);
        }

        @Override
        public DoubleAnswer execute(ServerBank bank, Account account) throws Exception {
            return new DoubleAnswer(account.getBalance());
        }
    }

    public static class BankAccountGetOwnerCommand extends BankAccountCommand<BankAnswer<String>> {
        private static final long serialVersionUID = 1L;

        public BankAccountGetOwnerCommand(String number) {
            super(number);
        }

        @Override
        protected BankAnswer<String> execute(ServerBank bank, Account account) throws Exception {
            return new StringAnswer(account.getOwner());
        }
    }

    public static class BankAccountGetActiveCommand extends BankAccountCommand<BooleanAnswer> {
        private static final long serialVersionUID = 1L;

        public BankAccountGetActiveCommand(String number) {
            super(number);
        }

        @Override
        protected BooleanAnswer execute(ServerBank bank, Account account) throws Exception {
            return new BooleanAnswer(account != null && account.isActive());
        }
    }

    public static class BankAccountExistsCommand extends BankAccountCommand<BooleanAnswer> {
        private static final long serialVersionUID = 1L;

        public BankAccountExistsCommand(String number) {
            super(number);
        }

        @Override
        public BooleanAnswer execute(ServerBank bank, Account account) throws Exception {
            return new BooleanAnswer(account != null);
        }
    }

    public class BankAccountModifyBalanceCommand extends BankAccountCommand<OkAnswer> {
        private static final long serialVersionUID = 1L;

        public static enum Type {
            WITHDRAW, DEPOSIT
        };

        private final Type type;
        private final double amount;

        public BankAccountModifyBalanceCommand(String number, Type type, double amount) {
            super(number);
            this.type = type;
            this.amount = amount;
        }

        @Override
        protected OkAnswer execute(ServerBank bank, Account account) throws Exception {
            if (type == Type.DEPOSIT) {
                account.deposit(amount);
            } else if (type == Type.WITHDRAW) {
                account.withdraw(amount);
            } else {
                throw new RuntimeException("Unknown bank balance command type" + type.toString());
            }
            return new OkAnswer();
        }
    }
}
