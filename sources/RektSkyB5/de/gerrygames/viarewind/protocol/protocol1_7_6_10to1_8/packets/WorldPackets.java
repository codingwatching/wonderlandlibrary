/*
 * Decompiled with CFR 0.152.
 */
package de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.packets;

import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.CustomByteType;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.types.Chunk1_8Type;
import com.viaversion.viaversion.util.ChatColorUtil;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.Protocol1_7_6_10TO1_8;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.chunks.ChunkPacketTransformer;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.items.ReplacementRegistry1_7_6_10to1_8;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.storage.WorldBorder;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.types.Chunk1_7_10Type;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.types.Particle;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.types.Types1_7_6_10;
import de.gerrygames.viarewind.replacement.Replacement;
import de.gerrygames.viarewind.types.VarLongType;
import de.gerrygames.viarewind.utils.ChatUtil;
import de.gerrygames.viarewind.utils.PacketUtil;

public class WorldPackets {
    public static void register(Protocol1_7_6_10TO1_8 protocol) {
        protocol.registerClientbound(ClientboundPackets1_8.CHUNK_DATA, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.handler(packetWrapper -> {
                    ClientWorld world = packetWrapper.user().get(ClientWorld.class);
                    Chunk chunk = packetWrapper.read(new Chunk1_8Type(world));
                    packetWrapper.write(new Chunk1_7_10Type(world), chunk);
                    for (ChunkSection section : chunk.getSections()) {
                        if (section == null) continue;
                        for (int i2 = 0; i2 < section.getPaletteSize(); ++i2) {
                            int block = section.getPaletteEntry(i2);
                            int replacedBlock = ReplacementRegistry1_7_6_10to1_8.replace(block);
                            section.setPaletteEntry(i2, replacedBlock);
                        }
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.MULTI_BLOCK_CHANGE, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.map(Type.INT);
                this.map(Type.INT);
                this.handler(packetWrapper -> {
                    BlockChangeRecord[] records = packetWrapper.read(Type.BLOCK_CHANGE_RECORD_ARRAY);
                    packetWrapper.write(Type.SHORT, (short)records.length);
                    packetWrapper.write(Type.INT, records.length * 4);
                    for (BlockChangeRecord record : records) {
                        short data = (short)(record.getSectionX() << 12 | record.getSectionZ() << 8 | record.getY());
                        packetWrapper.write(Type.SHORT, data);
                        int replacedBlock = ReplacementRegistry1_7_6_10to1_8.replace(record.getBlockId());
                        packetWrapper.write(Type.SHORT, (short)replacedBlock);
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.BLOCK_CHANGE, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.handler(packetWrapper -> {
                    Position position = packetWrapper.read(Type.POSITION);
                    packetWrapper.write(Type.INT, position.getX());
                    packetWrapper.write(Type.UNSIGNED_BYTE, (short)position.getY());
                    packetWrapper.write(Type.INT, position.getZ());
                });
                this.handler(packetWrapper -> {
                    int meta;
                    int data = packetWrapper.read(Type.VAR_INT);
                    int blockId = data >> 4;
                    Replacement replace = ReplacementRegistry1_7_6_10to1_8.getReplacement(blockId, meta = data & 0xF);
                    if (replace != null) {
                        blockId = replace.getId();
                        meta = replace.replaceData(meta);
                    }
                    packetWrapper.write(Type.VAR_INT, blockId);
                    packetWrapper.write(Type.UNSIGNED_BYTE, (short)meta);
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.BLOCK_ACTION, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.handler(packetWrapper -> {
                    Position position = packetWrapper.read(Type.POSITION);
                    packetWrapper.write(Type.INT, position.getX());
                    packetWrapper.write(Type.SHORT, (short)position.getY());
                    packetWrapper.write(Type.INT, position.getZ());
                });
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.VAR_INT);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.BLOCK_BREAK_ANIMATION, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.map(Type.VAR_INT);
                this.handler(packetWrapper -> {
                    Position position = packetWrapper.read(Type.POSITION);
                    packetWrapper.write(Type.INT, position.getX());
                    packetWrapper.write(Type.INT, position.getY());
                    packetWrapper.write(Type.INT, position.getZ());
                });
                this.map(Type.BYTE);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.MAP_BULK_CHUNK, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.handler(ChunkPacketTransformer::transformChunkBulk);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.EFFECT, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.map(Type.INT);
                this.handler(packetWrapper -> {
                    Position position = packetWrapper.read(Type.POSITION);
                    packetWrapper.write(Type.INT, position.getX());
                    packetWrapper.write(Type.BYTE, (byte)position.getY());
                    packetWrapper.write(Type.INT, position.getZ());
                });
                this.map(Type.INT);
                this.map(Type.BOOLEAN);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.SPAWN_PARTICLE, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.handler(packetWrapper -> {
                    int particleId = packetWrapper.read(Type.INT);
                    Particle particle = Particle.find(particleId);
                    if (particle == null) {
                        particle = Particle.CRIT;
                    }
                    packetWrapper.write(Type.STRING, particle.name);
                    packetWrapper.read(Type.BOOLEAN);
                });
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.INT);
                this.handler(packetWrapper -> {
                    String name = packetWrapper.get(Type.STRING, 0);
                    Particle particle = Particle.find(name);
                    if (particle == Particle.ICON_CRACK || particle == Particle.BLOCK_CRACK || particle == Particle.BLOCK_DUST) {
                        int data;
                        int id = packetWrapper.read(Type.VAR_INT);
                        int n2 = data = particle == Particle.ICON_CRACK ? packetWrapper.read(Type.VAR_INT) : 0;
                        if (id >= 256 && id <= 422 || id >= 2256 && id <= 2267) {
                            particle = Particle.ICON_CRACK;
                        } else if (id >= 0 && id <= 164 || id >= 170 && id <= 175) {
                            if (particle == Particle.ICON_CRACK) {
                                particle = Particle.BLOCK_CRACK;
                            }
                        } else {
                            packetWrapper.cancel();
                            return;
                        }
                        name = particle.name + "_" + id + "_" + data;
                    }
                    packetWrapper.set(Type.STRING, 0, name);
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.UPDATE_SIGN, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.handler(packetWrapper -> {
                    Position position = packetWrapper.read(Type.POSITION);
                    packetWrapper.write(Type.INT, position.getX());
                    packetWrapper.write(Type.SHORT, (short)position.getY());
                    packetWrapper.write(Type.INT, position.getZ());
                });
                this.handler(packetWrapper -> {
                    for (int i2 = 0; i2 < 4; ++i2) {
                        String line = packetWrapper.read(Type.STRING);
                        line = ChatUtil.jsonToLegacy(line);
                        if ((line = ChatUtil.removeUnusedColor(line, '0')).length() > 15 && (line = ChatColorUtil.stripColor(line)).length() > 15) {
                            line = line.substring(0, 15);
                        }
                        packetWrapper.write(Type.STRING, line);
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.MAP_DATA, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.handler(packetWrapper -> {
                    packetWrapper.cancel();
                    int id = packetWrapper.read(Type.VAR_INT);
                    byte scale = packetWrapper.read(Type.BYTE);
                    int count = packetWrapper.read(Type.VAR_INT);
                    byte[] icons = new byte[count * 4];
                    for (int i2 = 0; i2 < count; ++i2) {
                        byte j2 = packetWrapper.read(Type.BYTE);
                        icons[i2 * 4] = (byte)(j2 >> 4 & 0xF);
                        icons[i2 * 4 + 1] = packetWrapper.read(Type.BYTE);
                        icons[i2 * 4 + 2] = packetWrapper.read(Type.BYTE);
                        icons[i2 * 4 + 3] = (byte)(j2 & 0xF);
                    }
                    int columns = packetWrapper.read(Type.UNSIGNED_BYTE).shortValue();
                    if (columns > 0) {
                        int rows = packetWrapper.read(Type.UNSIGNED_BYTE).shortValue();
                        short x2 = packetWrapper.read(Type.UNSIGNED_BYTE);
                        short z2 = packetWrapper.read(Type.UNSIGNED_BYTE);
                        byte[] data = packetWrapper.read(Type.BYTE_ARRAY_PRIMITIVE);
                        for (int column = 0; column < columns; ++column) {
                            byte[] columnData = new byte[rows + 3];
                            columnData[0] = 0;
                            columnData[1] = (byte)(x2 + column);
                            columnData[2] = (byte)z2;
                            for (int i3 = 0; i3 < rows; ++i3) {
                                columnData[i3 + 3] = data[column + i3 * columns];
                            }
                            PacketWrapper columnUpdate = PacketWrapper.create(52, null, packetWrapper.user());
                            columnUpdate.write(Type.VAR_INT, id);
                            columnUpdate.write(Type.SHORT, (short)columnData.length);
                            columnUpdate.write(new CustomByteType(columnData.length), columnData);
                            PacketUtil.sendPacket(columnUpdate, Protocol1_7_6_10TO1_8.class, true, true);
                        }
                    }
                    if (count > 0) {
                        byte[] iconData = new byte[count * 3 + 1];
                        iconData[0] = 1;
                        for (int i4 = 0; i4 < count; ++i4) {
                            iconData[i4 * 3 + 1] = (byte)(icons[i4 * 4] << 4 | icons[i4 * 4 + 3] & 0xF);
                            iconData[i4 * 3 + 2] = icons[i4 * 4 + 1];
                            iconData[i4 * 3 + 3] = icons[i4 * 4 + 2];
                        }
                        PacketWrapper iconUpdate = PacketWrapper.create(52, null, packetWrapper.user());
                        iconUpdate.write(Type.VAR_INT, id);
                        iconUpdate.write(Type.SHORT, (short)iconData.length);
                        CustomByteType customByteType = new CustomByteType(iconData.length);
                        iconUpdate.write(customByteType, iconData);
                        PacketUtil.sendPacket(iconUpdate, Protocol1_7_6_10TO1_8.class, true, true);
                    }
                    PacketWrapper scaleUpdate = PacketWrapper.create(52, null, packetWrapper.user());
                    scaleUpdate.write(Type.VAR_INT, id);
                    scaleUpdate.write(Type.SHORT, (short)2);
                    scaleUpdate.write(new CustomByteType(2), new byte[]{2, scale});
                    PacketUtil.sendPacket(scaleUpdate, Protocol1_7_6_10TO1_8.class, true, true);
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_8.BLOCK_ENTITY_DATA, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.handler(packetWrapper -> {
                    Position position = packetWrapper.read(Type.POSITION);
                    packetWrapper.write(Type.INT, position.getX());
                    packetWrapper.write(Type.SHORT, (short)position.getY());
                    packetWrapper.write(Type.INT, position.getZ());
                });
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.NBT, Types1_7_6_10.COMPRESSED_NBT);
            }
        });
        protocol.cancelClientbound(ClientboundPackets1_8.SERVER_DIFFICULTY);
        protocol.cancelClientbound(ClientboundPackets1_8.COMBAT_EVENT);
        protocol.registerClientbound(ClientboundPackets1_8.WORLD_BORDER, null, new PacketRemapper(){

            @Override
            public void registerMap() {
                this.handler(packetWrapper -> {
                    int action = packetWrapper.read(Type.VAR_INT);
                    WorldBorder worldBorder = packetWrapper.user().get(WorldBorder.class);
                    if (action == 0) {
                        worldBorder.setSize(packetWrapper.read(Type.DOUBLE));
                    } else if (action == 1) {
                        worldBorder.lerpSize(packetWrapper.read(Type.DOUBLE), packetWrapper.read(Type.DOUBLE), packetWrapper.read(VarLongType.VAR_LONG));
                    } else if (action == 2) {
                        worldBorder.setCenter(packetWrapper.read(Type.DOUBLE), packetWrapper.read(Type.DOUBLE));
                    } else if (action == 3) {
                        worldBorder.init(packetWrapper.read(Type.DOUBLE), packetWrapper.read(Type.DOUBLE), packetWrapper.read(Type.DOUBLE), packetWrapper.read(Type.DOUBLE), packetWrapper.read(VarLongType.VAR_LONG), packetWrapper.read(Type.VAR_INT), packetWrapper.read(Type.VAR_INT), packetWrapper.read(Type.VAR_INT));
                    } else if (action == 4) {
                        worldBorder.setWarningTime(packetWrapper.read(Type.VAR_INT));
                    } else if (action == 5) {
                        worldBorder.setWarningBlocks(packetWrapper.read(Type.VAR_INT));
                    }
                    packetWrapper.cancel();
                });
            }
        });
    }
}

