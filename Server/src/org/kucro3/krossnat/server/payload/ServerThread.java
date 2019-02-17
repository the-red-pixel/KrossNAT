package org.kucro3.krossnat.server.payload;

import org.kucro3.krossnat.auth.Authorization;
import org.kucro3.krossnat.payload.PayloadProcess;
import org.kucro3.krossnat.server.InstanceTable;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Logger;

public class ServerThread extends Thread {
    public ServerThread(Logger logger, Authorization auth, InstanceTable instanceTable)
    {
        super("ServerPayloadThread");

        this.logger = logger;
        this.authorization = auth;
        this.instanceTable = instanceTable;
        this.port = Integer.parseInt(System.getProperties().getProperty("server.port", "1444"));

        this.payload = new ServerPayload(logger, auth, instanceTable);
    }

    @Override
    public void start()
    {
        logger.info("Starting server thread on port " + port);

        super.start();
    }

    @Override
    public void run()
    {
        try {
            payload();
        } catch (Exception e) {
            logger.severe("Server payload stopped. Exception occurred.");
            e.printStackTrace();
        }
    }

    private void payload() throws Exception
    {
        Selector selector = Selector.open();

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        InetSocketAddress socket = new InetSocketAddress(port);
        serverSocketChannel.socket().bind(socket);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (!interrupted)
            PayloadProcess.process(selector, payload);
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

    public Logger getLogger()
    {
        return logger;
    }

    public Authorization getAuthorization()
    {
        return authorization;
    }

    public InstanceTable getInstanceTable()
    {
        return instanceTable;
    }

    private volatile boolean interrupted = false;

    private final ServerPayload payload;

    private final Authorization authorization;

    private final InstanceTable instanceTable;

    private final Logger logger;

    private final int port;
}
