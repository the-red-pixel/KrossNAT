package org.kucro3.krossnat.protocol;

import java.nio.ByteBuffer;

public class Packet2ClientAuthResult implements Packet {
    public Packet2ClientAuthResult()
    {
    }

    public Packet2ClientAuthResult(boolean result)
    {
        this.result = result;
    }

    @Override
    public byte[] toBytes()
    {
        return new byte[] {TYPE.getCode(), (byte)(result ? 1 : 0)};
    }

    @Override
    public void read(ByteBuffer buffer, int len)
    {
        this.result = buffer.get() != 0;
    }

    @Override
    public boolean completed()
    {
        return result != null;
    }

    @Override
    public PacketEnum getType()
    {
        return TYPE;
    }

    public boolean getResult()
    {
        return result;
    }

    private Boolean result;

    public static final PacketEnum TYPE = PacketEnum.TOCLIENT_AUTH_RESULT;
}
