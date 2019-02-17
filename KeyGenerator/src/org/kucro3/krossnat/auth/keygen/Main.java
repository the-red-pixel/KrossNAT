package org.kucro3.krossnat.auth.keygen;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws IOException
    {
        Properties properties = System.getProperties();

        String outputDirectory = properties.getProperty("keygen.output", "output");
        int mount = Integer.parseInt(properties.getProperty("keygen.mount", "1"));
        int bKeyMount = Integer.parseInt(properties.getProperty("keygen.bkey", "64"));

        File directory = new File(outputDirectory);

        if (!directory.exists() || !directory.isDirectory())
            directory.mkdir();

        for (int i = 0; i < mount; i++)
        {
            UUID uuid = UUID.randomUUID();

            File file = new File(outputDirectory, uuid.toString().replace("-", ""));
            file.createNewFile();

            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));

            os.write(KeyGen.generate(uuid, bKeyMount));

            os.flush();
            os.close();

            System.out.println(new StringBuilder("[")
                    .append(DATE_FORMAT.format(new Date()))
                    .append("] ")
                    .append("Generated keyset: ")
                    .append(uuid));
        }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
}
