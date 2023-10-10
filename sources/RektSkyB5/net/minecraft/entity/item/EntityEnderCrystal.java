/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.entity.item;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;

public class EntityEnderCrystal
extends Entity {
    public int innerRotation;
    public int health;

    public EntityEnderCrystal(World worldIn) {
        super(worldIn);
        this.preventEntitySpawning = true;
        this.setSize(2.0f, 2.0f);
        this.health = 5;
        this.innerRotation = this.rand.nextInt(100000);
    }

    public EntityEnderCrystal(World worldIn, double p_i1699_2_, double p_i1699_4_, double p_i1699_6_) {
        this(worldIn);
        this.setPosition(p_i1699_2_, p_i1699_4_, p_i1699_6_);
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void entityInit() {
        this.dataWatcher.addObject(8, this.health);
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        ++this.innerRotation;
        this.dataWatcher.updateObject(8, this.health);
        int i2 = MathHelper.floor_double(this.posX);
        int j2 = MathHelper.floor_double(this.posY);
        int k2 = MathHelper.floor_double(this.posZ);
        if (this.worldObj.provider instanceof WorldProviderEnd && this.worldObj.getBlockState(new BlockPos(i2, j2, k2)).getBlock() != Blocks.fire) {
            this.worldObj.setBlockState(new BlockPos(i2, j2, k2), Blocks.fire.getDefaultState());
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tagCompound) {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tagCompund) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isEntityInvulnerable(source)) {
            return false;
        }
        if (!this.isDead && !this.worldObj.isRemote) {
            this.health = 0;
            if (this.health <= 0) {
                this.setDead();
                if (!this.worldObj.isRemote) {
                    this.worldObj.createExplosion(null, this.posX, this.posY, this.posZ, 6.0f, true);
                }
            }
        }
        return true;
    }
}

