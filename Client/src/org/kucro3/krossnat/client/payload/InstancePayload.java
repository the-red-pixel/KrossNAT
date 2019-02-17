package org.kucro3.krossnat.client.payload;

import com.sun.xml.internal.fastinfoset.algorithm.UUIDEncodingAlgorithm;
import com.theredpixelteam.redtea.util.Pair;
import org.kucro3.krossnat.payload.Payload;
import org.kucro3.krossnat.protocol.PacketQueue;
import org.kucro3.krossnat.protocol.PacketUniversalData;
import org.kucro3.krossnat.protocol.SelectionKeyEncouragementPacketQueueListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class InstancePayload implements Payload {
    public InstancePayload(Logger logger, ClientPayload payload, ConnectionTable table)
    {
        this.logger = logger;
        this.payload = payload;
        this.connectionTable = table;
    }

    @Override
    public void handleConnect(SelectionKey key)
    {
        SocketChannel channel = (SocketChannel) key.channel();

        try {
            if (!channel.finishConnect())
                return;
        } catch (IOException e) {
            e.printStackTrace();
            try {
                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        Pair<Integer, UUID> pair = (Pair<Integer, UUID>) key.attachment();

        int port = pair.first();
        UUID id = pair.second();

        Connection connection = connectionTable.establishConnection(id, port, channel);
        connection.getPacketQueue().registerListener(new SelectionKeyEncouragementPacketQueueListener(key));

        key.attach(connection);

        logger.info("Connection {" + pair.second() + "} established on "
                + connectionTable.getPortTable().getPortAddress(pair.first()).getSilently());

        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    public void handleAccept(SelectionKey key)
    {
        // unused
    }

    @Override
    public void handleRead(SelectionKey key)
    {
        Connection connection = (Connection) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        try {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            byte[] byts = new byte[4096];

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
                logger.info("Connection {" + connection.getChannelID() + "} closed. EOF.");

                return;
            }

            byte[] data = baos.toByteArray();

            if (data.length > 0)
                payload.getPacketQueue().pushPacket(ByteBuffer.wrap(
                        new PacketUniversalData(connection.getChannelID(), data).toBytes()));
        } catch (Exception e) {
            logger.severe("Exception occurred reading on connection {" + connection.getChannelID() + "}, connection closed.");
            e.printStackTrace();
            connection.destroy();
        }
    }

    @Override
    public void handleWrite(SelectionKey key)
    {
        Connection connection = (Connection) key.attachment();
        PacketQueue queue = connection.getPacketQueue();

        try {
            if (queue.hasPacket())
                connection.getChannel().write(queue.topPacket().getSilently());
        } catch (Exception e) {
            logger.severe("Exception occurred writing packet to connection {" + connection.getChannelID() + "}.");
            e.printStackTrace();

            connection.destroy();
        }
    }

    @Override
    public void tickOnKey(SelectionKey key)
    {
    }

    @Override
    public void tick()
    {
        while (!taskQueue.isEmpty())
            taskQueue.pollLast().run();

        Iterator<UUID> iter = preregisteredConnection.iterator();
        while (iter.hasNext())
        {
            UUID id = iter.next();
            Connection connection = connectionTable.getConnection(id).getSilently();

            try {
                if (connection == null || connection.getChannel().finishConnect())
                {
                    iter.remove();
                    preregisterBuffer.remove(id);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ConnectionTable getConnectionTable()
    {
        return connectionTable;
    }

    public ClientPayload getPayload()
    {
        return payload;
    }

    public Logger getLogger()
    {
        return logger;
    }

    final ConcurrentLinkedDeque<Runnable> taskQueue = new ConcurrentLinkedDeque<>();

    final Set<UUID> preregisteredConnection = new HashSet<>();

    final Map<UUID, PacketQueue> preregisterBuffer = new ConcurrentHashMap<>();

    private final Logger logger;

    private final ClientPayload payload;

    private final ConnectionTable connectionTable;
}
