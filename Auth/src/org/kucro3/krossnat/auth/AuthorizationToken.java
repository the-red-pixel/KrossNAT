package org.kucro3.krossnat.auth;

import com.theredpixelteam.redtea.util.Optional;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public final class AuthorizationToken {
    public AuthorizationToken(UUID aKey, Set<UUID> bKeys)
    {
        this.aKey = aKey;
        this.bKeys = bKeys;
    }

    // only for internal use
    AuthorizationToken(UUID aKey)
    {
        this.aKey = aKey;
        this.bKeys = null;
    }

    public void setCleaner(AuthorizationTokenCleaner cleaner)
    {
        this.cleaner = cleaner;
    }

    public Optional<AuthorizationTokenCleaner> getCleaner()
    {
        return Optional.ofNullable(cleaner);
    }

    public void clean()
    {
        if (cleaner != null)
            cleaner.clean();
    }

    public UUID getAKey()
    {
        return aKey;
    }

    public Set<UUID> getBKeys()
    {
        return Collections.unmodifiableSet(bKeys);
    }

    public boolean containsBKey(UUID bKey)
    {
        return bKeys.contains(bKey);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null)
            return false;

        if (!(object instanceof AuthorizationToken))
            return false;

        AuthorizationToken obj = (AuthorizationToken) object;

        if (!aKey.equals(obj.aKey))
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        return aKey.hashCode();
    }

    private AuthorizationTokenCleaner cleaner;

    private final UUID aKey;

    private final Set<UUID> bKeys;
}
