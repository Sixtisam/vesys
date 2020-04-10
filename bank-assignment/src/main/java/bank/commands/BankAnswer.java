package bank.commands;

import java.io.Serializable;
import java.util.HashSet;

/**
 * Any answer from the server is wrapped inside a BankAnswer class (or a subclass of it)
 * 
 * @author Samuel Keusch
 */
public abstract class BankAnswer<T extends Serializable> implements Serializable {
	private static final long serialVersionUID = 1L;
	private T data;

	public BankAnswer(T data) {
		this.data = data;
	}

	public T getData() {
		return data;
	}

	@SuppressWarnings("unchecked")
	public <E extends T> E dataAs(Class<E> type) {
		return (E) getData();
	}

	/**
	 * Special subclass which represents an exception which was occurred on the server.
	 * @author sixkn
	 *
	 */
	public static class BankExceptionAnswer extends BankAnswer<Exception> {
		private static final long serialVersionUID = 1L;
		public Exception exception;

		public BankExceptionAnswer(Exception exception) {
			super(exception);
		}
	}

	public static class HashSetAnswer<T extends Serializable> extends BankAnswer<HashSet<T>> {
		private static final long serialVersionUID = 1L;

		public HashSetAnswer(HashSet<T> data) {
			super(data);
		}

		@Override
		public HashSet<T> getData() {
			return super.getData();
		}

		@SuppressWarnings("unchecked")
		public <E extends T> HashSet<E> getData(Class<E> type) {
			return (HashSet<E>) getData();
		}

	}

	public static class StringAnswer extends BankAnswer<String> {
		private static final long serialVersionUID = 1L;

		public StringAnswer(String data) {
			super(data);
		}

		@Override
		public String getData() {
			return super.getData();
		}
	}

	public static class DoubleAnswer extends BankAnswer<Double> {

		private static final long serialVersionUID = 1L;

		public DoubleAnswer(Double data) {
			super(data);
		}

		public double primitive() {
			return (double) getData();
		}
	}

	public static class BooleanAnswer extends BankAnswer<Boolean> {

		private static final long serialVersionUID = 1L;

		public BooleanAnswer(Boolean data) {
			super(data);
		}

		public boolean primitive() {
			return (boolean) getData();
		}
	}

	/**
	 * Special answer which represents the result of <code>void</code>-Methods.
	 *
	 */
	public static class OkAnswer extends BankAnswer<Serializable> {

		private static final long serialVersionUID = 1L;

		public OkAnswer() {
			super(null);
		}
	}
}
