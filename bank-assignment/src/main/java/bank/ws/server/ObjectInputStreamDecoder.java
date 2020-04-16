package bank.ws.server;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class ObjectInputStreamDecoder implements Decoder.Binary<Object> {
	// XXX als Variante gäbe es auch das Decoder.BinaryStream Interface.
	// XXX Ich wollte eben noch vorschlagen, dass Sie auch Decoder.Binary<BankCommand> implementieren könnten, aber dann können Sie diesen
	//     Decoder nur auf Serverseite verwenden, den auf Klientenseite erwarten Sie ja BankAnswer Objekte.

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public Object decode(ByteBuffer bytes) throws DecodeException {
        try (ByteArrayInputStream bbis = new ByteArrayInputStream(bytes.array());
                ObjectInputStream ois = new ObjectInputStream(bbis)) {
            return ois.readObject();
        } catch (Exception e) {
            throw new DecodeException(bytes, "failed to decode", e);
        }
    }

    @Override
    public boolean willDecode(ByteBuffer bytes) {
        return true;
    }
}