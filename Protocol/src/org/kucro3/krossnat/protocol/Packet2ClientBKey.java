package org.kucro3.krossnat.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Packet2ClientBKey implements Packet {
    public Packet2ClientBKey()
    {
    }

    public Packet2ClientBKey(UUID[] bKeys)
    {
        this.bKeys = bKeys;
    }

    @Override
    public byte[] toBytes()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(baos);

        try {
            os.writeByte(TYPE.getCode() & 0xFF);

            for (int i = 0; i < 64; i++)
            {
                os.writeLong(bKeys[i].getMostSignificantBits());
                os.writeLong(bKeys[i].getLeastSignificantBits());
            }
        } catch (IOException e) {
            // unused
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    @Override
    public void read(ByteBuffer buffer, int len)
    {
        this.bKeys = new UUID[64];

        for (int i = 0; i < 64; i++)
        {
            long most = buffer.getLong();
            long least = buffer.getLong();

            bKeys[i] = new UUID(most, least);
        }
    }

    public UUID[] getBKeys()
    {
        return bKeys;
    }

    @Override
    public boolean completed()
    {
        return bKeys != null;
    }

    @Override
    public PacketEnum getType()
    {
        return TYPE;
    }

    private UUID[] bKeys;

    public static final PacketEnum TYPE = PacketEnum.TOCLIENT_BKEY;
}
