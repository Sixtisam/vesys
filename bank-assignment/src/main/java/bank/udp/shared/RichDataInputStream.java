package bank.udp.shared;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * A better DataInputStream which also enables to write Strings and Exceptions
 *
 */
public class RichDataInputStream extends DataInputStream {
	public RichDataInputStream(InputStream in) {
		super(in);
	}

	public final String readString() throws IOException {
		int length = readInt();
		if(length == -1) {
		    return null;
		}
		
		char[] chars = new char[length];
		for (int i = 0; i < length; i++) {
			chars[i] = readChar();
		}

		return String.valueOf(chars);
	}

	public final Exception readException() throws IOException {
		String clazzName = readString();
		String message = readString();
		try {
			return (Exception) Class.forName(clazzName).getDeclaredConstructor(String.class).newInstance(message);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			throw new IOException("Unknown exception type: " + clazzName);
		}
	}
}
