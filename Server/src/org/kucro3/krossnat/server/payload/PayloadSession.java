package org.kucro3.krossnat.server.payload;

import org.kucro3.krossnat.auth.AuthorizaionSession;
import org.kucro3.krossnat.auth.AuthorizationToken;
import org.kucro3.krossnat.protocol.PacketIterator;
import org.kucro3.krossnat.protocol.PacketQueue;
import org.kucro3.krossnat.server.Instance;

import java.util.UUID;

public class PayloadSession {
    public PayloadSession(UUID id)
    {
        this.id = id;
        this.iterator = new PacketIterator(true, true);
    }

    public ServerPayload.State getState()
    {
        return state;
    }

    public void setState(ServerPayload.State state)
    {
        this.state = state;
    }

    public PacketIterator getIterator()
    {
        return iterator;
    }

    public UUID getChannelID()
    {
        return id;
    }

    public PacketQueue getPacketQueue()
    {
        return packetQueue;
    }

    public AuthorizationToken getToken()
    {
        return token;
    }

    public void setToken(AuthorizationToken token)
    {
        this.token = token;
    }

    public AuthorizaionSession getAuthSession()
    {
        return session;
    }

    public void setAuthSession(AuthorizaionSession session)
    {
        this.session = session;
    }

    public Instance getInstance()
    {
        return instance;
    }

    public void setInstance(Instance instance)
    {
        this.instance = instance;
    }

    private Instance instance;

    private AuthorizaionSession session;

    private AuthorizationToken token;

    private final PacketQueue packetQueue = new PacketQueue();

    private final PacketIterator iterator;

    private final UUID id;

    private volatile ServerPayload.State state;
}
