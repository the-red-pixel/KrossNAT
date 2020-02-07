package org.kucro3.krossnat.server.payload;

import com.theredpixelteam.redtea.util.Pair;
import org.kucro3.krossnat.payload.PayloadProcess;
import org.kucro3.krossnat.server.Instance;
import org.kucro3.krossnat.server.InstanceTable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

public class InstanceThread extends Thread {
    public InstanceThread(Logger logger, InstanceTable instanceTable) throws IOException
    {
        super("InstancePayloadThread");

        this.logger = logger;
        this.selector = Selector.open();
        this.payload = new InstancePayload(logger, this, instanceTable);
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
        } catch (Exception e) {
            logger.severe("Instance payload stopped. Exception occurred.");
            e.printStackTrace();
        }
    }

    private void payload() throws Exception
    {
        while (!interrupted)
            PayloadProcess.process(selector, payload);
    }

    public boolean allocatePort(int port, Instance instance, PayloadSession session)
    {
        try {
            ServerSocketChannel serverSocket = ServerSocketChannel.open();

            serverSocket.configureBlocking(false);
            serverSocket.socket().bind(new InetSocketAddress(port & 0xFFFF));

            taskQueue.addFirst(() -> {
                        try {
                            serverSocket.register(selector, SelectionKey.OP_ACCEPT, Pair.of(port, session));
                        } catch (ClosedChannelException e) {
                            logger.warning("Selector task register failure.");
                            e.printStackTrace();
                        }
                    });

            selector.wakeup();

            if (serverSockets.putIfAbsent(port, serverSocket) != null)
            {
                serverSocket.close();
                logger.severe("Concurrent failure. Duplicated sockets on port " + port + ".");

                return false;
            }

            logger.info("NAT Socket on port " + port + " opened.");

            return true;
        } catch (Exception e) {
            logger.severe("Exception occurred when allocating instance socket on port " + port + ".");
            e.printStackTrace();

            return false;
        }
    }

    public boolean freePort(int port)
    {
        ServerSocketChannel serverSocket = serverSockets.remove(port);

        if (serverSocket == null)
            return false;

        try {
            serverSocket.close();

            logger.info("NAT Socket on port " + port + " closed.");

            return true;
        } catch (Exception e) {
            logger.severe("Exception occurred when closing instance socket on port " + port + ".");
            e.printStackTrace();
        }

        return false;
    }

    public Logger getLogger()
    {
        return logger;
    }

    @Override
    public boolean isInterrupted()
    {
        return interrupted;
    }

    @Override
    public void interrupt()
    {
        interrupted = true;
    }

    private volatile boolean interrupted = false;

    final ConcurrentLinkedDeque<Runnable> taskQueue = new ConcurrentLinkedDeque<>();

    private final InstancePayload payload;

    private final ConcurrentHashMap<Integer, ServerSocketChannel> serverSockets = new ConcurrentHashMap<>();

    private final Selector selector;

    private final Logger logger;
}
