package org.kucro3.krossnat.server;

import org.kucro3.krossnat.server.payload.PayloadSession;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Instance {
    public Instance(InstanceTable owner, PayloadSession session)
    {
        this.owner = owner;
        this.session = session;
    }

    public Set<Integer> getPorts()
    {
        return Collections.unmodifiableSet(ports);
    }

    boolean allocatePort0(int port, boolean callback)
    {
        if (callback)
            return owner.allocatePort(port, this);

        if (!ports.add(port))
            return false;

        return owner.thread.allocatePort(port, this, session);
    }

    public boolean allocatePort(int port)
    {
        return allocatePort0(port, true);
    }

    boolean freePort0(int port, boolean callback)
    {
        if (callback)
            return owner.freePort(port);
        else if (ports.remove(port))
        {
            connections.closeConnectionsOnPort(port, session);
            owner.thread.freePort(port);
            return true;
        }

        return false;
    }

    public void destroy()
    {
        for (int port : ports)
            freePort(port);

        ports.clear();
    }

    public boolean freePort(int port)
    {
        return freePort0(port, true);
    }

    public ConnectionTable getConnections()
    {
        return connections;
    }

    public InstanceTable getOwner()
    {
        return owner;
    }

    public PayloadSession getSession()
    {
        return session;
    }

    private final InstanceTable owner;

    private final PayloadSession session;

    private final Set<Integer> ports = new ConcurrentSkipListSet<>();

    private final ConnectionTable connections = new ConnectionTable(this);
}
