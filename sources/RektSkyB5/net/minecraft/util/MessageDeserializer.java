/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.util.List;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class MessageDeserializer
extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger();
    private static final Marker RECEIVED_PACKET_MARKER = MarkerManager.getMarker("PACKET_RECEIVED", NetworkManager.logMarkerPackets);
    private final EnumPacketDirection direction;

    public MessageDeserializer(EnumPacketDirection direction) {
        this.direction = direction;
    }

    @Override
    protected void decode(ChannelHandlerContext p_decode_1_, ByteBuf p_decode_2_, List<Object> p_decode_3_) throws IOException, InstantiationException, IllegalAccessException, Exception {
        if (p_decode_2_.readableBytes() != 0) {
            PacketBuffer packetbuffer = new PacketBuffer(p_decode_2_);
            int i2 = packetbuffer.readVarIntFromBuffer();
            Packet packet = p_decode_1_.channel().attr(NetworkManager.attrKeyConnectionState).get().getPacket(this.direction, i2);
            if (packet == null) {
                throw new IOException("Bad packet id " + i2);
            }
            packet.readPacketData(packetbuffer);
            if (packetbuffer.readableBytes() > 0) {
                throw new IOException("Packet " + p_decode_1_.channel().attr(NetworkManager.attrKeyConnectionState).get().getId() + "/" + i2 + " (" + packet.getClass().getSimpleName() + ") was larger than I expected, found " + packetbuffer.readableBytes() + " bytes extra whilst reading packet " + i2);
            }
            p_decode_3_.add(packet);
            if (logger.isDebugEnabled()) {
                logger.debug(RECEIVED_PACKET_MARKER, " IN: [{}:{}] {}", new Object[]{p_decode_1_.channel().attr(NetworkManager.attrKeyConnectionState).get(), i2, packet.getClass().getName()});
            }
        }
    }
}

