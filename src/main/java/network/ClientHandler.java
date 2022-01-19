package network;

import lombok.Getter;
import lombok.Setter;
import messageprocessing.GreetingMessage;
import reader.MessageReader;
import messageprocessing.RequestMessage;
import messageprocessing.ResponseMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Handler {
    private static final int BUFFER_SIZE = 4096;
    @Getter
    private final SocketChannel clientChannel;
    private final DNSHandler dns;
    private SocketChannel serverChannel = null;
    private State state = State.GREETING;
    @Getter @Setter
    private ByteBuffer readBuff = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private ByteBuffer writeBuff = null;
    private GreetingMessage hello = null;
    private RequestMessage request = null;

    public ClientHandler(SocketChannel client, DNSHandler dns, Selector selector) throws IOException {
        this.dns = dns;

        clientChannel = client;
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, this);
    }

    @Override
    public void handle(SelectionKey key) {
        try {
            if (!key.isValid()) {
                close();
                key.cancel();
                return;
            }

            if (key.isReadable()) {
                read(key);
            } else if (key.isWritable()) {
                write(key);
            } else if (key.isConnectable() && key.channel() == serverChannel) {
                serverConnect(key);
            }
        } catch (IOException ex) {
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (clientChannel != null) {
            clientChannel.close();
        }

        if (serverChannel != null) {
            serverChannel.close();
        }
    }

    public void read(SelectionKey key) throws IOException {
        if (key.channel() == clientChannel) {
            clientRead(key);
        } else if (key.channel() == serverChannel) {
            serverRead(key);
        }
    }

    public void write(SelectionKey key) throws IOException {
        if (key.channel() == clientChannel) {
            clientWrite(key);
        } else if (key.channel() == serverChannel) {
            serverWrite(key);
        }
    }

    private void clientRead(SelectionKey key) throws IOException {
        switch (state) {
            case GREETING:
                hello = MessageReader.readHelloMessage(this);
                if (hello == null) {
                    return;
                }

                key.interestOps(SelectionKey.OP_WRITE);
                readBuff.clear();
                break;

            case REQUEST:
                request = MessageReader.readRequestMessage(this);
                if (request == null) {
                    return;
                }

                if (!connect()) {
                    serverChannel = null;
                    key.interestOps(SelectionKey.OP_WRITE);
                } else {
                    serverChannel.register(key.selector(), SelectionKey.OP_CONNECT, this);
                    key.interestOps(0);
                }

                readBuff.clear();
                break;

            case MESSAGE:
                if (this.readFrom(clientChannel, readBuff)) {
                    serverChannel.keyFor(key.selector()).interestOpsOr(SelectionKey.OP_WRITE);
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                }

        }
    }

    private void clientWrite(SelectionKey key) throws IOException {
        switch (state) {
            case GREETING:
                if (writeBuff == null) {
                    writeBuff = ByteBuffer.wrap(MessageReader.getResponse());
                }
                if (writeTo(clientChannel, writeBuff)) {
                    writeBuff = null;

                    key.interestOps(SelectionKey.OP_READ);
                    state = State.REQUEST;

                    hello = null;
                }
                break;

            case REQUEST:
                if (writeBuff == null) {
                    ResponseMessage response = new ResponseMessage(request);
                    writeBuff = ByteBuffer.wrap(response.create(serverChannel != null));
                }
                if (writeTo(clientChannel, writeBuff)) {
                    writeBuff = null;

                    if (!request.isCommand(RequestMessage.CONNECT_TCP) || serverChannel == null) {
                        this.close();
                    } else {
                        key.interestOps(SelectionKey.OP_READ);
                        serverChannel.register(key.selector(), SelectionKey.OP_READ, this);
                        state = State.MESSAGE;
                    }

                    request = null;
                }
                break;


            case MESSAGE:
                if (writeTo(clientChannel, readBuff)) {
                    key.interestOps(SelectionKey.OP_READ);
                    serverChannel.keyFor(key.selector()).interestOpsOr(SelectionKey.OP_READ);
                }
        }
    }

    private void serverRead(SelectionKey key) throws IOException {
        if (readFrom(serverChannel, readBuff)) {
            clientChannel.register(key.selector(), SelectionKey.OP_WRITE, this);
            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
        }
    }

    private void serverWrite(SelectionKey key) throws IOException {
        if (writeTo(serverChannel, readBuff)) {
            key.interestOps(SelectionKey.OP_READ);
            clientChannel.register(key.selector(), SelectionKey.OP_READ, this);
        }
    }

    private void serverConnect(SelectionKey key) throws IOException {
        if (!serverChannel.isConnectionPending()) {
            return;
        }

        if (!serverChannel.finishConnect()) {
            return;
        }

        key.interestOps(0);
        clientChannel.register(key.selector(), SelectionKey.OP_WRITE, this);
    }

    public boolean connectToServer(InetAddress address) {
        try {
            serverChannel.connect(new InetSocketAddress(address, request.getDestPort()));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean connect() throws IOException {
        serverChannel = SocketChannel.open();
        serverChannel.configureBlocking(false);

        switch (request.getAddressType()) {
            case RequestMessage.IPv4:
                return connectToServer(InetAddress.getByAddress(request.getDestAddress()));


            case RequestMessage.IPv6:
                System.err.println("NOT SUPPORT IPV6");
                return false;


            case RequestMessage.DOMAIN_NAME:
                dns.sendToResolve(new String(request.getDestAddress(), StandardCharsets.US_ASCII), this);
        }

        return true;
    }

    private boolean readFrom(SocketChannel channel, ByteBuffer buffer) throws IOException {
        buffer.compact();

        int read_bytes = channel.read(buffer);
        if (read_bytes == -1) {
            this.close();
            return false;
        }

        if (read_bytes != 0)
            buffer.flip();

        return read_bytes != 0;
    }

    private boolean writeTo(SocketChannel channel, ByteBuffer buffer) throws IOException {
        channel.write(buffer);
        return !buffer.hasRemaining();
    }

}
