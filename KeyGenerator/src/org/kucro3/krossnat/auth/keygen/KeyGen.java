package org.kucro3.krossnat.auth.keygen;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class KeyGen {
    private KeyGen()
    {
    }

    private static void writeUUID(UUID uuid, DataOutputStream dos) throws IOException
    {
        dos.writeLong(uuid.getMostSignificantBits());
        dos.writeLong(uuid.getLeastSignificantBits());
    }

    public static byte[] generate(UUID uuid, int bkey)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            writeUUID(uuid, dos);

            for (int j = 0; j < bkey; j++)
                writeUUID(UUID.randomUUID(), dos);
        } catch (IOException e) {
            // unused
            e.printStackTrace();
        }

        return baos.toByteArray();
    }
}
