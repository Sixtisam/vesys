package bank.ws.server;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class ObjectOutputStreamEncoder implements Encoder.Binary<Object> {

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public ByteBuffer encode(Object object) throws EncodeException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);	// XXX ich frage mich ob man hier nicht noch ein flush/close hätte machen müssen,
            							//     denn das close aus dem try-with kommt erst nach dem Aufruf von ByteBuffer.wrap.
            return ByteBuffer.wrap(baos.toByteArray());
        } catch (Exception e) {
            throw new EncodeException(object, "failed to encode", e);
        }
    }

}