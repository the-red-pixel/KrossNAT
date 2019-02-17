package org.kucro3.krossnat.protocol;

import java.nio.channels.SelectionKey;

public class SelectionKeyEncouragementPacketQueueListener implements PacketQueueListener {
    public SelectionKeyEncouragementPacketQueueListener(SelectionKey key)
    {
        this.key = key;
    }

    @Override
    public void onQueueEmptied(PacketQueue queue)
    {
        if (!checkValid(queue))
            return;

        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
    }

    @Override
    public void onQueueWake(PacketQueue queue)
    {
        if (!checkValid(queue))
            return;

        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        key.selector().wakeup();
    }

    boolean checkValid(PacketQueue queue)
    {
        if (!key.isValid())
        {
            queue.unregisterListener(this);
            return false;
        }

        return true;
    }

    public SelectionKey getKey()
    {
        return key;
    }

    private final SelectionKey key;
}
