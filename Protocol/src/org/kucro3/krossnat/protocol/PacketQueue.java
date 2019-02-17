package org.kucro3.krossnat.protocol;

import com.theredpixelteam.redtea.util.Optional;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

public class PacketQueue {
    public PacketQueue()
    {
    }

    public boolean registerListener(PacketQueueListener listener)
    {
        return listeners.add(listener);
    }

    public boolean unregisterListener(PacketQueueListener listener)
    {
        return listeners.remove(listener);
    }

    public boolean hasPacket()
    {
        synchronized (packetQueue)
        {
            if (packetQueue.isEmpty())
                return false;

            while (!packetQueue.isEmpty() && packetQueue.getLast().remaining() == 0) {
                ByteBuffer packet = packetQueue.pollLast();

                for (PacketQueueListener listener : listeners)
                    listener.onPacketReadComplete(this, packet);
            }

            boolean isEmpty = packetQueue.isEmpty();

            if (isEmpty)
                for (PacketQueueListener listener : listeners)
                    listener.onQueueEmptied(this);

            return !isEmpty;
        }
    }

    public Optional<ByteBuffer> topPacket()
    {
        synchronized (packetQueue)
        {
            if (!hasPacket())
                return Optional.empty();

            return Optional.of(packetQueue.getLast());
        }
    }

    public ByteBuffer popPacket()
    {
        synchronized (packetQueue)
        {
            if (!hasPacket())
                throw new NoSuchElementException();

            ByteBuffer packet = packetQueue.pollLast();

            for (PacketQueueListener listener : listeners)
                listener.onPacketPop(this, packet);

            hasPacket();

            return packet;
        }
    }

    public void pushPacket(ByteBuffer buf)
    {
        synchronized (packetQueue)
        {
            if (!hasPacket())
                for (PacketQueueListener listener : listeners)
                    listener.onQueueWake(this);

            for (PacketQueueListener listener : listeners)
                listener.onPacketPush(this, buf);

            packetQueue.addFirst(buf);
        }
    }

    public int count()
    {
        synchronized (packetQueue)
        {
            hasPacket();

            return packetQueue.size();
        }
    }

    public void clear()
    {
        synchronized (packetQueue)
        {
            packetQueue.clear();

            for (PacketQueueListener listener : listeners)
                listener.onQueueEmptied(this);
        }
    }

    private final Set<PacketQueueListener> listeners = new HashSet<>();

    private final ConcurrentLinkedDeque<ByteBuffer> packetQueue = new ConcurrentLinkedDeque<>();
}
