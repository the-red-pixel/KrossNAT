package org.kucro3.krossnat.auth;

import java.io.*;
import java.util.HashSet;
import java.util.UUID;

public class AuthorizationTokenLoader {
    private AuthorizationTokenLoader()
    {
    }

    private static UUID readUUID(DataInputStream is) throws IOException
    {
        long most = is.readLong();
        long least = is.readLong();

        return new UUID(most, least);
    }

    public static void loadFromDirectory(AuthorizationTokenPool pool, File directory) throws IOException
    {
        File[] files = directory.listFiles();

        if (files == null)
            return;

        for (File file : files)
            if (!file.isDirectory())
                loadFile(pool, file);
    }

    public static void loadFile(AuthorizationTokenPool pool, File file) throws IOException
    {
        UUID aKey;
        HashSet<UUID> bKeys = new HashSet<>();

        BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
        DataInputStream dis = new DataInputStream(is);

        aKey = readUUID(dis);
        for (int i = 0; i < 64; i++)
            if (!bKeys.add(readUUID(dis)))
                throw new IOException("Duplicated bkeys: " + aKey);

        is.close();

        if (!pool.addToken(aKey, bKeys, () -> file.delete()))
            throw new IOException("Duplicated token: " + aKey);
    }
}
