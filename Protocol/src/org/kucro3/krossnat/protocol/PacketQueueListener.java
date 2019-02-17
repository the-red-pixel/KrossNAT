package org.kucro3.krossnat.protocol;

import java.nio.ByteBuffer;

public interface PacketQueueListener {
    public default void onPacketPush(PacketQueue queue, ByteBuffer buffer) {}

    public default void onPacketPop(PacketQueue queue, ByteBuffer buffer) {}

    public default void onPacketReadComplete(PacketQueue queue, ByteBuffer buffer) {}

    public default void onQueueEmptied(PacketQueue queue) {}

    public default void onQueueWake(PacketQueue queue) {}
}
