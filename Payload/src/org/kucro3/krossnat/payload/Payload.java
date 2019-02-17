package org.kucro3.krossnat.payload;

import java.nio.channels.SelectionKey;

public interface Payload {
    public void handleConnect(SelectionKey key);

    public void handleAccept(SelectionKey key);

    public void handleRead(SelectionKey key);

    public void handleWrite(SelectionKey key);

    public void tick();

    public default void tickOnKey(SelectionKey key) {};
}
