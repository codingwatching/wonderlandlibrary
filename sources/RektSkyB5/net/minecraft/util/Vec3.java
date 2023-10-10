/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3i;

public class Vec3 {
    public final double xCoord;
    public final double yCoord;
    public final double zCoord;

    public Vec3(double x2, double y2, double z2) {
        if (x2 == -0.0) {
            x2 = 0.0;
        }
        if (y2 == -0.0) {
            y2 = 0.0;
        }
        if (z2 == -0.0) {
            z2 = 0.0;
        }
        this.xCoord = x2;
        this.yCoord = y2;
        this.zCoord = z2;
    }

    public Vec3(Vec3i p_i46377_1_) {
        this(p_i46377_1_.getX(), p_i46377_1_.getY(), p_i46377_1_.getZ());
    }

    public Vec3 subtractReverse(Vec3 vec) {
        return new Vec3(vec.xCoord - this.xCoord, vec.yCoord - this.yCoord, vec.zCoord - this.zCoord);
    }

    public Vec3 normalize() {
        double d0 = MathHelper.sqrt_double(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
        return d0 < 1.0E-4 ? new Vec3(0.0, 0.0, 0.0) : new Vec3(this.xCoord / d0, this.yCoord / d0, this.zCoord / d0);
    }

    public double dotProduct(Vec3 vec) {
        return this.xCoord * vec.xCoord + this.yCoord * vec.yCoord + this.zCoord * vec.zCoord;
    }

    public Vec3 crossProduct(Vec3 vec) {
        return new Vec3(this.yCoord * vec.zCoord - this.zCoord * vec.yCoord, this.zCoord * vec.xCoord - this.xCoord * vec.zCoord, this.xCoord * vec.yCoord - this.yCoord * vec.xCoord);
    }

    public Vec3 subtract(Vec3 vec) {
        return this.subtract(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    public Vec3 subtract(double x2, double y2, double z2) {
        return this.addVector(-x2, -y2, -z2);
    }

    public Vec3 add(Vec3 vec) {
        return this.addVector(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    public Vec3 addVector(double x2, double y2, double z2) {
        return new Vec3(this.xCoord + x2, this.yCoord + y2, this.zCoord + z2);
    }

    public double distanceTo(Vec3 vec) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.yCoord - this.yCoord;
        double d2 = vec.zCoord - this.zCoord;
        return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public double squareDistanceTo(Vec3 vec) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.yCoord - this.yCoord;
        double d2 = vec.zCoord - this.zCoord;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public double lengthVector() {
        return MathHelper.sqrt_double(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
    }

    public Vec3 getIntermediateWithXValue(Vec3 vec, double x2) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.yCoord - this.yCoord;
        double d2 = vec.zCoord - this.zCoord;
        if (d0 * d0 < (double)1.0E-7f) {
            return null;
        }
        double d3 = (x2 - this.xCoord) / d0;
        return d3 >= 0.0 && d3 <= 1.0 ? new Vec3(this.xCoord + d0 * d3, this.yCoord + d1 * d3, this.zCoord + d2 * d3) : null;
    }

    public Vec3 getIntermediateWithYValue(Vec3 vec, double y2) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.yCoord - this.yCoord;
        double d2 = vec.zCoord - this.zCoord;
        if (d1 * d1 < (double)1.0E-7f) {
            return null;
        }
        double d3 = (y2 - this.yCoord) / d1;
        return d3 >= 0.0 && d3 <= 1.0 ? new Vec3(this.xCoord + d0 * d3, this.yCoord + d1 * d3, this.zCoord + d2 * d3) : null;
    }

    public Vec3 getIntermediateWithZValue(Vec3 vec, double z2) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.yCoord - this.yCoord;
        double d2 = vec.zCoord - this.zCoord;
        if (d2 * d2 < (double)1.0E-7f) {
            return null;
        }
        double d3 = (z2 - this.zCoord) / d2;
        return d3 >= 0.0 && d3 <= 1.0 ? new Vec3(this.xCoord + d0 * d3, this.yCoord + d1 * d3, this.zCoord + d2 * d3) : null;
    }

    public String toString() {
        return "(" + this.xCoord + ", " + this.yCoord + ", " + this.zCoord + ")";
    }

    public Vec3 rotatePitch(float pitch) {
        float f2 = MathHelper.cos(pitch);
        float f1 = MathHelper.sin(pitch);
        double d0 = this.xCoord;
        double d1 = this.yCoord * (double)f2 + this.zCoord * (double)f1;
        double d2 = this.zCoord * (double)f2 - this.yCoord * (double)f1;
        return new Vec3(d0, d1, d2);
    }

    public Vec3 rotateYaw(float yaw) {
        float f2 = MathHelper.cos(yaw);
        float f1 = MathHelper.sin(yaw);
        double d0 = this.xCoord * (double)f2 + this.zCoord * (double)f1;
        double d1 = this.yCoord;
        double d2 = this.zCoord * (double)f2 - this.xCoord * (double)f1;
        return new Vec3(d0, d1, d2);
    }
}

