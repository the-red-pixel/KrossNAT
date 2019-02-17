package org.kucro3.krossnat.server;

import com.theredpixelteam.redtea.util.Optional;
import org.kucro3.krossnat.payload.Payload;
import org.kucro3.krossnat.protocol.PacketConnectionClosed;
import org.kucro3.krossnat.protocol.PacketConnectionEstablished;
import org.kucro3.krossnat.server.payload.PayloadSession;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionTable {
    public ConnectionTable(Instance owner)
    {
        this.owner = owner;
    }

    public Instance getOwner()
    {
        return owner;
    }

    Connection establishConnection0(SocketChannel channel, int port, UUID id, boolean queue, PayloadSession session)
    {
        Connection connection = new Connection(this, port, id);

        if (connections.putIfAbsent(id, connection) != null)
            throw new IllegalStateException("Connection duplicated: " + id);

        connection.establish(channel);

        if (queue)
            pushConnectionEsablishedPacket(session, id, port);

        return connection;
    }

    public Connection establishConnection(SocketChannel channel, int port, PayloadSession session)
    {
        return establishConnection0(channel, port, UUID.randomUUID(), true, session);
    }

    public Connection establishConnectionPassively(SocketChannel channel, int port, UUID id)
    {
        return establishConnection0(channel, port, id, false, null);
    }

    int closeConnectionsOnPort0(int port, boolean queue, PayloadSession session)
    {
        int count = 0;

        Iterator<Map.Entry<UUID, Connection>> iter = connections.entrySet().iterator();
        Map.Entry<UUID, Connection> entry;
        while (iter.hasNext())
        {
            entry = iter.next();
            Connection connection = entry.getValue();

            if (connection.getPort() == port)
            {
                iter.remove();

                connection.close();

                if (queue)
                    pushConnectionClosedPacket(session, connection.getConnectionID());
            }
        }

        return count;
    }

    public int closeConnectionsOnPort(int port, PayloadSession session)
    {
        return closeConnectionsOnPort0(port, true, session);
    }

    public int closeConnectionsOnPortPassively(int port)
    {
        return closeConnectionsOnPort0(port, false, null);
    }

    boolean closeConnection0(UUID uuid, boolean queue, PayloadSession session)
    {
        Connection connection = connections.get(uuid);

        if (connection == null)
            return false;

        connection.close();

        connections.remove(uuid);

        if (queue)
            pushConnectionClosedPacket(session, uuid);

        return true;
    }

    private void pushConnectionEsablishedPacket(PayloadSession session, UUID id, int port)
    {
        session.getPacketQueue().pushPacket(
                ByteBuffer.wrap(new PacketConnectionEstablished(id, port).toBytes()));
    }

    private void pushConnectionClosedPacket(PayloadSession session, UUID id)
    {
        session.getPacketQueue().pushPacket(
                ByteBuffer.wrap(new PacketConnectionClosed(id).toBytes()));
    }

    public boolean closeConnection(UUID uuid, PayloadSession session)
    {
        return closeConnection0(uuid, true, session);
    }

    public boolean closeConnectionPassively(UUID uuid)
    {
        return closeConnection0(uuid, false, null);
    }

    public Optional<Connection> getConnection(UUID uuid)
    {
        return Optional.ofNullable(connections.get(uuid));
    }

    public Map<UUID, Connection> getConnections()
    {
        return Collections.unmodifiableMap(connections);
    }

    private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();

    private final Instance owner;
}
