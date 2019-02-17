package org.kucro3.krossnat.client.payload;

import com.theredpixelteam.redtea.util.Optional;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ConnectionTable {
    public ConnectionTable(PortTable portTable)
    {
        this.portTable = portTable;
    }

    public PortTable getPortTable()
    {
        return portTable;
    }

    public Connection establishConnection(UUID id, int port, SocketChannel channel)
    {
        Connection connection = new Connection(this, id, port, channel);

        connections.put(id, connection);

        return connection;
    }

    public Optional<Connection> getConnection(UUID id)
    {
        return Optional.ofNullable(connections.get(id));
    }

    public boolean closeConnection(UUID id)
    {
        Connection connection = connections.remove(id);

        if (connection == null)
            return false;

        connection.close();
        closedConnections.addFirst(connection);

        return true;
    }

    public Optional<Connection> popClosedConnection()
    {
        if (closedConnections.isEmpty())
            return Optional.empty();

        return Optional.of(closedConnections.pollLast());
    }

    public boolean removeConnection(UUID id)
    {
        return connections.remove(id) != null;
    }

    private final ConcurrentLinkedDeque<Connection> closedConnections = new ConcurrentLinkedDeque<>();

    private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();

    private final PortTable portTable;
}
