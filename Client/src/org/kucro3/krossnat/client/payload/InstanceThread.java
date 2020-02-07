package org.kucro3.krossnat.client.payload;

import com.theredpixelteam.redtea.util.Pair;
import org.kucro3.krossnat.payload.PayloadProcess;

import java.io.IOException;
import java.nio.channels.*;
import java.util.UUID;
import java.util.logging.Logger;

public class InstanceThread extends Thread {
    public InstanceThread(Logger logger, ClientPayload payload, ConnectionTable connectionTable) throws IOException
    {
        super("ClientInstanceThread");
        this.payload = new InstancePayload(logger, payload, connectionTable);
        this.logger = logger;
        this.selector = Selector.open();
    }

    public InstancePayload getPayload()
    {
        return payload;
    }

    @Override
    public void start()
    {
        super.start();

        logger.info("Instance thread started.");
    }

    @Override
    public void run()
    {
        try {
            payload();
        } catch (CancelledKeyException e) {
            // ignored
        } catch (Exception e) {
            logger.severe("Instance payload stopped. Exception occurred.");
            e.printStackTrace();
        }
    }

    private void payload() throws Exception
    {
        while (!interrupted && payload.getPayload().getState().equals(ClientPayload.State.ONLINE))
            PayloadProcess.process(selector, payload);
    }

    @Override
    public void interrupt()
    {
        this.interrupted = true;
    }

    @Override
    public boolean isInterrupted()
    {
        return interrupted;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public Selector getSelector()
    {
        return selector;
    }

    public void establishConnection(int port, UUID uuid) throws IOException
    {
        ConnectionTable connections = payload.getConnectionTable();
        PortTable ports = connections.getPortTable();

        if (!ports.isOnline(port))
            throw new IOException("Port " + port + " not available");

        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        socketChannel.connect(ports.getPortAddress(port).getSilently());

        payload.taskQueue.addFirst(() -> {
            try {
                socketChannel.register(selector, SelectionKey.OP_CONNECT, Pair.of(port, uuid));
            } catch (ClosedChannelException e) {
                logger.warning("Selector task register failure.");
                e.printStackTrace();
            }
        });

        selector.wakeup();
    }

    private volatile boolean interrupted = false;

    private final Selector selector;

    private final Logger logger;

    private final InstancePayload payload;
}
