package org.kucro3.krossnat.server.task;

import com.theredpixelteam.redtea.util.Predication;
import org.kucro3.krossnat.payload.task.AbstractTask;
import org.kucro3.krossnat.protocol.PacketUniversalPing;
import org.kucro3.krossnat.server.payload.PayloadSession;
import org.kucro3.krossnat.server.payload.ServerPayload;

import java.nio.ByteBuffer;

public class ServerPingTask extends AbstractTask {
    public ServerPingTask(PayloadSession session)
    {
        this(session, DEFAULT_INTERVAL, DEFAULT_TIME_OUT);
    }

    public ServerPingTask(PayloadSession session,
                          long interval,
                          long timeOut)
    {
        this.session = session;
        this.interval = Predication.requirePositive(interval, "interval");
        this.timeOut = Predication.requirePositive(timeOut, "timeOut");
        this.lastTime = System.currentTimeMillis();
    }

    @Override
    public boolean shouldExecute()
    {
        if (sentPacket == null)
            return true;

        if (!echo)
            return (System.currentTimeMillis() - lastTime) >= timeOut;
        else
            return (System.currentTimeMillis() - lastTime) >= interval;
    }

    @Override
    public boolean shouldRequeue()
    {
        return session.getState().isAlive();
    }

    @Override
    public void onRequeue()
    {
        this.lastTime = System.currentTimeMillis();
    }

    @Override
    public void run()
    {
        if (sentPacket == null || echo) // packet not sent or echo received
        {
            echo = false;

            session.getPacketQueue().pushPacket(
                    ByteBuffer.wrap(
                            (sentPacket = new PacketUniversalPing(System.currentTimeMillis())).toBytes()));
        }
        else // timed out
        {
            session.setState(ServerPayload.State.TIMED_OUT);
        }
    }

    public void echo()
    {
        this.echo = true;
    }

    public PacketUniversalPing getSentPacket()
    {
        return sentPacket;
    }

    public PayloadSession getSession()
    {
        return session;
    }

    public static final long DEFAULT_INTERVAL = 1000;

    public static final long DEFAULT_TIME_OUT = 15000;

    private volatile PacketUniversalPing sentPacket;

    private volatile boolean echo;

    private volatile long lastTime;

    private final long interval;

    private final long timeOut;

    private final PayloadSession session;
}
