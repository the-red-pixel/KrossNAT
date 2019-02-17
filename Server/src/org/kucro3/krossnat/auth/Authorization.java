package org.kucro3.krossnat.auth;

import com.theredpixelteam.redtea.util.Optional;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Authorization {
    public Authorization()
    {
    }

    public UUID[] generateMaskSource(Set<UUID> bKeys)
    {
        UUID[] maskSource = new UUID[64];

        Iterator<UUID> iter = bKeys.iterator();

        for (int i = 0; i < 64; i++)
            if (random.nextBoolean())
                maskSource[i] = iter.next();
            else
                maskSource[i] = UUID.randomUUID();

        return maskSource;
    }

    public Optional<AuthorizaionSession> createSession(UUID aKey)
    {
        if (!verifyAKey(aKey))
            return Optional.empty();

        AuthorizationToken token = pool.getToken(aKey).getSilently();
        AuthorizaionSession session = AuthorizaionSession.createSession(token);

        session.defineMaskSource(generateMaskSource(token.getBKeys()));

        return Optional.of(session);
    }

    public boolean verifyAKey(UUID uuid)
    {
        return pool.containsToken(uuid);
    }

    public boolean verifySession(AuthorizaionSession session)
    {
        if (!session.isMaskReceived())
            throw new IllegalStateException("Corrupt session state: mask not received.");

        return session.getMask().getSilently().equals(session.getExpectedMask().getSilently());
    }

    public boolean verifySessionAndRemove(AuthorizaionSession session)
    {
        boolean result;

        if (result = verifySession(session))
        {
            AuthorizationToken token = session.getToken();
            pool.removeTokenAndClean(session.getTokenAKey());
        }

        return result;
    }

    public AuthorizationTokenPool getTokenPool()
    {
        return pool;
    }

    private final AuthorizationTokenPool pool = new AuthorizationTokenPool();

    private final Random random = new Random();
}
