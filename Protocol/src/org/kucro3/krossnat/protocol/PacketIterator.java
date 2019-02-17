package org.kucro3.krossnat.protocol;

import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;

public class PacketIterator {
    public PacketIterator()
    {
        this(true, false);
    }

    public PacketIterator(boolean usingQueue, boolean cleanOnRemove)
    {
        this.cleanOnRemove = cleanOnRemove;
    }

    public void write(byte[] data)
    {
        deque.addLast(ByteBuffer.wrap(data));
    }

    public void write(ByteBuffer buffer)
    {
        deque.addLast(buffer);
    }

    boolean ensure(int requiredLen)
    {
        int rmnLen = 0;

        for (ByteBuffer buf : deque)
        {
            rmnLen += buf.remaining();

            if (rmnLen >= requiredLen)
                return true;
        }

        return false;
    }

    byte[] next(int len)
    {
        if (len == 0)
            return new byte[0];

        if (len < 0)
            throw new IllegalArgumentException("Len: " + len);

        if (!ensure(len))
            return null;

        int ptr = 0;
        byte[] byts = new byte[len];

        while (ptr < len)
        {
            ByteBuffer buf = deque.getFirst();

            if (buf.remaining() < (len - ptr))
            {
                int remaining = buf.remaining();

                buf.get(byts, ptr, remaining);
                ptr += remaining;

                deque.removeFirst();

                if (buf instanceof DirectBuffer)
                    ((DirectBuffer) buf).cleaner().clean();

                continue;
            }

            buf.get(byts, ptr, len - ptr);
            break;
        }

        return byts;
    }

    Integer nextInt()
    {
        byte[] byts = next(4);

        if (byts == null)
            return null;

        int value = 0;

        value |= (byts[3] & 0xFF);
        value |= (byts[2] & 0xFF) << 8;
        value |= (byts[1] & 0xFF) << 16;
        value |= (byts[0] & 0xFF) << 24;

        return value;
    }

    public synchronized boolean hasNext()
    {
        if (last != null)
            return true;

        if (currentType == null)
        {
            if (deque.isEmpty())
                return false;

            int type;
            byte[] byts = next(1);

            if (byts == null)
                return false;

            this.currentType = PacketEnum.getType(type = (byts[0] & 0xFF))
                    .orElseThrow(() -> new IllegalStateException("Unknown packet type: " + type));
        }

        if (requiredLen == null || requiredLen.equals(-1))
        {
            requiredLen = currentType.getLength();

            if (requiredLen.equals(-1)) {
                Integer intObject = nextInt();

                if (intObject == null)
                    return false;

                requiredLen = intObject;
            }
        }

        byte[] byts = next(requiredLen);

        if (byts == null)
            return false;

        Packet packet = currentType.getConstructor().construct();

        packet.read(ByteBuffer.wrap(byts), requiredLen);

        this.last = packet;

        return true;
    }

    public synchronized Packet next()
    {
        if (this.last == null)
            throw new NoSuchElementException("no packet remaining");

        Packet last = this.last;
        this.last = null;
        this.currentType = null;
        this.requiredLen = null;

        return last;
    }

    private PacketEnum currentType;

    private Integer requiredLen;

    private Packet last;

    private final boolean cleanOnRemove;

    private volatile ConcurrentLinkedDeque<ByteBuffer> deque = new ConcurrentLinkedDeque<>();
}
