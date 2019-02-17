package org.kucro3.krossnat.protocol;

import java.nio.ByteBuffer;

public interface Packet {
    public byte[] toBytes();

    public void read(ByteBuffer buffer, int len);

    public boolean completed();

    public PacketEnum getType();
}
