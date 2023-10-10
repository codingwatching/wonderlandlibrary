/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.client.gui;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import optfine.Reflector;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

public class GuiOverlayDebug
extends Gui {
    private final Minecraft mc;
    private final FontRenderer fontRenderer;
    private static final String __OBFID = "CL_00001956";

    public GuiOverlayDebug(Minecraft mc) {
        this.mc = mc;
        this.fontRenderer = mc.fontRendererObj;
    }

    public void renderDebugInfo(ScaledResolution scaledResolutionIn) {
        this.mc.mcProfiler.startSection("debug");
        GlStateManager.pushMatrix();
        this.renderDebugInfoLeft();
        this.renderDebugInfoRight(scaledResolutionIn);
        GlStateManager.popMatrix();
        this.mc.mcProfiler.endSection();
    }

    private boolean isReducedDebug() {
        return this.mc.thePlayer.hasReducedDebug() || this.mc.gameSettings.reducedDebugInfo;
    }

    protected void renderDebugInfoLeft() {
        List list = this.call();
        for (int i2 = 0; i2 < list.size(); ++i2) {
            String s2 = (String)list.get(i2);
            if (Strings.isNullOrEmpty(s2)) continue;
            int j2 = this.fontRenderer.FONT_HEIGHT;
            int k2 = this.fontRenderer.getStringWidth(s2);
            boolean flag = true;
            int l2 = 2 + j2 * i2;
            GuiOverlayDebug.drawRect(1, l2 - 1, 2 + k2 + 1, l2 + j2 - 1, -1873784752);
            this.fontRenderer.drawString(s2, 2, l2, 0xE0E0E0);
        }
    }

    protected void renderDebugInfoRight(ScaledResolution p_175239_1_) {
        List list = this.getDebugInfoRight();
        for (int i2 = 0; i2 < list.size(); ++i2) {
            String s2 = (String)list.get(i2);
            if (Strings.isNullOrEmpty(s2)) continue;
            int j2 = this.fontRenderer.FONT_HEIGHT;
            int k2 = this.fontRenderer.getStringWidth(s2);
            int l2 = p_175239_1_.getScaledWidth() - 2 - k2;
            int i1 = 2 + j2 * i2;
            GuiOverlayDebug.drawRect(l2 - 1, i1 - 1, l2 + k2 + 1, i1 + j2 - 1, -1873784752);
            this.fontRenderer.drawString(s2, l2, i1, 0xE0E0E0);
        }
    }

    protected List call() {
        BlockPos blockpos = new BlockPos(this.mc.getRenderViewEntity().posX, this.mc.getRenderViewEntity().getEntityBoundingBox().minY, this.mc.getRenderViewEntity().posZ);
        if (this.isReducedDebug()) {
            return Lists.newArrayList("Minecraft 1.8.8 (" + this.mc.getVersion() + "/" + ClientBrandRetriever.getClientModName() + ")", this.mc.debug, this.mc.renderGlobal.getDebugInfoRenders(), this.mc.renderGlobal.getDebugInfoEntities(), "P: " + this.mc.effectRenderer.getStatistics() + ". T: " + this.mc.theWorld.getDebugLoadedEntities(), this.mc.theWorld.getProviderName(), "", String.format("Chunk-relative: %d %d %d", blockpos.getX() & 0xF, blockpos.getY() & 0xF, blockpos.getZ() & 0xF));
        }
        Entity entity = this.mc.getRenderViewEntity();
        EnumFacing enumfacing = entity.getHorizontalFacing();
        String s2 = "Invalid";
        switch (enumfacing) {
            case NORTH: {
                s2 = "Towards negative Z";
                break;
            }
            case SOUTH: {
                s2 = "Towards positive Z";
                break;
            }
            case WEST: {
                s2 = "Towards negative X";
                break;
            }
            case EAST: {
                s2 = "Towards positive X";
            }
        }
        ArrayList<String> arraylist = Lists.newArrayList("Minecraft 1.8.8 (" + this.mc.getVersion() + "/" + ClientBrandRetriever.getClientModName() + ")", this.mc.debug, this.mc.renderGlobal.getDebugInfoRenders(), this.mc.renderGlobal.renderDispatcher.getDebugInfo(), this.mc.renderGlobal.getDebugInfoEntities(), "Effect Render Statistics: " + this.mc.effectRenderer.getStatistics(), this.mc.theWorld.getProviderName(), "", String.format("XYZ: %.3f / %.5f / %.3f", this.mc.getRenderViewEntity().posX, this.mc.getRenderViewEntity().getEntityBoundingBox().minY, this.mc.getRenderViewEntity().posZ), String.format("Block: %d %d %d", blockpos.getX(), blockpos.getY(), blockpos.getZ()), String.format("Chunk: %d %d %d in %d %d %d", blockpos.getX() & 0xF, blockpos.getY() & 0xF, blockpos.getZ() & 0xF, blockpos.getX() >> 4, blockpos.getY() >> 4, blockpos.getZ() >> 4), String.format("Facing: %s (%s) (%.1f / %.1f)", enumfacing, s2, Float.valueOf(MathHelper.wrapAngleTo180_float(entity.rotationYaw)), Float.valueOf(MathHelper.wrapAngleTo180_float(entity.rotationPitch))));
        if (this.mc.theWorld != null && this.mc.theWorld.isBlockLoaded(blockpos)) {
            EntityPlayerMP entityplayermp;
            Chunk chunk = this.mc.theWorld.getChunkFromBlockCoords(blockpos);
            arraylist.add("Biome: " + chunk.getBiome((BlockPos)blockpos, (WorldChunkManager)this.mc.theWorld.getWorldChunkManager()).biomeName);
            arraylist.add("Light: " + chunk.getLightSubtracted(blockpos, 0) + " (" + chunk.getLightFor(EnumSkyBlock.SKY, blockpos) + " sky, " + chunk.getLightFor(EnumSkyBlock.BLOCK, blockpos) + " block)");
            DifficultyInstance difficultyinstance = this.mc.theWorld.getDifficultyForLocation(blockpos);
            if (this.mc.isIntegratedServerRunning() && this.mc.getIntegratedServer() != null && (entityplayermp = this.mc.getIntegratedServer().getConfigurationManager().getPlayerByUUID(this.mc.thePlayer.getUniqueID())) != null) {
                difficultyinstance = entityplayermp.worldObj.getDifficultyForLocation(new BlockPos(entityplayermp));
            }
            arraylist.add(String.format("Local Difficulty: %.2f (Day %d)", Float.valueOf(difficultyinstance.getAdditionalDifficulty()), this.mc.theWorld.getWorldTime() / 24000L));
        }
        if (this.mc.entityRenderer != null && this.mc.entityRenderer.isShaderActive()) {
            arraylist.add("Shader: " + this.mc.entityRenderer.getShaderGroup().getShaderGroupName());
        }
        if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && this.mc.objectMouseOver.getBlockPos() != null) {
            BlockPos blockpos1 = this.mc.objectMouseOver.getBlockPos();
            arraylist.add(String.format("Looking at: %d %d %d", blockpos1.getX(), blockpos1.getY(), blockpos1.getZ()));
        }
        return arraylist;
    }

    protected List getDebugInfoRight() {
        long i2 = Runtime.getRuntime().maxMemory();
        long j2 = Runtime.getRuntime().totalMemory();
        long k2 = Runtime.getRuntime().freeMemory();
        long l2 = j2 - k2;
        ArrayList<String> arraylist = Lists.newArrayList(String.format("Java: %s %dbit", System.getProperty("java.version"), this.mc.isJava64bit() ? 64 : 32), String.format("Mem: % 2d%% %03d/%03dMB", l2 * 100L / i2, GuiOverlayDebug.bytesToMb(l2), GuiOverlayDebug.bytesToMb(i2)), String.format("Allocated: % 2d%% %03dMB", j2 * 100L / i2, GuiOverlayDebug.bytesToMb(j2)), "", String.format("CPU: %s", OpenGlHelper.func_183029_j()), "", String.format("Display: %dx%d (%s)", Display.getWidth(), Display.getHeight(), GL11.glGetString(7936)), GL11.glGetString(7937), GL11.glGetString(7938));
        if (Reflector.FMLCommonHandler_getBrandings.exists()) {
            Object object = Reflector.call(Reflector.FMLCommonHandler_instance, new Object[0]);
            arraylist.add("");
            arraylist.addAll((Collection)Reflector.call(object, Reflector.FMLCommonHandler_getBrandings, false));
        }
        if (this.isReducedDebug()) {
            return arraylist;
        }
        if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && this.mc.objectMouseOver.getBlockPos() != null) {
            BlockPos blockpos = this.mc.objectMouseOver.getBlockPos();
            IBlockState iblockstate = this.mc.theWorld.getBlockState(blockpos);
            if (this.mc.theWorld.getWorldType() != WorldType.DEBUG_WORLD) {
                iblockstate = iblockstate.getBlock().getActualState(iblockstate, this.mc.theWorld, blockpos);
            }
            arraylist.add("");
            arraylist.add(String.valueOf(Block.blockRegistry.getNameForObject(iblockstate.getBlock())));
            for (Map.Entry entry : iblockstate.getProperties().entrySet()) {
                String s2 = ((Comparable)entry.getValue()).toString();
                if (entry.getValue() == Boolean.TRUE) {
                    s2 = (Object)((Object)EnumChatFormatting.GREEN) + s2;
                } else if (entry.getValue() == Boolean.FALSE) {
                    s2 = (Object)((Object)EnumChatFormatting.RED) + s2;
                }
                arraylist.add(((IProperty)entry.getKey()).getName() + ": " + s2);
            }
        }
        return arraylist;
    }

    private void func_181554_e() {
        GlStateManager.disableDepth();
        FrameTimer frametimer = this.mc.func_181539_aj();
        int i2 = frametimer.func_181749_a();
        int j2 = frametimer.func_181750_b();
        long[] along = frametimer.func_181746_c();
        ScaledResolution scaledresolution = new ScaledResolution(this.mc);
        int k2 = i2;
        int l2 = 0;
        GuiOverlayDebug.drawRect(0, scaledresolution.getScaledHeight() - 60, 240, scaledresolution.getScaledHeight(), -1873784752);
        while (k2 != j2) {
            int i1 = frametimer.func_181748_a(along[k2], 30);
            int j1 = this.func_181552_c(MathHelper.clamp_int(i1, 0, 60), 0, 30, 60);
            this.drawVerticalLine(l2, scaledresolution.getScaledHeight(), scaledresolution.getScaledHeight() - i1, j1);
            ++l2;
            k2 = frametimer.func_181751_b(k2 + 1);
        }
        GuiOverlayDebug.drawRect(1, scaledresolution.getScaledHeight() - 30 + 1, 14, scaledresolution.getScaledHeight() - 30 + 10, -1873784752);
        this.fontRenderer.drawString("60", 2, scaledresolution.getScaledHeight() - 30 + 2, 0xE0E0E0);
        this.drawHorizontalLine(0, 239, scaledresolution.getScaledHeight() - 30, -1);
        GuiOverlayDebug.drawRect(1, scaledresolution.getScaledHeight() - 60 + 1, 14, scaledresolution.getScaledHeight() - 60 + 10, -1873784752);
        this.fontRenderer.drawString("30", 2, scaledresolution.getScaledHeight() - 60 + 2, 0xE0E0E0);
        this.drawHorizontalLine(0, 239, scaledresolution.getScaledHeight() - 60, -1);
        this.drawHorizontalLine(0, 239, scaledresolution.getScaledHeight() - 1, -1);
        this.drawVerticalLine(0, scaledresolution.getScaledHeight() - 60, scaledresolution.getScaledHeight(), -1);
        this.drawVerticalLine(239, scaledresolution.getScaledHeight() - 60, scaledresolution.getScaledHeight(), -1);
        if (this.mc.gameSettings.limitFramerate <= 120) {
            this.drawHorizontalLine(0, 239, scaledresolution.getScaledHeight() - 60 + this.mc.gameSettings.limitFramerate / 2, -16711681);
        }
        GlStateManager.enableDepth();
    }

    private int func_181552_c(int p_181552_1_, int p_181552_2_, int p_181552_3_, int p_181552_4_) {
        return p_181552_1_ < p_181552_3_ ? this.func_181553_a(-16711936, -256, (float)p_181552_1_ / (float)p_181552_3_) : this.func_181553_a(-256, -65536, (float)(p_181552_1_ - p_181552_3_) / (float)(p_181552_4_ - p_181552_3_));
    }

    private int func_181553_a(int p_181553_1_, int p_181553_2_, float p_181553_3_) {
        int i2 = p_181553_1_ >> 24 & 0xFF;
        int j2 = p_181553_1_ >> 16 & 0xFF;
        int k2 = p_181553_1_ >> 8 & 0xFF;
        int l2 = p_181553_1_ & 0xFF;
        int i1 = p_181553_2_ >> 24 & 0xFF;
        int j1 = p_181553_2_ >> 16 & 0xFF;
        int k1 = p_181553_2_ >> 8 & 0xFF;
        int l1 = p_181553_2_ & 0xFF;
        int i22 = MathHelper.clamp_int((int)((float)i2 + (float)(i1 - i2) * p_181553_3_), 0, 255);
        int j22 = MathHelper.clamp_int((int)((float)j2 + (float)(j1 - j2) * p_181553_3_), 0, 255);
        int k22 = MathHelper.clamp_int((int)((float)k2 + (float)(k1 - k2) * p_181553_3_), 0, 255);
        int l22 = MathHelper.clamp_int((int)((float)l2 + (float)(l1 - l2) * p_181553_3_), 0, 255);
        return i22 << 24 | j22 << 16 | k22 << 8 | l22;
    }

    private static long bytesToMb(long bytes) {
        return bytes / 1024L / 1024L;
    }
}

