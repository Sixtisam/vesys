package bank.udp.shared;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public interface RequestResponseDescriptor<T> {
	MessageType messageType();

	/**
	 * Write your request data to the output stream. Do not include your
	 * {@link MessageType}
	 * 
	 * @return
	 */
	default void buildRequest(RichDataOutputStream out) throws IOException {
	}

	/**
	 * read your requested data from the input stream. There is no message type
	 * field at the begin
	 */
	T processResponse(RichDataInputStream in) throws IOException;

	/**
	 * *******************************************************************************************
	 * *******************************************************************************************
	 * *******************************************************************************************
	 */

	public static class GetAccountNumbersDescriptor implements RequestResponseDescriptor<Set<String>> {
		@Override
		public MessageType messageType() {
			return MessageType.GET_ACCOUNT_NUMBERS;
		}

		@Override
		public Set<String> processResponse(RichDataInputStream in) throws IOException {
			int nrOfAccounts = in.readInt();
			HashSet<String> accountNumbers = new HashSet<>();
			for (int i = 0; i < nrOfAccounts; i++) {
				accountNumbers.add(in.readString());
			}
			return accountNumbers;
		}
	}

	public static class CreateAccountDescriptor implements RequestResponseDescriptor<String> {
		private String owner;

		public CreateAccountDescriptor(String owner) {
			this.owner = owner;
		}

		@Override
		public MessageType messageType() {
			return MessageType.CREATE_ACCOUNT;
		}

		@Override
		public void buildRequest(RichDataOutputStream out) throws IOException {
			out.writeString(owner);
		}

		@Override
		public String processResponse(RichDataInputStream in) throws IOException {
			return in.readString();
		}
	}

	public static class GetAccountOwnerDescriptor implements RequestResponseDescriptor<String> {
		private String number;

		public GetAccountOwnerDescriptor(String number) {
			this.number = number;
		}

		@Override
		public MessageType messageType() {
			return MessageType.GET_OWNER;
		}

		@Override
		public void buildRequest(RichDataOutputStream out) throws IOException {
			out.writeString(number);
		}

		@Override
		public String processResponse(RichDataInputStream in) throws IOException {
			return in.readString();
		}
	}

	public static class GetAccountBalanceDescriptor implements RequestResponseDescriptor<Double> {
		private String number;

		public GetAccountBalanceDescriptor(String number) {
			this.number = number;
		}

		@Override
		public MessageType messageType() {
			return MessageType.GET_BALANCE;
		}

		@Override
		public void buildRequest(RichDataOutputStream out) throws IOException {
			out.writeString(number);
		}

		@Override
		public Double processResponse(RichDataInputStream in) throws IOException {
			return in.readDouble();
		}
	}
}
