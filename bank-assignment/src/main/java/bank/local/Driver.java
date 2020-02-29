/*
 * Copyright (c) 2020 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package bank.local;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import bank.InactiveException;
import bank.OverdrawException;

public class Driver implements bank.BankDriver {
	private Bank bank = null;

	@Override
	public void connect(String[] args) {
		bank = new Bank();
		System.out.println("connected...");
	}

	@Override
	public void disconnect() {
		bank = null;
		System.out.println("disconnected...");
	}

	@Override
	public bank.Bank getBank() {
		return bank;
	}

	public class Bank implements bank.Bank {

		private final HashMap<String, Account> accounts = new HashMap<>();

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
			Account account;
			do {
				account = new Account(UUID.randomUUID().toString(), owner);
			} while (accounts.putIfAbsent(account.getNumber(), account) != null);

			return account.getNumber();
		}

		@Override
		public boolean closeAccount(String number) {
			Account acc = accounts.get(number);
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

	/**
	 * Account which is thread-safe
	 *
	 */
	static class Account implements bank.Account {
		private String number;
		private String owner;
		private double balance;
		private boolean active = true;

		Account(String number, String owner) {
			this.owner = owner;
			this.number = number;
		}

		@Override
		public double getBalance() {
			return balance;
		}

		@Override
		public String getOwner() {
			return owner;
		}

		@Override
		public String getNumber() {
			return number;
		}

		@Override
		public boolean isActive() {
			return active;
		}

		@Override
		public void deposit(double amount) throws InactiveException {
			if (!isActive()) {
				throw new InactiveException();
			}
			if (amount < 0) {
				throw new IllegalArgumentException("not possible to deposit negative");
			}

			balance += amount;
		}

		@Override
		public void withdraw(double amount) throws InactiveException, OverdrawException {
			if (!isActive()) {
				throw new InactiveException();
			}
			if (amount < 0) {
				throw new IllegalArgumentException("not allowed to withdraw negative");
			}
			if (getBalance() < amount) {
				throw new OverdrawException();
			}
			balance -= amount;
		}

		public boolean close() {
			if (getBalance() == 0.0 && isActive()) {
				active = false;
				return true;
			}
			return false;
		}
	}

}