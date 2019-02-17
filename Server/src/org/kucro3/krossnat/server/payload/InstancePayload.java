package org.kucro3.krossnat.server.payload;

import com.theredpixelteam.redtea.util.Pair;
import org.kucro3.krossnat.payload.Payload;
import org.kucro3.krossnat.protocol.PacketQueue;
import org.kucro3.krossnat.protocol.PacketUniversalData;
import org.kucro3.krossnat.protocol.SelectionKeyEncouragementPacketQueueListener;
import org.kucro3.krossnat.server.Connection;
import org.kucro3.krossnat.server.ConnectionTable;
import org.kucro3.krossnat.server.Instance;
import org.kucro3.krossnat.server.InstanceTable;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class InstancePayload implements Payload {
    public InstancePayload(Logger logger, InstanceThread owner, InstanceTable instances)
    {
        this.logger = logger;
        this.owner = owner;
        this.instances = instances;
    }

    @Override
    public void handleConnect(SelectionKey key)
    {
        // unused
    }

    @Override
    public void handleAccept(SelectionKey key)
    {
        UUID uuid = UUID.randomUUID();

        Pair<Integer, PayloadSession> pair = (Pair<Integer, PayloadSession>) key.attachment();

        int port = pair.first();
        PayloadSession session = pair.second();

        Instance instance = instances.getInstanceOnPort(port).getSilently();

        if (instance == null)
        {
            logger.warning("Instance not found on port " + port + ". May be concurrent failure.");
            return;
        }

        try {
            SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();

            ConnectionTable connections = instance.getConnections();
            Connection connection = connections.establishConnection(socketChannel, port, session);

            socketChannel.configureBlocking(false);
            SelectionKey channelKey = socketChannel.register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE, connection);

            connection.getPacketQueue().registerListener(new SelectionKeyEncouragementPacketQueueListener(channelKey));

            logger.info("Accepted connection from "
                    + socketChannel.getRemoteAddress()
                    + " for instance {" + instance.getSession().getChannelID() + "} on port " + port + ".");
        } catch (Exception e) {
            logger.info("Exception occurred pre-establishing connection for instance {"
                    + instance.getSession().getChannelID() + "} on port " + port + ".");
            e.printStackTrace();
        }
    }

    @Override
    public void handleRead(SelectionKey key)
    {
        Connection connection = (Connection) key.attachment();
        int port = connection.getPort();

        SocketChannel channel = connection.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        byte[] byts = new byte[4096];

        Instance instance = instances.getInstanceOnPort(port).getSilently();

        if (instance == null)
        {
            logger.warning("Instance not found on port " + port + ". May be concurrent failure.");
            return;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int cap;
            if ((cap = channel.read(buffer)) > 0)
            {
                buffer.flip();
                buffer.get(byts, 0, cap);
                baos.write(byts, 0, cap);

                buffer.clear();
            }


            if (cap < 0 && !connection.getPacketQueue().hasPacket())
            {
                connection.destroy();
                logger.info("Connection {" + connection.getConnectionID() + "} closed. EOF.");

                return;
            }

            byte[] data = baos.toByteArray();

            if (data.length > 0)
                instance.getSession().getPacketQueue().pushPacket(
                        ByteBuffer.wrap(new PacketUniversalData(connection.getConnectionID(), data).toBytes()));
        } catch (Exception e) {
            logger.severe("Exception occurred when reading from connection {"
                    + connection.getConnectionID() + "}. Connection closed.");
            e.printStackTrace();

            connection.destroy();
        }
    }

    @Override
    public void handleWrite(SelectionKey key)
    {
        Connection connection = (Connection) key.attachment();
        PacketQueue queue = connection.getPacketQueue();

        if (queue.hasPacket()) {
            try {
                connection.getChannel().write(queue.topPacket().getSilently());
             } catch (Exception e) {
                logger.severe("Exception occurred when wirting to connection {"
                        + connection.getConnectionID() + "}. Connection closed.");
                e.printStackTrace();

                connection.destroy();
            }
        }
    }

    @Override
    public void tick()
    {
        if (!owner.taskQueue.isEmpty())
            owner.taskQueue.pollLast().run();
    }

    public InstanceThread getOwner()
    {
        return owner;
    }

    public InstanceTable getInstanceTable()
    {
        return instances;
    }

    public Logger getLogger()
    {
        return logger;
    }

    private final InstanceThread owner;

    private final InstanceTable instances;

    private final Logger logger;
}
