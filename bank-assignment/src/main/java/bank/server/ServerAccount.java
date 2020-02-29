package bank.server;

import bank.InactiveException;
import bank.OverdrawException;

/**
 * Account which is thread-safe
 *
 */
class ServerAccount implements bank.Account {
	private String number;
	private String owner;
	private double balance;
	private boolean active = true;

	ServerAccount(String number, String owner) {
		this.owner = owner;
		this.number = number;
	}

	@Override
	public synchronized double getBalance() {
		return balance;
	}

	@Override
	public synchronized String getOwner() {
		return owner;
	}

	@Override
	public synchronized String getNumber() {
		return number;
	}

	@Override
	public synchronized boolean isActive() {
		return active;
	}

	@Override
	public synchronized void deposit(double amount) throws InactiveException {
		if (!isActive()) {
			throw new InactiveException();
		}
		if (amount < 0) {
			throw new IllegalArgumentException("not possible to deposit negative");
		}

		balance += amount;
	}

	@Override
	public synchronized void withdraw(double amount) throws InactiveException, OverdrawException {
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

	public synchronized boolean close() {
		if (getBalance() == 0.0 && isActive()) {
			active = false;
			return true;
		}
		return false;
	}

}