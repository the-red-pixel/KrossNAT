package org.kucro3.krossnat.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Packet2ServerAKeyVerification implements Packet {
    public Packet2ServerAKeyVerification()
    {
    }

    public Packet2ServerAKeyVerification(UUID aKey)
    {
        this.aKey = aKey;
    }

    @Override
    public byte[] toBytes()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(baos);

        try {
            os.writeByte(TYPE.getCode() & 0xFF);

            os.writeLong(aKey.getMostSignificantBits());
            os.writeLong(aKey.getLeastSignificantBits());
        } catch (IOException e) {
            // unused
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    @Override
    public void read(ByteBuffer buffer, int len)
    {
        long most = buffer.getLong();
        long least = buffer.getLong();

        aKey = new UUID(most, least);
    }

    public UUID getAKey()
    {
        return aKey;
    }

    @Override
    public boolean completed()
    {
        return aKey != null;
    }

    @Override
    public PacketEnum getType()
    {
        return TYPE;
    }

    private UUID aKey;

    public static final PacketEnum TYPE = PacketEnum.TOSERVER_AKEY_VERIFICATION;
}
