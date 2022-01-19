package reader;

import network.ClientHandler;
import messageprocessing.Message;
import messageprocessing.GreetingMessage;
import messageprocessing.RequestMessage;

import java.io.IOException;

public class MessageReader {
    public static GreetingMessage readHelloMessage(ClientHandler session) throws IOException {
        int read_bytes = session.getClientChannel().read(session.getReadBuff());
        if (read_bytes == -1) {
            session.close();
            return null;
        }

        if (GreetingMessage.isCorrectSizeOfMessage(session.getReadBuff())) {
            session.setReadBuff(session.getReadBuff().flip());
            return new GreetingMessage(session.getReadBuff());
        }

        return null;
    }

    public static RequestMessage readRequestMessage(ClientHandler session) throws IOException {
        int read_bytes = session.getClientChannel().read(session.getReadBuff());
        if (read_bytes == -1) {
            session.close();
            return null;
        }

        if (RequestMessage.isCorrectSizeOfMessage(session.getReadBuff())) {
            session.setReadBuff(session.getReadBuff().flip());
            return new RequestMessage(session.getReadBuff());
        }

        return null;
    }

    public static byte[] getResponse() {
        byte[] data = new byte[2];
        data[0] = Message.SOCKS_5;
        data[1] = Message.NO_AUTHENTICATION;

        return data;
    }
}
