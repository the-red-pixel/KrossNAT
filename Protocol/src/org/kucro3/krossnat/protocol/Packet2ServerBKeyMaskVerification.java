package org.kucro3.krossnat.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Packet2ServerBKeyMaskVerification implements Packet {
    public Packet2ServerBKeyMaskVerification()
    {
    }

    public Packet2ServerBKeyMaskVerification(long mask)
    {
        this.mask = mask;
    }

    @Override
    public byte[] toBytes()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            dos.writeByte(TYPE.getCode() & 0xFF);

            dos.writeLong(mask);
        } catch (IOException e) {
            // unused
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    @Override
    public void read(ByteBuffer buffer, int len)
    {
        this.mask = buffer.getLong();
    }

    @Override
    public boolean completed()
    {
        return mask != null;
    }

    @Override
    public PacketEnum getType()
    {
        return TYPE;
    }

    public long getMask()
    {
        return mask;
    }

    private Long mask;

    public static final PacketEnum TYPE = PacketEnum.TOSERVER_BKEY_MASK_VERIFICATION;
}
