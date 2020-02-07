package org.kucro3.krossnat.server.payload;

import com.theredpixelteam.redtea.util.Optional;
import com.theredpixelteam.redtea.util.Pair;
import org.kucro3.krossnat.auth.AuthorizaionSession;
import org.kucro3.krossnat.auth.Authorization;
import org.kucro3.krossnat.auth.AuthorizationToken;
import org.kucro3.krossnat.auth.AuthorizationTokenPool;
import org.kucro3.krossnat.payload.Payload;
import org.kucro3.krossnat.payload.task.TaskQueue;
import org.kucro3.krossnat.protocol.*;
import org.kucro3.krossnat.server.Connection;
import org.kucro3.krossnat.server.ConnectionTable;
import org.kucro3.krossnat.server.Instance;
import org.kucro3.krossnat.server.InstanceTable;
import org.kucro3.krossnat.server.task.ServerPingTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class ServerPayload implements Payload {
    public ServerPayload(Logger logger, Authorization auth, InstanceTable instanceTable)
    {
        this.logger = logger;
        this.auth = auth;
        this.tokenPool = auth.getTokenPool();
        this.instanceTable = instanceTable;
    }

    @Override
    public void handleConnect(SelectionKey key)
    {
        // unused
    }

    @Override
    public void handleAccept(SelectionKey key)
    {
        try {
            SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
            UUID uuid = UUID.randomUUID();

            logger.info("Accepting connection from " + socketChannel.getRemoteAddress());
            logger.info("Attaching id {" + uuid + "} to connection");

            PayloadSession session = new PayloadSession(uuid);
            session.setState(State.WAITING_AKEY);

            socketChannel.configureBlocking(false);
            SelectionKey channelKey = socketChannel.register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE, session);

            session.getPacketQueue().registerListener(new SelectionKeyEncouragementPacketQueueListener(channelKey));
        } catch (Exception e) {
            logger.severe("Exception occurred handling session initialization");
            e.printStackTrace();
        }
    }

    @Override
    public void handleRead(SelectionKey key)
    {
        PayloadSession session = (PayloadSession) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        if (!channel.isOpen())
            return;

        PacketIterator iter = session.getIterator();

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

            if (cap < 0)
            {
                if (session.getInstance() != null)
                    session.getInstance().destroy();

                channel.close();
            }

            try {
                while (iter.hasNext())
                {
                    Packet packet = iter.next();

                    try {
                        switch (session.getState())
                        {
                            case WAITING_AKEY:
                                handleReadAKey(key, session, packet);
                                break;

                            case VERIFYING_BKEY:
                                handleReadBKey(key, session, packet);
                                break;

                            case ONLINE:
                                handleReadOnline(key, session, packet);
                                break;
                        }
                    } catch (Exception e) {
                        logger.severe("Exception occurred ticking on session {" + session.getChannelID() + "}");
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                logger.severe("Failed to parse packet");
                e.printStackTrace();

                channel.close();
                return;
            }
        } catch (Exception e) {
            logger.severe("Exception occurred handling session read {" + session.getChannelID() + "}");
            e.printStackTrace();

            try {
                if (session.getInstance() != null)
                    session.getInstance().destroy();

                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void handleReadOnline(SelectionKey key, PayloadSession session, Packet packet)
    {
        switch (packet.getType())
        {
            case CONNECTION_CLOSED:
                handleReadOnlineConnectionClosed(key, session, (PacketConnectionClosed) packet);
                break;

//            case CONNECTION_ESTABLISHED:
//                break;

            case TOSERVER_PORT_ALLOCATION:
                handleReadOnlinePortAllocation(key, session, (Packet2ServerPortAllocation) packet);
                break;

            case TOSERVER_PORT_FREE:
                handleReadOnlinePortFree(key, session, (Packet2ServerPortFree) packet);
                break;

            case UNIVERSAL_DATA:
                handleReadOnlineData(key, session, (PacketUniversalData) packet);
                break;

            case UNIVERSAL_PING:
                handleReadOnlinePing(key, session, (PacketUniversalPing) packet);
                break;
        }
    }

    private void handleReadOnlinePing(SelectionKey key, PayloadSession session, PacketUniversalPing packet)
    {
        ServerPingTask pingTask = session.getPingTask();

        if (pingTask.getSentPacket() == null)
            return; // ignored

        long stamp = packet.getStamp();

        if (stamp != pingTask.getSentPacket().getStamp())
            session.setState(State.ECHO_CORRUPTION);
        else
            pingTask.echo();
    }

    private void handleReadOnlineConnectionClosed(SelectionKey key, PayloadSession session, PacketConnectionClosed packet)
    {
        Instance instance = session.getInstance();
        ConnectionTable connections = instance.getConnections();

        UUID connectionID = packet.getChannelID();
        Connection connection = connections.getConnection(connectionID).getSilently();

        if (connection != null)
        {
            connection.destroyPassively();
            logger.info("Instance {" + connectionID + "} connection closed.");
        }
        else
            logger.warning("Closing a connection that not exists: {" + connectionID + "}");
    }

    private void handleReadOnlinePortAllocation(SelectionKey key, PayloadSession session, Packet2ServerPortAllocation packet)
    {
        Instance instance = session.getInstance();
        PacketQueue queue = session.getPacketQueue();

        int port = packet.getPort();

        try {
            if (!instance.allocatePort(port))
            {
                logger.warning("Port " + port + " in use or exception occurred. Allocation request from {" + session.getChannelID() + "} aborted.");

                queue.pushPacket(
                        ByteBuffer.wrap(new Packet2ClientPortAllocationResult(false).toBytes()));
            }
            else
            {
                logger.info("Port " + port + " allocated to instance {" + session.getChannelID() + "}");

                queue.pushPacket(
                        ByteBuffer.wrap(new Packet2ClientPortAllocationResult(true).toBytes()));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred allocation port. Allocation require from {" + session.getChannelID() + "} aborted.");
            e.printStackTrace();

            queue.pushPacket(
                    ByteBuffer.wrap(new Packet2ClientPortAllocationResult(false).toBytes()));
        }
    }

    private void handleReadOnlinePortFree(SelectionKey key, PayloadSession session, Packet2ServerPortFree packet)
    {
        Instance instance = session.getInstance();

        int port = packet.getPort();

        if (!instance.freePort(port))
            logger.warning("Freeing the port " + port + " which is not in use. Ignored.");
        else
            logger.info("Freeing port " + port + " from instance {" + session.getChannelID() + "}.");
    }

    private void handleReadOnlineData(SelectionKey key, PayloadSession session, PacketUniversalData packet)
    {
        ConnectionTable connections = session.getInstance().getConnections();

        byte[] data = packet.getData();
        UUID connectionID = packet.getChannelID();

        Connection connection = connections.getConnection(connectionID).getSilently();

        if (connection != null)
            connection.getPacketQueue().pushPacket(ByteBuffer.wrap(data));
        else
            logger.warning("Received data from ghost connection {" + connectionID + "}");
    }

    private void handleReadAKey(SelectionKey key, PayloadSession session, Packet packet) throws IOException
    {
        switch (packet.getType()) {
            case TOSERVER_AKEY_VERIFICATION:
                Packet2ServerAKeyVerification p = (Packet2ServerAKeyVerification) packet;
                UUID aKey = p.getAKey();

                logger.info("Session sending back token: " + aKey);

                Optional<AuthorizationToken> tokenOptional = tokenPool.getToken(aKey);
                if (tokenOptional.isPresent())
                {
                    session.setToken(tokenOptional.getSilently());
                    session.setState(State.AKEY_ACCEPTED);

                    logger.info("Token " + aKey + " accepted.");

                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
                else
                {
                    session.setState(State.AKEY_ABORTED);

                    logger.info("Token " + aKey + " aborted.");
                }

                break;

            default:
                logProtocolCorruption(session.getChannelID(), packet.getType(), PacketEnum.TOSERVER_AKEY_VERIFICATION);
                key.channel().close();
        }
    }

    private void handleReadBKey(SelectionKey key, PayloadSession session, Packet packet) throws IOException
    {
        switch (packet.getType()) {
            case TOSERVER_BKEY_MASK_VERIFICATION:
                Packet2ServerBKeyMaskVerification p = (Packet2ServerBKeyMaskVerification) packet;
                long mask = p.getMask();

                AuthorizaionSession authSession = session.getAuthSession();
                authSession.receiveMask(mask);

                if (auth.verifySessionAndRemove(authSession))
                {
                    logger.info("B Key mask accepted for session {" + session. getChannelID() + "}");
                    session.setState(State.ACCEPTED);

                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
                else {
                    logger.info("B Key mask aborted for session {" + session.getChannelID() + "}");
                    session.setState(State.ABORTED);
                }

                break;

            default:
                logProtocolCorruption(session.getChannelID(), packet.getType(), PacketEnum.TOSERVER_BKEY_MASK_VERIFICATION);
                key.channel().close();
        }
    }

    @Override
    public void handleWrite(SelectionKey key)
    {
        PayloadSession session = (PayloadSession) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        PacketQueue queue = session.getPacketQueue();

        if (!channel.isOpen())
            return;

        try {
            if (queue.hasPacket())
                channel.write(queue.topPacket().getSilently());
            else
                switch (session.getState()) {
                    case ECHO_CORRUPTION:
                        logger.severe("Session {" + session.getChannelID() + "} echo malformation.");

                        session.getInstance().destroy();
                        channel.close();
                        break;

                    case TIMED_OUT:
                        logger.severe("Session {" + session.getChannelID() + "} timed out.");

                        session.getInstance().destroy();
                        channel.close();
                        break;

                    case DEAD:
                        channel.close();
                        break;

                    case AKEY_ABORTED:
                        handleWriteAKeyAborted(key, session);
                        break;

                    case AKEY_ACCEPTED:
                        handleWriteVerifyBKey(key, session);
                        break;

                    case ABORTED:
                        handleWriteBKeyAborted(key, session);
                        break;

                    case ACCEPTED:
                        handleWriteBKeyAccepted(key, session);
                        break;

                    case ONLINE:
                        handleWriteOnline(key, session);
                        break;
                }
        } catch (Exception e) {
            logger.severe("Exception occurred handling session write {" + session.getChannelID() + "}");
            e.printStackTrace();

            try {
                session.getInstance().destroy();
                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void tick()
    {
    }

    private void handleWriteOnline(SelectionKey key, PayloadSession session) throws IOException
    {
        // unused
    }

    private void handleWriteAKeyAborted(SelectionKey key, PayloadSession session) throws IOException
    {
        session.getPacketQueue().pushPacket(
                ByteBuffer.wrap(new Packet2ClientAuthResult(false).toBytes()));

        logger.info("Authorization failure. Connection {" + session.getChannelID() + "} aborted.");

        session.setState(State.DEAD);
    }

    private void handleWriteVerifyBKey(SelectionKey key, PayloadSession session) throws IOException
    {
        AuthorizationToken token = session.getToken();
        AuthorizaionSession authSession = AuthorizaionSession.createSession(token);

        UUID[] maskSource;
        authSession.defineMaskSource(maskSource = auth.generateMaskSource(token.getBKeys()));

        session.setAuthSession(authSession);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(new Packet2ClientAuthResult(true).toBytes());
        baos.write(new Packet2ClientBKey(maskSource).toBytes());

        session.getPacketQueue().pushPacket(ByteBuffer.wrap(baos.toByteArray()));
        session.setState(State.VERIFYING_BKEY);
    }

    private void handleWriteBKeyAccepted(SelectionKey key, PayloadSession session) throws IOException
    {
        session.getPacketQueue().pushPacket(
                ByteBuffer.wrap(new Packet2ClientAuthResult(true).toBytes()));

        session.setInstance(new Instance(instanceTable, session));

        taskQueue.queue(session.getPingTask());

        logger.info("Authorization verified for connection {" + session.getChannelID() + "}.");

        session.setState(State.ONLINE);
    }

    private void handleWriteBKeyAborted(SelectionKey key, PayloadSession session) throws IOException
    {
        session.getPacketQueue().pushPacket(
                ByteBuffer.wrap(new Packet2ClientAuthResult(false).toBytes()));

        logger.info("Authorization failure. Connection {" + session.getChannelID() + "} aborted.");

        session.setState(State.DEAD);
    }

    private void logProtocolCorruption(UUID id, PacketEnum received, PacketEnum expected)
    {
        logger.severe("Protocol corruption on connection {" + id + "}: "
                + "Receiving " + received + ", expecting " + expected);
    }

    @Override
    public TaskQueue getTaskQueue()
    {
        return taskQueue;
    }

    private final TaskQueue taskQueue = new TaskQueue();

    private final ConcurrentLinkedDeque<Pair<Packet, SelectionKey>> receivedPackets = new ConcurrentLinkedDeque<>();

    private final InstanceTable instanceTable;

    private final Authorization auth;

    private final AuthorizationTokenPool tokenPool;

    private final Logger logger;

    public static enum State
    {
        WAITING_AKEY    (true),
        AKEY_ABORTED    (false),
        AKEY_ACCEPTED   (true),
        VERIFYING_BKEY  (false),
        ACCEPTED        (true),
        ABORTED         (false),
        ONLINE          (true),
        DEAD            (false),
        TIMED_OUT       (false),
        ECHO_CORRUPTION (false);

        private State(boolean alive)
        {
            this.alive = alive;
        }

        public boolean isAlive()
        {
            return alive;
        }

        private final boolean alive;
    }
}
