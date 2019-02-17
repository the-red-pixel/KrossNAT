package org.kucro3.krossnat.client;

import org.kucro3.krossnat.auth.AuthorizationToken;
import org.kucro3.krossnat.auth.AuthorizationTokenLoader;
import org.kucro3.krossnat.auth.AuthorizationTokenPool;
import org.kucro3.krossnat.client.payload.*;
import org.kucro3.krossnat.logger.LoggerFormatter;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args)
    {
        ConsoleHandler conLogHandler = new ConsoleHandler();
        conLogHandler.setFormatter(new LoggerFormatter());

        Logger clientLogger = Logger.getLogger("CLIENT");
        Logger instanceLogger = Logger.getLogger("NATINS");

        clientLogger.setUseParentHandlers(false);
        instanceLogger.setUseParentHandlers(false);

        clientLogger.addHandler(conLogHandler);
        instanceLogger.addHandler(conLogHandler);

        try {
            AuthorizationTokenPool pool = new AuthorizationTokenPool();

            File directory = new File(System.getProperties().getProperty("auth.token.directory", "tokens"));

            clientLogger.info("Starting to load auth tokens from directory: " + directory.getAbsolutePath());

            AuthorizationTokenLoader.loadFromDirectory(pool, directory);

            clientLogger.info(pool.getTokenCount() + " token(s) loaded.");

            AuthorizationToken token = pool.peek();

            clientLogger.info("Using token: " + token.getAKey());
            clientLogger.info("Launching client thread.");

            clientLogger.info("Constructing port table.");
            PortTable portTable = new PortTable();

            clientLogger.info("Constructing connection table.");
            ConnectionTable connectionTable = new ConnectionTable(portTable);

            ClientThread clientThread = new ClientThread(clientLogger, token, portTable, connectionTable);

            clientLogger.info("Construction instance payload.");
            InstanceThread instanceThread = new InstanceThread(instanceLogger, clientThread.getPayload(), connectionTable);

            clientThread.getPayload().setInstanceThread(instanceThread);
            clientThread.start();
            clientLogger.info("Client thread started.");

            instanceThread.start();
            clientLogger.info("Instance thread started.");

            ClientPayload payload = clientThread.getPayload();

            clientLogger.info("Waiting on state.");

            while(payload.getState() == ClientPayload.State.ZERO);

            while (payload.getState() != ClientPayload.State.ONLINE
                    && payload.getState() != ClientPayload.State.AKEY_AUTH_FAILURE
                    && payload.getState() != ClientPayload.State.BKEY_AUTH_FAILURE);

            clientLogger.info("State has been ONLINE. Starting port allocation.");

            if (payload.getState() == ClientPayload.State.ONLINE)
            {
                PortConfiguration.load(portTable);

                for (Integer port : portTable.getPorts())
                {
                    payload.pushPortRequest(port);
                }
            }
        } catch (Exception e) {
            clientLogger.severe("Failed to boot, exception occurred.");
            e.printStackTrace();
        }

    }
}
