package org.kucro3.krossnat.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@SuppressWarnings("DuplicatedCode")
public class PacketUniversalPing implements Packet {
    public PacketUniversalPing()
    {
    }

    public PacketUniversalPing(long stamp)
    {
        this.stamp = stamp;
    }

    @Override
    public byte[] toBytes()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            dos.writeByte(TYPE.getCode() & 0xFF);

            dos.writeLong(stamp);
        } catch (IOException e) {
            // unused
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    @Override
    public void read(ByteBuffer buffer, int len)
    {
        this.stamp = buffer.getLong();
    }

    @Override
    public boolean completed()
    {
        return true;
    }

    @Override
    public PacketEnum getType()
    {
        return TYPE;
    }

    public long getStamp()
    {
        return stamp;
    }

    public void setStamp(long stamp)
    {
        this.stamp = stamp;
    }

    private long stamp;

    public static final PacketEnum TYPE = PacketEnum.UNIVERSAL_PING;
}
