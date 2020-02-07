package org.kucro3.krossnat.protocol;

import com.theredpixelteam.redtea.util.Optional;

public enum PacketEnum {
    TOSERVER_AKEY_VERIFICATION          (0,  16,     Packet2ServerAKeyVerification::new),
    TOCLIENT_BKEY                       (1,  1024,   Packet2ClientBKey::new),
    TOSERVER_BKEY_MASK_VERIFICATION     (2,  8,      Packet2ServerBKeyMaskVerification::new),
    TOCLIENT_AUTH_RESULT                (3,  1,      Packet2ClientAuthResult::new),
    UNIVERSAL_DATA                      (4,  -1,     PacketUniversalData::new),
    CONNECTION_ESTABLISHED              (5,  20,     PacketConnectionEstablished::new),
    CONNECTION_CLOSED                   (6,  16,     PacketConnectionClosed::new),
    TOSERVER_PORT_ALLOCATION            (7,  4,      Packet2ServerPortAllocation::new),
    TOCLIENT_PORT_ALLOCATION_RESULT     (8,  1,      Packet2ClientPortAllocationResult::new),
    TOSERVER_PORT_FREE                  (9,  4,      Packet2ServerPortFree::new),
    UNIVERSAL_PING                      (10, 8,      PacketUniversalPing::new);

    PacketEnum(int code, int length, PacketConstructor constructor)
    {
        this.code = (byte) code;
        this.length = length;
        this.constructor = constructor;
    }

    public PacketConstructor getConstructor()
    {
        return constructor;
    }

    public byte getCode()
    {
        return this.code;
    }

    public int getLength()
    {
        return this.length;
    }

    public static Optional<PacketEnum> getType(int code)
    {
        if (code < 0 || code > PACKETS.length)
            return Optional.empty();

        return Optional.of(PACKETS[code]);
    }

    private final byte code;

    private final int length;

    private final PacketConstructor constructor;

    private static final PacketEnum[] PACKETS = {
            TOSERVER_AKEY_VERIFICATION,
            TOCLIENT_BKEY,
            TOSERVER_BKEY_MASK_VERIFICATION,
            TOCLIENT_AUTH_RESULT,
            UNIVERSAL_DATA,
            CONNECTION_ESTABLISHED,
            CONNECTION_CLOSED,
            TOSERVER_PORT_ALLOCATION,
            TOCLIENT_PORT_ALLOCATION_RESULT,
            TOSERVER_PORT_FREE,
            UNIVERSAL_PING
    };
}
