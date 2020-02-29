package bank.server;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import bank.Account;
import bank.InactiveException;
import bank.OverdrawException;

public class ServerBank implements bank.Bank {

	private final ConcurrentHashMap<String, ServerAccount> accounts = new ConcurrentHashMap<>();

	@Override
	public Set<String> getAccountNumbers() {
		return accounts.entrySet().stream()
				.filter(e -> e.getValue().isActive())
				.map(e -> e.getKey())
				.collect(Collectors.toSet());
	}

	@Override
	public String createAccount(String owner) {
		// ensure account number is unique
		ServerAccount account;
		do {
			account = new ServerAccount(UUID.randomUUID().toString(), owner);
		} while (accounts.putIfAbsent(account.getNumber(), account) != null);

		return account.getNumber();
	}

	@Override
	public boolean closeAccount(String number) {
		ServerAccount acc = accounts.get(number);
		if (acc != null) {
			return acc.close();
		} else {
			return false;
		}
	}

	@Override
	public Account getAccount(String number) throws IOException {
		return accounts.get(number);
	}

	@Override
	public void transfer(bank.Account from, bank.Account to, double amount)
			throws IOException, InactiveException, OverdrawException {
		from.withdraw(amount);
		try {
			to.deposit(amount);
		} catch (Exception e) {
			from.deposit(amount);
			throw e;
		}
	}

}