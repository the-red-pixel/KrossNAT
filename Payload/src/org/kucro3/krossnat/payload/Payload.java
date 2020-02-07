package org.kucro3.krossnat.payload;

import org.kucro3.krossnat.payload.task.TaskQueue;

import java.nio.channels.SelectionKey;

public interface Payload {
    public void handleConnect(SelectionKey key);

    public void handleAccept(SelectionKey key);

    public void handleRead(SelectionKey key);

    public void handleWrite(SelectionKey key);

    public void tick();

    public default void tickOnKey(SelectionKey key) {}

    public default TaskQueue getTaskQueue()
    {
        return null;
    }
}
