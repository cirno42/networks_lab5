package messageprocessing;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class RequestMessage extends Message {

    private static final int COMMAND_INDEX = 1;
    private static final int RESERVED_ZEROS_INDEX = 2;
    private static final int ADDRESS_TYPE_INDEX = 3;
    private static final int LENGTH_OF_DNS_NAME_INDEX = 4;

    private static final int LENGTH_OF_MESSAGE_WITHOUT_ADDRESS = 4;
    private static final int IPV4_LENGTH = 4;
    private static final int IPV6_LENGTH = 16;
    private static final int PORT_LENGTH = 2;

    public RequestMessage(ByteBuffer buffer) {
        super(new byte[buffer.limit()]);
        buffer.get(this.data);
        if (!isCorrect()) {
            throw new IllegalArgumentException("Request message isn't correct");
        }
    }

    private boolean isCorrect() {
        if (data.length < LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + PORT_LENGTH + 1) {
            return false;
        }

        if (data[RESERVED_ZEROS_INDEX] != 0x00) {
            return false;
        }

        switch (data[ADDRESS_TYPE_INDEX]) {
            case IPv4:
                if (data.length != LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + IPV4_LENGTH + PORT_LENGTH) {
                    return false;
                }
                break;

            case IPv6:
                if (data.length != LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + IPV6_LENGTH + PORT_LENGTH) {
                    return false;
                }
                break;

            case DOMAIN_NAME:
                if (data.length != LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + 1 + data[LENGTH_OF_DNS_NAME_INDEX] + PORT_LENGTH) {
                    return false;
                }
                break;
        }

        return true;
    }

    public static boolean isCorrectSizeOfMessage(ByteBuffer data) {
        if (data.position() < LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + 1) {
            return false;
        }


        switch (data.get(ADDRESS_TYPE_INDEX)) {
            case IPv4:
                if (data.position() != LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + IPV4_LENGTH + PORT_LENGTH) {
                    return false;
                }
                break;

            case IPv6:
                if (data.position() != LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + IPV6_LENGTH + PORT_LENGTH) {
                    return false;
                }
                break;

            case DOMAIN_NAME:
                if (data.position() != LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + 1 + data.get(LENGTH_OF_DNS_NAME_INDEX) + PORT_LENGTH) {
                    return false;
                }
                break;
        }

        return true;
    }

    public boolean isCommand(byte command) {
        return command == data[COMMAND_INDEX];
    }

    public byte getAddressType() {
        if (data.length < ADDRESS_TYPE_INDEX + 1) {
            return 0;
        }
        return data[ADDRESS_TYPE_INDEX];
    }

    public byte[] getDestAddress() {
        if (!isCorrect()) {
            return null;
        }
        switch (this.getAddressType()) {
            case IPv4:
                return Arrays.copyOfRange(data, LENGTH_OF_MESSAGE_WITHOUT_ADDRESS, LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + IPV4_LENGTH);

            case DOMAIN_NAME:
                int length = data[LENGTH_OF_DNS_NAME_INDEX];
                return Arrays.copyOfRange(data, LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + 1, LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + 1 + length);

            case IPv6:
                return Arrays.copyOfRange(data, LENGTH_OF_MESSAGE_WITHOUT_ADDRESS, LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + IPV6_LENGTH);
        }

        return null;
    }

    public short getDestPort() {
        switch (data[ADDRESS_TYPE_INDEX]) {
            case IPv4:
                return ByteBuffer.wrap(data, LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + IPV4_LENGTH, 2).getShort();

            case DOMAIN_NAME:
                int length = data[LENGTH_OF_DNS_NAME_INDEX];
                return ByteBuffer.wrap(data, LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + 1 + length, 2).getShort();

            case IPv6:
                return ByteBuffer.wrap(data, LENGTH_OF_MESSAGE_WITHOUT_ADDRESS + IPV6_LENGTH, 2).getShort();
        }

        return -1;
    }

    public byte[] getBytes() {
        return data;
    }


}
