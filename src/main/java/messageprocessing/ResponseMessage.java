package messageprocessing;

import java.util.Arrays;

public class ResponseMessage extends Message {
    private final RequestMessage request;
    private static final int SOCKS_VERSION_INDEX = 0;
    private static final int RESPONSE_CODE_INDEX = 1;
    public ResponseMessage(RequestMessage request) {
        super(Arrays.copyOf(request.getBytes(), request.getBytes().length));
        this.request = request;
    }

    public byte[] create(boolean isConnected) {
        data[SOCKS_VERSION_INDEX] = SOCKS_5;
        data[RESPONSE_CODE_INDEX] = SUCCEEDED;

        if (!request.isCommand(CONNECT_TCP)) {
            data[RESPONSE_CODE_INDEX] = COMMAND_NOT_SUPPORTED;
        }

        if (!isConnected) {
            data[RESPONSE_CODE_INDEX] = HOST_NOT_AVAILABLE;
        }

        if (request.getAddressType() == IPv6) {
            data[RESPONSE_CODE_INDEX] = ADDRESS_TYPE_NOT_SUPPORTED;
        }

        return data;
    }
}
