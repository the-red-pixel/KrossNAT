package org.kucro3.krossnat.auth;

import com.theredpixelteam.redtea.util.Optional;

import java.util.BitSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuthorizaionSession {
    private AuthorizaionSession(AuthorizationToken token)
    {
        this.token = Objects.requireNonNull(token, "null token");
    }

    public static AuthorizaionSession createSession(AuthorizationToken token)
    {
        AuthorizaionSession session = new AuthorizaionSession(token);

        session.maskSourceLock.lock();
        session.maskLock.lock();

        return session;
    }

    public AuthorizationToken getToken()
    {
        return token;
    }

    public UUID getTokenAKey()
    {
        return token.getAKey();
    }

    public void defineMaskSource(UUID[] maskSource)
    {
        if (maskSource.length != MASK_SOURCE_LENGTH)
            throw new IllegalArgumentException("Corrupt mask source length.");

        if (isMaskSourceGenerated())
            throw new IllegalStateException("Mask source already generated.");

        this.maskSource = maskSource;

        BitSet bitSet = new BitSet(MASK_SOURCE_LENGTH);

        // compute mask
        for (int i = 0; i < MASK_SOURCE_LENGTH; i++)
            if (this.token.containsBKey(maskSource[i]))
                bitSet.set(i);

        this.expectedMask = bitSet;
    }

    public void receiveMask(long mask)
    {
        receiveMask(BitSet.valueOf(new long[] {mask}));
    }

    public void receiveMask(BitSet bitSet)
    {
        if (!isMaskSourceGenerated())
            throw new IllegalStateException("Mask source not generated.");

        if (isMaskReceived())
            throw new IllegalStateException("Mask already received.");

        this.mask = bitSet;
    }

    public void waitForMaskSource()
    {
        while (!isMaskSourceGenerated());
    }

    public boolean waitForMaskSource(long timeout) throws InterruptedException
    {
        if (isMaskSourceGenerated())
            return true;

        if (maskSourceLock.tryLock(timeout, TimeUnit.MILLISECONDS))
            maskSourceLock.unlock();

        return isMaskSourceGenerated();
    }

    public void waitForMask()
    {
        while (!isMaskReceived());
    }

    public boolean waitForMask(long timeout) throws InterruptedException
    {
        if (isMaskReceived())
            return true;

        if (maskLock.tryLock(timeout, TimeUnit.MILLISECONDS))
            maskLock.unlock();

        return isMaskReceived();
    }

    public boolean isMaskSourceGenerated()
    {
        return this.maskSource != null;
    }

    public boolean isMaskReceived()
    {
        return this.mask != null;
    }

    public Optional<UUID[]> getMaskSource()
    {
        return Optional.ofNullable(this.maskSource);
    }

    public Optional<BitSet> getMask()
    {
        return Optional.ofNullable(this.mask);
    }

    public Optional<BitSet> getExpectedMask()
    {
        return Optional.ofNullable(expectedMask);
    }

    public static final int MASK_SOURCE_LENGTH = 64;

    private final Lock maskSourceLock = new ReentrantLock();

    private final Lock maskLock = new ReentrantLock();

    private final AuthorizationToken token;

    private volatile UUID[] maskSource;

    private volatile BitSet mask;

    private volatile BitSet expectedMask;
}
