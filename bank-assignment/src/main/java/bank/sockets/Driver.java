package bank.sockets;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;

import bank.InactiveException;
import bank.OverdrawException;
import bank.shared.BankAnswer;
import bank.shared.BankAnswer.BankExceptionAnswer;
import bank.shared.BankAnswer.BooleanAnswer;
import bank.shared.BankAnswer.DoubleAnswer;
import bank.shared.BankAnswer.HashSetAnswer;
import bank.shared.BankAnswer.OkAnswer;
import bank.shared.BankAnswer.StringAnswer;
import bank.shared.BankCommand;
import bank.shared.BankCommand.BankAccountCloseCommand;
import bank.shared.BankCommand.BankAccountExistsCommand;
import bank.shared.BankCommand.BankAccountGetActiveCommand;
import bank.shared.BankCommand.BankAccountGetBalanceCommand;
import bank.shared.BankCommand.BankAccountGetOwnerCommand;
import bank.shared.BankCommand.BankAccountModifyBalanceCommand;
import bank.shared.BankCommand.BankAccountModifyBalanceCommand.Type;
import bank.shared.BankCommand.BankCreateAccountCommand;
import bank.shared.BankCommand.BankGetAccountNumbersCommand;
import bank.shared.BankCommand.BankTransferCommand;

public class Driver implements bank.BankDriver {
	private Bank bank = null;
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	@Override
	public void connect(String[] args) throws UnknownHostException, IOException {
		bank = new Bank();
		System.out.println("connected...");
		socket = new Socket(args[0], Integer.parseInt(args[1]));
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
	}

	@Override
	public void disconnect() throws IOException {
		bank = null;
		out.close();
		if (!socket.isClosed()) {
			throw new RuntimeException("Not closed");
		}
		System.out.println("disconnected...");
	}

	@Override
	public bank.Bank getBank() {
		return bank;
	}

	@SuppressWarnings("unchecked")
	protected <T extends BankCommand<? extends BankAnswer<? extends Serializable>>, R extends BankAnswer<? extends Serializable>> R remoteCall(
			T cmd, Class<R> resultType) throws IOException, Exception {
		try {
			out.writeObject(cmd);
			Object obj = in.readObject();
			if (obj instanceof BankExceptionAnswer) {
				throw ((BankExceptionAnswer) obj).getData();
			} else if (obj instanceof BankAnswer) {
				return (R) obj;
			} else {
				throw new IOException("unexpected type " + obj.getClass().getSimpleName());
			}
		} catch (ClassNotFoundException e) {
			throw new IOException("class not found", e);
		}

	}

	public class Bank implements bank.Bank {

		@Override
		public bank.Account getAccount(String number) throws IOException {
			try {
				boolean isActive = remoteCall(new BankAccountExistsCommand(number), BooleanAnswer.class).primitive();
				if (isActive) {
					return new RemoteAccount(number);
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

	}

	private class RemoteAccount implements bank.Account {
		private String number;

		private RemoteAccount(String number) {
			this.number = number;
		}

		@Override
		public String getNumber() {
			return number;
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
		public String getOwner() throws IOException {
			try {
				return remoteCall(new BankAccountGetOwnerCommand(number), StringAnswer.class).getData();
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