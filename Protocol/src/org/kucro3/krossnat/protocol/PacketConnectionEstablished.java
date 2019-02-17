package org.kucro3.krossnat.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PacketConnectionEstablished implements Packet {
    public PacketConnectionEstablished()
    {
    }

    public PacketConnectionEstablished(UUID channelID, int port)
    {
        this.channelID = channelID;
        this.port = port;
    }

    @Override
    public byte[] toBytes()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(baos);

        try {
            os.writeByte(TYPE.getCode() & 0xFF);

            os.writeInt(port);

            os.writeLong(channelID.getMostSignificantBits());
            os.writeLong(channelID.getLeastSignificantBits());
        } catch (IOException e) {
            // unused
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    @Override
    public void read(ByteBuffer buffer, int len)
    {
        this.port = buffer.getInt();

        long most = buffer.getLong();
        long least = buffer.getLong();

        this.channelID = new UUID(most, least);
    }

    @Override
    public boolean completed()
    {
        return channelID != null && port != null;
    }

    @Override
    public PacketEnum getType()
    {
        return TYPE;
    }

    public UUID getChannelID()
    {
        return channelID;
    }

    public int getPort()
    {
        return this.port;
    }

    private Integer port;

    private UUID channelID;

    public static final PacketEnum TYPE = PacketEnum.CONNECTION_ESTABLISHED;
}
