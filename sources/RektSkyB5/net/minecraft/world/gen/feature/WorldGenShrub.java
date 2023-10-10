/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenTrees;

public class WorldGenShrub
extends WorldGenTrees {
    private final IBlockState leavesMetadata;
    private final IBlockState woodMetadata;

    public WorldGenShrub(IBlockState p_i46450_1_, IBlockState p_i46450_2_) {
        super(false);
        this.woodMetadata = p_i46450_1_;
        this.leavesMetadata = p_i46450_2_;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        Block block;
        while (((block = worldIn.getBlockState(position).getBlock()).getMaterial() == Material.air || block.getMaterial() == Material.leaves) && position.getY() > 0) {
            position = position.down();
        }
        Block block1 = worldIn.getBlockState(position).getBlock();
        if (block1 == Blocks.dirt || block1 == Blocks.grass) {
            position = position.up();
            this.setBlockAndNotifyAdequately(worldIn, position, this.woodMetadata);
            for (int i2 = position.getY(); i2 <= position.getY() + 2; ++i2) {
                int j2 = i2 - position.getY();
                int k2 = 2 - j2;
                for (int l2 = position.getX() - k2; l2 <= position.getX() + k2; ++l2) {
                    int i1 = l2 - position.getX();
                    for (int j1 = position.getZ() - k2; j1 <= position.getZ() + k2; ++j1) {
                        BlockPos blockpos;
                        int k1 = j1 - position.getZ();
                        if (Math.abs(i1) == k2 && Math.abs(k1) == k2 && rand.nextInt(2) == 0 || worldIn.getBlockState(blockpos = new BlockPos(l2, i2, j1)).getBlock().isFullBlock()) continue;
                        this.setBlockAndNotifyAdequately(worldIn, blockpos, this.leavesMetadata);
                    }
                }
            }
        }
        return true;
    }
}

