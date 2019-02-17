package org.kucro3.krossnat.client;

import org.kucro3.krossnat.client.payload.PortTable;

import java.net.InetSocketAddress;

public class PortConfiguration {
    private PortConfiguration()
    {
    }

    public static void load(PortTable portTable)
    {
        String cfg = System.getProperties().getProperty("client.ports");

        if (cfg == null)
            return;

        String[] cfgs = cfg.split(",");

        for (String str : cfgs)
        {
            String[] addressAndPort = str.split("=");

            if (addressAndPort.length != 2)
                continue;

            String address = addressAndPort[0];
            int remotePort = Integer.parseInt(addressAndPort[1]);

            String[] local = address.split(":");

            String host = local[0];
            int localPort = Integer.parseInt(local[1]);

            InetSocketAddress socketAddress = new InetSocketAddress(host, localPort);

            System.out.println("Loaded port config: " + socketAddress + " on " + remotePort);

            portTable.registerPort(remotePort, socketAddress);
        }
    }
}
