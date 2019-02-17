package org.kucro3.krossnat.protocol;

import java.nio.ByteBuffer;

public class Packet2ClientPortAllocationResult implements Packet {
    public Packet2ClientPortAllocationResult()
    {
    }

    public Packet2ClientPortAllocationResult(boolean result)
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

    public boolean getResult()
    {
        return result;
    }

    @Override
    public PacketEnum getType()
    {
        return TYPE;
    }

    private Boolean result;

    public static final PacketEnum TYPE = PacketEnum.TOCLIENT_PORT_ALLOCATION_RESULT;
}
