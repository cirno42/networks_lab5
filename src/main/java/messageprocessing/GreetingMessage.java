package messageprocessing;

import java.nio.ByteBuffer;

public class GreetingMessage extends Message {
    private static final int COUNT_OF_AUTH_METHODS_INDEX = 1;
    private static final int MIN_LENGTH_OF_MESSAGE = 2;
    public GreetingMessage(ByteBuffer buffer) {

        super(new byte[buffer.limit()]);
        buffer.get(data);
        if ((data.length < MIN_LENGTH_OF_MESSAGE) || (data[COUNT_OF_AUTH_METHODS_INDEX] + MIN_LENGTH_OF_MESSAGE != data.length)) {
            throw new IllegalArgumentException("Greeting message isn't correct");
        }
    }

    public static boolean isCorrectSizeOfMessage(ByteBuffer data) {
        return data.position() >= MIN_LENGTH_OF_MESSAGE
                && data.position() >= MIN_LENGTH_OF_MESSAGE + data.get(COUNT_OF_AUTH_METHODS_INDEX);
    }

}
