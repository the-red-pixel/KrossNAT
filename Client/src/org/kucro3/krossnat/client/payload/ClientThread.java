package org.kucro3.krossnat.client.payload;

import org.kucro3.krossnat.auth.AuthorizationToken;
import org.kucro3.krossnat.payload.PayloadProcess;
import org.kucro3.krossnat.protocol.SelectionKeyEncouragementPacketQueueListener;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

public class ClientThread extends Thread {
    public ClientThread(Logger logger, AuthorizationToken token, PortTable portTable, ConnectionTable connectionTable)
    {
        super("ClientPayloadThread");
        this.logger = logger;
        this.payload = new ClientPayload(logger, token, portTable, connectionTable);
    }

    @Override
    public void start()
    {
        super.start();

        logger.info("Client thread started.");
    }

    @Override
    public void run()
    {
        try {
            payload();
        } catch (Exception e) {
            logger.severe("Client payload stopped. Exception occurred.");
            e.printStackTrace();
        }
    }

    private void payload() throws Exception
    {
        String serverAddress = System.getProperties().getProperty("server.address");
        short serverPort = Short.parseShort(System.getProperties().getProperty("server.port", "1444"));

        if (serverAddress == null)
            throw new IllegalStateException("Server address required (property server.address)");

        SocketChannel socket = SocketChannel.open();
        socket.configureBlocking(false);

        logger.info("Connecting to server: " + serverAddress + ":" + serverPort);

        try {
            if (!socket.connect(new InetSocketAddress(serverAddress, serverPort)))
                while (!socket.finishConnect());
        } catch (Exception e) {
            logger.severe("Failed to connect to server: " + e.getMessage());
            System.exit(0);
            return;
        }

        logger.info("Connected to server, starting authorization.");

        Selector selector = Selector.open();
        SelectionKey key = socket.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);

        payload.getPacketQueue().registerListener(new SelectionKeyEncouragementPacketQueueListener(key));

        while (!interrupted && socket.isOpen())
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

    public ClientPayload getPayload()
    {
        return payload;
    }

    private volatile boolean interrupted = false;

    private final ClientPayload payload;

    private final Logger logger;
}
