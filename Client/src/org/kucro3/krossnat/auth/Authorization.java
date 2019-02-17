package org.kucro3.krossnat.auth;

import java.util.BitSet;
import java.util.Set;
import java.util.UUID;

public class Authorization {
    private Authorization()
    {
    }

    public static long computeMask(AuthorizationToken token, UUID[] receivedBKeys)
    {
        BitSet bitSet = new BitSet(64);

        Set<UUID> bKeySet = token.getBKeys();

        for (int i = 0; i < 64; i++)
            if (bKeySet.contains(receivedBKeys[i]))
                bitSet.set(i);

        return bitSet.toLongArray()[0];
    }
}
