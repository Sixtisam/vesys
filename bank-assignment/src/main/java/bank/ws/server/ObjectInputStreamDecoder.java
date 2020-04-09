package bank.ws.server;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import bank.tcp.shared.BankCommand;

public class ObjectInputStreamDecoder implements Decoder.Binary<BankCommand<?>> {

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public BankCommand<?> decode(ByteBuffer bytes) throws DecodeException {
        try (ByteArrayInputStream bbis = new ByteArrayInputStream(bytes.array());
                ObjectInputStream ois = new ObjectInputStream(bbis)) {
            return (BankCommand<?>) ois.readObject();
        } catch (Exception e) {
            throw new DecodeException(bytes, "failed to decode", e);
        }
    }

    @Override
    public boolean willDecode(ByteBuffer bytes) {
        return true;
    }
}