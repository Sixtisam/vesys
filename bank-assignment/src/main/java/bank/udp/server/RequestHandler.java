package bank.udp.server;

import java.util.Set;

import bank.Account;
import bank.Bank;
import bank.udp.shared.RichDataInputStream;
import bank.udp.shared.RichDataOutputStream;

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
}
