package org.kucro3.krossnat.client.payload;

import com.theredpixelteam.redtea.util.Pair;
import org.kucro3.krossnat.auth.Authorization;
import org.kucro3.krossnat.auth.AuthorizationToken;
import org.kucro3.krossnat.payload.Payload;
import org.kucro3.krossnat.protocol.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class ClientPayload implements Payload {
    public ClientPayload(Logger logger, AuthorizationToken token, PortTable portTable, ConnectionTable connectionTable)
    {
        this.logger = logger;
        this.token = token;
        this.state = State.ZERO;

        this.portTable = portTable;
        this.connectionTable = connectionTable;
    }

    @Override
    public void handleConnect(SelectionKey key)
    {
    }

    @Override
    public void handleAccept(SelectionKey key)
    {
        // unused
    }

    @Override
    public void handleRead(SelectionKey key)
    {
        SocketChannel channel = (SocketChannel) key.channel();

        if (!channel.isOpen())
            return;

        try {
            ByteBuffer buf = ByteBuffer.allocate(1024);

            int cap;
            while ((cap = channel.read(buf)) > 0)
            {
                buf.flip();

                ByteBuffer mem = ByteBuffer.allocateDirect(cap);
                mem.put(buf);
                mem.flip();

                iter.write(mem);

                buf.clear();
            }

            try {
                while (iter.hasNext())
                {
                    Packet packet = iter.next();

                    try {
                        switch (state)
                        {
                            case WAITING_AKEY_AUTH_RESULT:
                                handleReadAKeyAuthResult(key, (Packet2ClientAuthResult) packet);
                                break;

                            case WAITING_BKEY:
                                handleReadBKey(key, (Packet2ClientBKey) packet);
                                break;

                            case WAITING_BKEY_AUTH_RESULT:
                                handleReadBKeyAuthResult(key, (Packet2ClientAuthResult) packet);
                                break;

                            case ONLINE:
                                handleReadOnline(key, packet);
                                break;
                        }
                    } catch (Exception e) {
                        logger.severe("Exception occurred ticking.");
                        e.printStackTrace();
                    }
                }

                if (cap < 0)
                    channel.close();
            } catch (Exception e) {
                logger.severe("Failed to parse packet");
                e.printStackTrace();

                channel.close();
                return;
            }
        } catch (Exception e) {
            logger.severe("Exception occurred while reading packet. State: " + state);
            e.printStackTrace();

            try {
                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void handleReadAKeyAuthResult(SelectionKey key, Packet2ClientAuthResult packet) throws IOException
    {
        boolean result = packet.getResult();

        if (!result)
        {
            logger.info("Auth failure, cannot connect to NAT server.");
            key.channel().close();

            state = State.AKEY_AUTH_FAILURE;
        }
        else {
            logger.info("Auth success, verifying B key mask.");

            state = State.WAITING_BKEY;
        }
    }

    private void handleReadBKey(SelectionKey key, Packet2ClientBKey packet) throws IOException
    {
        UUID[] receivedBKeys = packet.getBKeys();
        long mask = Authorization.computeMask(token, receivedBKeys);

        queue.pushPacket(
                ByteBuffer.wrap(new Packet2ServerBKeyMaskVerification(mask).toBytes()));

        state = State.WAITING_BKEY_AUTH_RESULT;
    }

    private void handleReadBKeyAuthResult(SelectionKey key, Packet2ClientAuthResult packet) throws IOException
    {
        boolean result = packet.getResult();

        if (!result)
        {
            logger.info("Auth failure, corrupt B key mask.");
            key.channel().close();

            state = State.BKEY_AUTH_FAILURE;
        }
        else
        {
            logger.info("Auth success, connected to server.");
            token.clean();

            state = State.ONLINE;
        }
    }

    @Override
    public void handleWrite(SelectionKey key)
    {
        SocketChannel channel = (SocketChannel) key.channel();

        if (!channel.isOpen())
            return;

        connectionTable.popClosedConnection().ifPresent((connection) ->
                queue.pushPacket(ByteBuffer.wrap(new PacketConnectionClosed(connection.getChannelID()).toBytes())));

        try {
            if (queue.hasPacket())
                channel.write(queue.topPacket().getSilently());
            else
                switch (state)
                {
                    case ZERO:
                        handleWriteAKey(key);
                        break;

                    case ONLINE:
                        handleWriteOnline(key);
                        break;
                }
        } catch (Exception e) {
            logger.severe("Exception occurred while writing packet. State: " + state);
            e.printStackTrace();

            try {
                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void handleWriteOnline(SelectionKey key)
    {
    }

    @Override
    public void tick()
    {
    }

    private void handleReadOnline(SelectionKey key, Packet packet)
    {
        switch (packet.getType())
        {
            case CONNECTION_CLOSED:
                handleReadOnlineConnectionClosed(key, (PacketConnectionClosed) packet);
                break;

            case CONNECTION_ESTABLISHED:
                handleReadOnlineConnectionEstablished(key, (PacketConnectionEstablished) packet);
                break;

            case TOCLIENT_PORT_ALLOCATION_RESULT:
                handleReadOnlinePortAllocationResult(key, (Packet2ClientPortAllocationResult) packet);
                break;

            case UNIVERSAL_DATA:
                handleReadOnlineData(key, (PacketUniversalData) packet);
                break;
        }
    }

    private void handleReadOnlineConnectionClosed(SelectionKey key, PacketConnectionClosed packet)
    {
        UUID id = packet.getChannelID();

        Connection connection = connectionTable.getConnection(id).getSilently();

        if (connection == null)
        {
            logger.severe("Closing an ghost connection {" + id + "}. Ignored.");
            return;
        }

        connection.closePassively();

        logger.info("Connection {" + connection.getChannelID() + "} on port " + connection.getPort() + " closed.");
    }

    private void handleReadOnlineConnectionEstablished(SelectionKey key, PacketConnectionEstablished packet)
    {
        UUID id = packet.getChannelID();
        int port = packet.getPort();

        PortTable portTable = connectionTable.getPortTable();

        if (!portTable.isOnline(port))
        {
            logger.warning("Establishing connection on unavailable port " + port + ". Ignored.");
            return;
        }

        logger.info("Establishing connection {" + id + "} on port " + port + ".");

        if (!instanceThread.getPayload().preregisteredConnection.add(id))
        {
            logger.severe("Connection {" + id + "} duplicated on preregister table. May be concurrent failure.");
            return;
        }

        instanceThread.getPayload().preregisterBuffer.put(id, new PacketQueue());

        try {
            instanceThread.establishConnection(port, id);
        } catch (IOException e) {
            logger.severe("Failed to establish connection {" + id + "} on port " + port + ". Exception occurred.");
            e.printStackTrace();

            return;
        }
    }

    private void handleReadOnlinePortAllocationResult(SelectionKey key, Packet2ClientPortAllocationResult packet)
    {
        if (!waitingPortAllocationResult)
        {
            logger.warning("Unspecified port allocation received from server. Ignored");
            return;
        }

        boolean result = packet.getResult();
        int port = lastAllocatingPort;

        if (!result)
            logger.warning("Port " + port + " allocation request aborted.");
        else
            if (!connectionTable.getPortTable().establish(port))
                logger.severe("Port " + port + " not found on port table. Ignored.");
            else
                logger.info("Port " + port + " allocation request accepted. State set to online.");

        waitingPortAllocationResult = false;
    }

    private void handleReadOnlineData(SelectionKey key, PacketUniversalData packet)
    {
        UUID id = packet.getChannelID();
        byte[] data = packet.getData();

        Connection connection = connectionTable.getConnection(id).getSilently();

        if (connection == null)
        {
            PacketQueue preregBuffer = instanceThread.getPayload().preregisterBuffer.get(id);
            if (preregBuffer != null)
            {
                preregBuffer.pushPacket(ByteBuffer.wrap(data));
                return;
            }

            logger.warning("Received data for ghost connection {" + id + "}. Ignored");
            return;
        }

        connection.getPacketQueue().pushPacket(ByteBuffer.wrap(data));
    }

    private void handleWriteAKey(SelectionKey key)
    {
        queue.pushPacket(
                ByteBuffer.wrap(new Packet2ServerAKeyVerification(token.getAKey()).toBytes()));

        state = State.WAITING_AKEY_AUTH_RESULT;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public AuthorizationToken getToken()
    {
        return token;
    }

    public State getState()
    {
        return state;
    }

    public PacketQueue getPacketQueue()
    {
        return queue;
    }

    public PortTable getPortTable()
    {
        return portTable;
    }

    public ConnectionTable getConnectionTable()
    {
        return connectionTable;
    }

    public InstanceThread getInstanceThread()
    {
        return instanceThread;
    }

    public void setInstanceThread(InstanceThread instanceThread)
    {
        this.instanceThread = instanceThread;
    }

    public boolean pushPortRequest(int port)
    {
        lastAllocatingPort = port;

        logger.info("Requesting remote port: " + port);

        queue.pushPacket(ByteBuffer.wrap(
                new Packet2ServerPortAllocation(port).toBytes()));

        waitingPortAllocationResult = true;

        while (waitingPortAllocationResult);

        return portAllocationResult;
    }

    private volatile boolean portAllocationResult = false;

    private volatile boolean waitingPortAllocationResult = false;

    private int lastAllocatingPort;

    private final ConcurrentLinkedDeque<Pair<Packet, SelectionKey>> receivedPacket = new ConcurrentLinkedDeque<>();

    private final PacketIterator iter = new PacketIterator(true, true);

    private final PacketQueue queue = new PacketQueue();

    private volatile State state;

    private final Logger logger;

    private final AuthorizationToken token;

    private final PortTable portTable;

    private final ConnectionTable connectionTable;

    private InstanceThread instanceThread;

    public static enum State
    {
        ZERO,
        WAITING_AKEY_AUTH_RESULT,
        AKEY_AUTH_FAILURE,
        WAITING_BKEY,
        WAITING_BKEY_AUTH_RESULT,
        ONLINE,
        BKEY_AUTH_FAILURE,
    }
}
