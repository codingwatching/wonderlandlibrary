/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

public final class SpawnerAnimals {
    private static final int MOB_COUNT_DIV = (int)Math.pow(17.0, 2.0);
    private final Set<ChunkCoordIntPair> eligibleChunksForSpawning = Sets.newHashSet();

    public int findChunksForSpawning(WorldServer p_77192_1_, boolean spawnHostileMobs, boolean spawnPeacefulMobs, boolean p_77192_4_) {
        if (!spawnHostileMobs && !spawnPeacefulMobs) {
            return 0;
        }
        this.eligibleChunksForSpawning.clear();
        int i2 = 0;
        for (EntityPlayer entityplayer : p_77192_1_.playerEntities) {
            if (entityplayer.isSpectator()) continue;
            int j2 = MathHelper.floor_double(entityplayer.posX / 16.0);
            int k2 = MathHelper.floor_double(entityplayer.posZ / 16.0);
            int l2 = 8;
            for (int i1 = -l2; i1 <= l2; ++i1) {
                for (int j1 = -l2; j1 <= l2; ++j1) {
                    boolean flag = i1 == -l2 || i1 == l2 || j1 == -l2 || j1 == l2;
                    ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i1 + j2, j1 + k2);
                    if (this.eligibleChunksForSpawning.contains(chunkcoordintpair)) continue;
                    ++i2;
                    if (flag || !p_77192_1_.getWorldBorder().contains(chunkcoordintpair)) continue;
                    this.eligibleChunksForSpawning.add(chunkcoordintpair);
                }
            }
        }
        int i4 = 0;
        BlockPos blockpos2 = p_77192_1_.getSpawnPoint();
        for (EnumCreatureType enumcreaturetype : EnumCreatureType.values()) {
            int k4;
            int j4;
            if (enumcreaturetype.getPeacefulCreature() && !spawnPeacefulMobs || !enumcreaturetype.getPeacefulCreature() && !spawnHostileMobs || enumcreaturetype.getAnimal() && !p_77192_4_ || (j4 = p_77192_1_.countEntities(enumcreaturetype.getCreatureClass())) > (k4 = enumcreaturetype.getMaxNumberOfCreature() * i2 / MOB_COUNT_DIV)) continue;
            block6: for (ChunkCoordIntPair chunkcoordintpair1 : this.eligibleChunksForSpawning) {
                BlockPos blockpos = SpawnerAnimals.getRandomChunkPosition(p_77192_1_, chunkcoordintpair1.chunkXPos, chunkcoordintpair1.chunkZPos);
                int k1 = blockpos.getX();
                int l1 = blockpos.getY();
                int i22 = blockpos.getZ();
                Block block = p_77192_1_.getBlockState(blockpos).getBlock();
                if (block.isNormalCube()) continue;
                int j2 = 0;
                block7: for (int k2 = 0; k2 < 3; ++k2) {
                    int l2 = k1;
                    int i3 = l1;
                    int j3 = i22;
                    int k3 = 6;
                    BiomeGenBase.SpawnListEntry biomegenbase$spawnlistentry = null;
                    IEntityLivingData ientitylivingdata = null;
                    for (int l3 = 0; l3 < 4; ++l3) {
                        EntityLiving entityliving;
                        BlockPos blockpos1 = new BlockPos(l2 += p_77192_1_.rand.nextInt(k3) - p_77192_1_.rand.nextInt(k3), i3 += p_77192_1_.rand.nextInt(1) - p_77192_1_.rand.nextInt(1), j3 += p_77192_1_.rand.nextInt(k3) - p_77192_1_.rand.nextInt(k3));
                        float f2 = (float)l2 + 0.5f;
                        float f1 = (float)j3 + 0.5f;
                        if (p_77192_1_.isAnyPlayerWithinRangeAt(f2, i3, f1, 24.0) || !(blockpos2.distanceSq(f2, i3, f1) >= 576.0)) continue;
                        if (biomegenbase$spawnlistentry == null && (biomegenbase$spawnlistentry = p_77192_1_.getSpawnListEntryForTypeAt(enumcreaturetype, blockpos1)) == null) continue block7;
                        if (!p_77192_1_.canCreatureTypeSpawnHere(enumcreaturetype, biomegenbase$spawnlistentry, blockpos1) || !SpawnerAnimals.canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(biomegenbase$spawnlistentry.entityClass), p_77192_1_, blockpos1)) continue;
                        try {
                            entityliving = biomegenbase$spawnlistentry.entityClass.getConstructor(World.class).newInstance(p_77192_1_);
                        }
                        catch (Exception exception) {
                            exception.printStackTrace();
                            return i4;
                        }
                        entityliving.setLocationAndAngles(f2, i3, f1, p_77192_1_.rand.nextFloat() * 360.0f, 0.0f);
                        if (entityliving.getCanSpawnHere() && entityliving.isNotColliding()) {
                            ientitylivingdata = entityliving.onInitialSpawn(p_77192_1_.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);
                            if (entityliving.isNotColliding()) {
                                ++j2;
                                p_77192_1_.spawnEntityInWorld(entityliving);
                            }
                            if (j2 >= entityliving.getMaxSpawnedInChunk()) continue block6;
                        }
                        i4 += j2;
                    }
                }
            }
        }
        return i4;
    }

    protected static BlockPos getRandomChunkPosition(World worldIn, int x2, int z2) {
        Chunk chunk = worldIn.getChunkFromChunkCoords(x2, z2);
        int i2 = x2 * 16 + worldIn.rand.nextInt(16);
        int j2 = z2 * 16 + worldIn.rand.nextInt(16);
        int k2 = MathHelper.func_154354_b(chunk.getHeight(new BlockPos(i2, 0, j2)) + 1, 16);
        int l2 = worldIn.rand.nextInt(k2 > 0 ? k2 : chunk.getTopFilledSegment() + 16 - 1);
        return new BlockPos(i2, l2, j2);
    }

    public static boolean canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType p_180267_0_, World worldIn, BlockPos pos) {
        if (!worldIn.getWorldBorder().contains(pos)) {
            return false;
        }
        Block block = worldIn.getBlockState(pos).getBlock();
        if (p_180267_0_ == EntityLiving.SpawnPlacementType.IN_WATER) {
            return block.getMaterial().isLiquid() && worldIn.getBlockState(pos.down()).getBlock().getMaterial().isLiquid() && !worldIn.getBlockState(pos.up()).getBlock().isNormalCube();
        }
        BlockPos blockpos = pos.down();
        if (!World.doesBlockHaveSolidTopSurface(worldIn, blockpos)) {
            return false;
        }
        Block block1 = worldIn.getBlockState(blockpos).getBlock();
        boolean flag = block1 != Blocks.bedrock && block1 != Blocks.barrier;
        return flag && !block.isNormalCube() && !block.getMaterial().isLiquid() && !worldIn.getBlockState(pos.up()).getBlock().isNormalCube();
    }

    public static void performWorldGenSpawning(World worldIn, BiomeGenBase p_77191_1_, int p_77191_2_, int p_77191_3_, int p_77191_4_, int p_77191_5_, Random p_77191_6_) {
        List<BiomeGenBase.SpawnListEntry> list = p_77191_1_.getSpawnableList(EnumCreatureType.CREATURE);
        if (!list.isEmpty()) {
            while (p_77191_6_.nextFloat() < p_77191_1_.getSpawningChance()) {
                BiomeGenBase.SpawnListEntry biomegenbase$spawnlistentry = WeightedRandom.getRandomItem(worldIn.rand, list);
                int i2 = biomegenbase$spawnlistentry.minGroupCount + p_77191_6_.nextInt(1 + biomegenbase$spawnlistentry.maxGroupCount - biomegenbase$spawnlistentry.minGroupCount);
                IEntityLivingData ientitylivingdata = null;
                int j2 = p_77191_2_ + p_77191_6_.nextInt(p_77191_4_);
                int k2 = p_77191_3_ + p_77191_6_.nextInt(p_77191_5_);
                int l2 = j2;
                int i1 = k2;
                for (int j1 = 0; j1 < i2; ++j1) {
                    boolean flag = false;
                    for (int k1 = 0; !flag && k1 < 4; ++k1) {
                        BlockPos blockpos = worldIn.getTopSolidOrLiquidBlock(new BlockPos(j2, 0, k2));
                        if (SpawnerAnimals.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, worldIn, blockpos)) {
                            EntityLiving entityliving;
                            try {
                                entityliving = biomegenbase$spawnlistentry.entityClass.getConstructor(World.class).newInstance(worldIn);
                            }
                            catch (Exception exception) {
                                exception.printStackTrace();
                                continue;
                            }
                            entityliving.setLocationAndAngles((float)j2 + 0.5f, blockpos.getY(), (float)k2 + 0.5f, p_77191_6_.nextFloat() * 360.0f, 0.0f);
                            worldIn.spawnEntityInWorld(entityliving);
                            ientitylivingdata = entityliving.onInitialSpawn(worldIn.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);
                            flag = true;
                        }
                        j2 += p_77191_6_.nextInt(5) - p_77191_6_.nextInt(5);
                        k2 += p_77191_6_.nextInt(5) - p_77191_6_.nextInt(5);
                        while (j2 < p_77191_2_ || j2 >= p_77191_2_ + p_77191_4_ || k2 < p_77191_3_ || k2 >= p_77191_3_ + p_77191_4_) {
                            j2 = l2 + p_77191_6_.nextInt(5) - p_77191_6_.nextInt(5);
                            k2 = i1 + p_77191_6_.nextInt(5) - p_77191_6_.nextInt(5);
                        }
                    }
                }
            }
        }
    }
}

