package bank.udp.shared;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A better DataOutputStream which also enables to write Strings and Exceptions
 *
 */
public class RichDataOutputStream extends DataOutputStream {
    public RichDataOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * writeUTF does not handle null
     */
    public final void writeString(String str) throws IOException {
        if(str == null) {
            writeInt(-1);
            return;
        }
        if (str.length() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("string too long");
        }
        writeInt(str.length());
        for (int i = 0; i < str.length(); i++) {
            writeChar(str.charAt(i));
        }
    }

    public final void writeException(Exception e) throws IOException {
        writeString(e.getClass().getName());
        writeString(e.getMessage());
    }
}
