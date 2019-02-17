package org.kucro3.krossnat.client.payload;

import org.kucro3.krossnat.protocol.PacketQueue;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connection {
    public Connection(ConnectionTable owner, UUID id, int port, SocketChannel channel)
    {
        this.owner = owner;
        this.channel = channel;
        this.channelID = id;
        this.port = port;
    }

    void close()
    {
        try {
            if (channel.isOpen())
                channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PacketQueue getPacketQueue()
    {
        return packetQueue;
    }

    public void closePassively()
    {
        owner.removeConnection(channelID);

        close();
    }

    public void destroy()
    {
        owner.closeConnection(channelID);
    }

    public SocketChannel getChannel()
    {
        return channel;
    }

    public UUID getChannelID()
    {
        return channelID;
    }

    public ConnectionTable getOwner()
    {
        return owner;
    }

    public int getPort()
    {
        return port;
    }

    private final int port;

    private final ConnectionTable owner;

    private final UUID channelID;

    private final SocketChannel channel;

    private final PacketQueue packetQueue = new PacketQueue();
}
