package org.kucro3.krossnat.auth;

import com.theredpixelteam.redtea.util.Optional;

import java.util.*;

public class AuthorizationTokenPool {
    public AuthorizationTokenPool()
    {
    }

    public boolean containsToken(UUID uuid)
    {
        return tokens.containsKey(uuid);
    }

    public boolean removeToken(UUID uuid)
    {
        return tokens.remove(uuid) != null;
    }

    public boolean removeTokenAndClean(UUID uuid)
    {
        AuthorizationToken token = tokens.remove(uuid);

        if (token == null)
            return false;

        token.clean();

        return true;
    }

    public boolean addToken(UUID aKey, Set<UUID> bKeys)
    {
        return addToken(new AuthorizationToken(aKey, bKeys));
    }

    public boolean addToken(UUID aKey, Set<UUID> bKeys, AuthorizationTokenCleaner cleaner)
    {
        AuthorizationToken token = new AuthorizationToken(aKey, bKeys);

        token.setCleaner(cleaner);

        return addToken(token);
    }

    boolean addToken(AuthorizationToken token)
    {
        return tokens.putIfAbsent(token.getAKey(), token) == null;
    }

    public Optional<AuthorizationToken> getToken(UUID aKey)
    {
        return Optional.ofNullable(tokens.get(aKey));
    }

    public Optional<Set<UUID>> getBKeys(UUID aKey)
    {
        AuthorizationToken token = tokens.get(aKey);

        if (token == null)
            return Optional.empty();

        return Optional.of(token.getBKeys());
    }

    public int getTokenCount()
    {
        return tokens.size();
    }

    public AuthorizationToken peek()
    {
        if (tokens.isEmpty())
            throw new NoSuchElementException("Token required");

        return tokens.values().iterator().next();
    }

    private final Map<UUID, AuthorizationToken> tokens = new HashMap<>();
}
