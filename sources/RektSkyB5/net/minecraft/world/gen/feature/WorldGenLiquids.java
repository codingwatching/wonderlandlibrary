/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

public class WorldGenLiquids
extends WorldGenerator {
    private Block block;

    public WorldGenLiquids(Block p_i45465_1_) {
        this.block = p_i45465_1_;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (worldIn.getBlockState(position.up()).getBlock() != Blocks.stone) {
            return false;
        }
        if (worldIn.getBlockState(position.down()).getBlock() != Blocks.stone) {
            return false;
        }
        if (worldIn.getBlockState(position).getBlock().getMaterial() != Material.air && worldIn.getBlockState(position).getBlock() != Blocks.stone) {
            return false;
        }
        int i2 = 0;
        if (worldIn.getBlockState(position.west()).getBlock() == Blocks.stone) {
            ++i2;
        }
        if (worldIn.getBlockState(position.east()).getBlock() == Blocks.stone) {
            ++i2;
        }
        if (worldIn.getBlockState(position.north()).getBlock() == Blocks.stone) {
            ++i2;
        }
        if (worldIn.getBlockState(position.south()).getBlock() == Blocks.stone) {
            ++i2;
        }
        int j2 = 0;
        if (worldIn.isAirBlock(position.west())) {
            ++j2;
        }
        if (worldIn.isAirBlock(position.east())) {
            ++j2;
        }
        if (worldIn.isAirBlock(position.north())) {
            ++j2;
        }
        if (worldIn.isAirBlock(position.south())) {
            ++j2;
        }
        if (i2 == 3 && j2 == 1) {
            worldIn.setBlockState(position, this.block.getDefaultState(), 2);
            worldIn.forceBlockUpdateTick(this.block, position, rand);
        }
        return true;
    }
}

