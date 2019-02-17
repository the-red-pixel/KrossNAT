package org.kucro3.krossnat.server;

import com.theredpixelteam.redtea.util.Optional;
import org.kucro3.krossnat.server.payload.InstanceThread;
import org.kucro3.krossnat.server.payload.PayloadSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class InstanceTable {
    public InstanceTable(Logger logger) throws IOException
    {
        this.thread = new InstanceThread(logger, this);
    }

    public Optional<Instance> getInstanceOnPort(int port)
    {
        return Optional.ofNullable(ports.get(port));
    }

    public boolean allocatePort(int port, Instance instance)
    {
        if (ports.putIfAbsent(port, instance) != null)
            return false;

        if (!instance.allocatePort0(port, false))
            throw new IllegalStateException("Port congestion");

        return true;
    }

    public boolean freePort(int port)
    {
        Instance instance = ports.remove(port);

        if (instance == null)
            return false;

        instance.freePort0(port, false);
        return true;
    }

    public boolean isPortFree(int port)
    {
        return !ports.containsKey(port);
    }

    public Set<Integer> getPortsInUse()
    {
        return Collections.unmodifiableSet(ports.keySet());
    }

    public Map<Integer, Instance> getAllPorts()
    {
        return Collections.unmodifiableMap(ports);
    }

    public InstanceThread getPayload()
    {
        return thread;
    }

    final InstanceThread thread;

    private final Map<Integer, Instance> ports = new ConcurrentHashMap<>(65535);
}
