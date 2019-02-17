package org.kucro3.krossnat.payload;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class PayloadProcess {
    private PayloadProcess()
    {
    }

    public static void process(Selector selector, Payload payload) throws Exception
    {
        payload.tick();

        if (selector.select() == 0)
            return;

        Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

        while (iter.hasNext())
        {
            SelectionKey key = iter.next();

            if (key.isValid() && key.isAcceptable())
                payload.handleAccept(key);

            if (key.isValid() && key.isConnectable())
                payload.handleConnect(key);

            if (key.isValid() && key.isWritable())
                payload.handleWrite(key);

            if (key.isValid() && key.isReadable())
                payload.handleRead(key);

            payload.tickOnKey(key);

            iter.remove();
        }
    }
}
