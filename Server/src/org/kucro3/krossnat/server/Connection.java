package org.kucro3.krossnat.server;

import org.kucro3.krossnat.protocol.PacketQueue;

import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connection {
    public Connection(ConnectionTable owner, int port, UUID id)
    {
        this.owner = owner;
        this.port = port;
        this.id = id;
    }

    public UUID getConnectionID()
    {
        return id;
    }

    public ConnectionTable getOwner()
    {
        return owner;
    }

    public int getPort()
    {
        return port;
    }

    public void establish(SocketChannel channel)
    {
        online = true;
        this.channel = channel;
    }

    void close()
    {
        online = false;
        packetQueue.clear();

        try {
            if (channel.isOpen())
                channel.close();
        } catch (Exception e) {
            // mute
            e.printStackTrace();
        }
    }

    public SocketChannel getChannel()
    {
        return this.channel;
    }

    public void destroy()
    {
        owner.closeConnection(id, owner.getOwner().getSession());
    }

    public void destroyPassively()
    {
        owner.closeConnectionPassively(id);
    }

    public boolean isOnline()
    {
        return online;
    }

    public PacketQueue getPacketQueue()
    {
        return packetQueue;
    }

    private final PacketQueue packetQueue = new PacketQueue();

    private volatile SocketChannel channel;

    private volatile boolean online;

    private final ConnectionTable owner;

    private final UUID id;

    private final int port;
}
