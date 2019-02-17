package org.kucro3.krossnat.client.payload;

import com.theredpixelteam.redtea.util.Optional;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class PortTable {
    public PortTable()
    {
    }

    public boolean isPortInUse(int port)
    {
        return portMap.containsKey(port);
    }

    public boolean registerPort(int port, InetSocketAddress address)
    {
        return portMap.putIfAbsent(port, address) != null;
    }

    public boolean unregisterPort(int port)
    {
        if (portMap.remove(port) == null)
            return false;

        remove(port);

        return true;
    }

    public Optional<InetSocketAddress> getPortAddress(int port)
    {
        return Optional.ofNullable(portMap.get(port));
    }

    public boolean isOnline(int port)
    {
        return established.contains(port);
    }

    public boolean establish(int port)
    {
        if (!portMap.containsKey(port))
            return false;

        established.add(port);

        return true;
    }

    public Set<Integer> getPorts()
    {
        return Collections.unmodifiableSet(portMap.keySet());
    }

    public boolean remove(int port)
    {
        return established.remove(port);
    }

    private final Set<Integer> established = new ConcurrentSkipListSet<>();

    private final Map<Integer, InetSocketAddress> portMap = new ConcurrentHashMap<>();
}
