package org.kucro3.krossnat.client.task;

import org.kucro3.krossnat.client.payload.ClientPayload;
import org.kucro3.krossnat.payload.task.AbstractTask;
import org.kucro3.krossnat.protocol.PacketUniversalPing;

import java.nio.ByteBuffer;

public class ClientPingTask extends AbstractTask {
    public ClientPingTask(ClientPayload payload)
    {
        this.payload = payload;
    }

    @Override
    public boolean shouldExecute()
    {
        return ping;
    }

    @Override
    public boolean shouldRequeue()
    {
        return payload.getState().equals(ClientPayload.State.ONLINE);
    }

    @Override
    public void run() // pong
    {
        payload.getPacketQueue().pushPacket(
                ByteBuffer.wrap(new PacketUniversalPing(stamp).toBytes()));

        ping = false;
    }

    public ClientPayload getPayload()
    {
        return payload;
    }

    public void ping()
    {
        this.ping = true;
    }

    public long getStamp()
    {
        return stamp;
    }

    public void setStamp(long stamp)
    {
        this.stamp = stamp;
    }

    private final ClientPayload payload;

    private volatile long stamp;

    private volatile boolean ping;
}
