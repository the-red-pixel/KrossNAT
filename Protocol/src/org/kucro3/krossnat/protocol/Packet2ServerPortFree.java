package org.kucro3.krossnat.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Packet2ServerPortFree implements Packet {
    public Packet2ServerPortFree()
    {
    }

    public Packet2ServerPortFree(int port)
    {
        this.port = port;
    }

    @Override
    public byte[] toBytes()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(baos);

        try {
            os.writeByte(TYPE.getCode());

            os.writeInt(port);
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
    }

    @Override
    public boolean completed()
    {
        return port != null;
    }

    @Override
    public PacketEnum getType()
    {
        return TYPE;
    }

    public int getPort()
    {
        return port;
    }

    private Integer port;

    public static final PacketEnum TYPE = PacketEnum.TOSERVER_PORT_FREE;
}
