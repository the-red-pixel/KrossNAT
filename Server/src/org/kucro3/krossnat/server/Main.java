package org.kucro3.krossnat.server;

import org.kucro3.krossnat.auth.Authorization;
import org.kucro3.krossnat.auth.AuthorizationTokenLoader;
import org.kucro3.krossnat.auth.AuthorizationTokenPool;
import org.kucro3.krossnat.logger.LoggerFormatter;
import org.kucro3.krossnat.server.payload.ServerThread;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args)
    {
        ConsoleHandler conLogHandler = new ConsoleHandler();
        conLogHandler.setFormatter(new LoggerFormatter());

        FileHandler fileHandler = null;
        try {
            fileHandler = new FileHandler("log.log");
            fileHandler.setFormatter(new LoggerFormatter());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger bootLogger = Logger.getLogger("BOOT");
        Logger serverThreadLogger = Logger.getLogger("SERVER");
        Logger instanceThreadLogger = Logger.getLogger("NATINS");

        bootLogger.setUseParentHandlers(false);
        serverThreadLogger.setUseParentHandlers(false);
        instanceThreadLogger.setUseParentHandlers(false);

        bootLogger.addHandler(conLogHandler);
        serverThreadLogger.addHandler(conLogHandler);
        instanceThreadLogger.addHandler(conLogHandler);

        if (fileHandler != null)
        {
            bootLogger.addHandler(fileHandler);
            serverThreadLogger.addHandler(fileHandler);
            instanceThreadLogger.addHandler(fileHandler);
        }

        Authorization authorization = new Authorization();
        AuthorizationTokenPool pool = authorization.getTokenPool();

        try {
            File directory = new File(System.getProperties().getProperty("auth.token.directory", "tokens"));

            bootLogger.info("Loading tokens from directory: " + directory.getAbsolutePath());

            AuthorizationTokenLoader.loadFromDirectory(pool, directory);

            bootLogger.info(pool.getTokenCount() + " token(s) loaded.");
        } catch (Exception e) {
            bootLogger.severe("Failed to load auth tokens. Exception occurred.");
            e.printStackTrace();

            return;
        }

        InstanceTable instanceTable;
        try {
            bootLogger.info("Constructing NAT instance table.");
            instanceTable = new InstanceTable(instanceThreadLogger);

        } catch (IOException e) {
            bootLogger.info("Boot failure: Failed to create instance table.");
            e.printStackTrace();

            return;
        }

        ServerThread serverThread = new ServerThread(serverThreadLogger, authorization, instanceTable);

        bootLogger.info("Starting server thread.");

        serverThread.start();

        bootLogger.info("Server thread started.");

        bootLogger.info("Starting NAT instance thread.");

        instanceTable.thread.start();

        bootLogger.info("NAT instance thread started.");
    }

    private static ServerThread serverThread;
}
