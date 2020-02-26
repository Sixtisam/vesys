/*
 * Copyright (c) 2020 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package bank.local;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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

	public static class Bank implements bank.Bank {

		private final Map<String, Account> accounts = new HashMap<>();

		@Override
		public Set<String> getAccountNumbers() {
			return accounts.keySet();
		}

		@Override
		public String createAccount(String owner) {
			String accNr;
			while (accounts.containsKey(accNr = UUID.randomUUID().toString())) {
				// nop
			}
			accounts.put(accNr, new Account(accNr, owner));
			return accNr;
		}

		@Override
		public boolean closeAccount(String number) {
			Account acc = accounts.get(number);
			Objects.requireNonNull(acc);

			if (acc.getBalance() == 0.0) {
				acc.active = false;
				return true;
			}
			return false;
		}

		@Override
		public bank.Account getAccount(String number) {
			return accounts.get(number);
		}

		@Override
		public void transfer(bank.Account from, bank.Account to, double amount)
				throws IOException, InactiveException, OverdrawException {
			from.withdraw(amount);
			to.deposit(amount);
		}

	}

	private static class Account implements bank.Account {
		private String number;
		private String owner;
		private double balance;
		private boolean active = true;

		private Account(String number, String owner) {
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
			balance += amount;
		}

		@Override
		public void withdraw(double amount) throws InactiveException, OverdrawException {
			if (!isActive()) {
				throw new InactiveException();
			}
			if (getBalance() < amount) {
				throw new OverdrawException();
			}
			balance -= amount;
		}

	}

}