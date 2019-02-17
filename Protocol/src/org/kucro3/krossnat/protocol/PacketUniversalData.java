package org.kucro3.krossnat.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PacketUniversalData implements Packet {
    public PacketUniversalData()
    {
    }

    public PacketUniversalData(UUID channelID, byte[] data)
    {
        this.channelID = channelID;
        this.data = data;
    }

    @Override
    public byte[] toBytes()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(baos);

        try {
            os.writeByte(TYPE.getCode() & 0xFF);

            int len = data.length + 16;

            os.writeInt(len);

            os.writeLong(channelID.getMostSignificantBits());
            os.writeLong(channelID.getLeastSignificantBits());

            os.write(data);
        } catch (IOException e) {
            // unused
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    @Override
    public void read(ByteBuffer buffer, int len)
    {
        int datalen = len - 16;

        long most = buffer.getLong();
        long least = buffer.getLong();

        this.channelID = new UUID(most, least);

        this.data = new byte[datalen];

        buffer.get(data);
    }

    @Override
    public boolean completed()
    {
        return channelID != null && data != null;
    }

    @Override
    public PacketEnum getType()
    {
        return TYPE;
    }

    public byte[] getData()
    {
        return data;
    }

    public UUID getChannelID()
    {
        return channelID;
    }

    private UUID channelID;

    private byte[] data;

    public static final PacketEnum TYPE = PacketEnum.UNIVERSAL_DATA;
}
