package net.minecraft.world.chunk;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ClassInheratanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.EnumFacing.Plane;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderDebug;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Chunk
{
  private static final Logger logger = ;
  private final ExtendedBlockStorage[] storageArrays;
  private final byte[] blockBiomeArray;
  private final int[] precipitationHeightMap;
  private final boolean[] updateSkylightColumns;
  private boolean isChunkLoaded;
  private final World worldObj;
  private final int[] heightMap;
  public final int xPosition;
  public final int zPosition;
  private boolean isGapLightingUpdated;
  private final Map chunkTileEntityMap;
  private final ClassInheratanceMultiMap[] entityLists;
  private boolean isTerrainPopulated;
  private boolean isLightPopulated;
  private boolean field_150815_m;
  private boolean isModified;
  private boolean hasEntities;
  private long lastSaveTime;
  private int heightMapMinimum;
  private long inhabitedTime;
  private int queuedLightChecks;
  private ConcurrentLinkedQueue field_177447_w;
  private static final String __OBFID = "CL_00000373";
  
  public Chunk(World worldIn, int x, int z)
  {
    this.storageArrays = new ExtendedBlockStorage[16];
    this.blockBiomeArray = new byte['Ā'];
    this.precipitationHeightMap = new int['Ā'];
    this.updateSkylightColumns = new boolean['Ā'];
    this.chunkTileEntityMap = Maps.newHashMap();
    this.queuedLightChecks = 4096;
    this.field_177447_w = Queues.newConcurrentLinkedQueue();
    this.entityLists = ((ClassInheratanceMultiMap[])new ClassInheratanceMultiMap[16]);
    this.worldObj = worldIn;
    this.xPosition = x;
    this.zPosition = z;
    this.heightMap = new int['Ā'];
    for (int var4 = 0; var4 < this.entityLists.length; var4++) {
      this.entityLists[var4] = new ClassInheratanceMultiMap(Entity.class);
    }
    Arrays.fill(this.precipitationHeightMap, 64537);
    Arrays.fill(this.blockBiomeArray, (byte)-1);
  }
  
  public Chunk(World worldIn, ChunkPrimer primer, int x, int z)
  {
    this(worldIn, x, z);
    short var5 = 256;
    boolean var6 = !worldIn.provider.getHasNoSky();
    for (int var7 = 0; var7 < 16; var7++) {
      for (int var8 = 0; var8 < 16; var8++) {
        for (int var9 = 0; var9 < var5; var9++)
        {
          int var10 = var7 * var5 * 16 | var8 * var5 | var9;
          IBlockState var11 = primer.getBlockState(var10);
          if (var11.getBlock().getMaterial() != Material.air)
          {
            int var12 = var9 >> 4;
            if (this.storageArrays[var12] == null) {
              this.storageArrays[var12] = new ExtendedBlockStorage(var12 << 4, var6);
            }
            this.storageArrays[var12].set(var7, var9 & 0xF, var8, var11);
          }
        }
      }
    }
  }
  
  public boolean isAtLocation(int x, int z)
  {
    return (x == this.xPosition) && (z == this.zPosition);
  }
  
  public int getHeight(BlockPos pos)
  {
    return getHeight(pos.getX() & 0xF, pos.getZ() & 0xF);
  }
  
  public int getHeight(int x, int z)
  {
    return this.heightMap[(z << 4 | x)];
  }
  
  public int getTopFilledSegment()
  {
    for (int var1 = this.storageArrays.length - 1; var1 >= 0; var1--) {
      if (this.storageArrays[var1] != null) {
        return this.storageArrays[var1].getYLocation();
      }
    }
    return 0;
  }
  
  public ExtendedBlockStorage[] getBlockStorageArray()
  {
    return this.storageArrays;
  }
  
  protected void generateHeightMap()
  {
    int var1 = getTopFilledSegment();
    this.heightMapMinimum = Integer.MAX_VALUE;
    for (int var2 = 0; var2 < 16; var2++)
    {
      int var3 = 0;
      while (var3 < 16)
      {
        this.precipitationHeightMap[(var2 + (var3 << 4))] = 64537;
        int var4 = var1 + 16;
        while (var4 > 0)
        {
          Block var5 = getBlock0(var2, var4 - 1, var3);
          if (var5.getLightOpacity() == 0)
          {
            var4--;
          }
          else
          {
            this.heightMap[(var3 << 4 | var2)] = var4;
            if (var4 < this.heightMapMinimum) {
              this.heightMapMinimum = var4;
            }
          }
        }
        var3++;
      }
    }
    this.isModified = true;
  }
  
  public void generateSkylightMap()
  {
    int var1 = getTopFilledSegment();
    this.heightMapMinimum = Integer.MAX_VALUE;
    for (int var2 = 0; var2 < 16; var2++)
    {
      int var3 = 0;
      while (var3 < 16)
      {
        this.precipitationHeightMap[(var2 + (var3 << 4))] = 64537;
        int var4 = var1 + 16;
        while (var4 > 0) {
          if (getBlockLightOpacity(var2, var4 - 1, var3) == 0)
          {
            var4--;
          }
          else
          {
            this.heightMap[(var3 << 4 | var2)] = var4;
            if (var4 < this.heightMapMinimum) {
              this.heightMapMinimum = var4;
            }
          }
        }
        if (!this.worldObj.provider.getHasNoSky())
        {
          var4 = 15;
          int var5 = var1 + 16 - 1;
          do
          {
            int var6 = getBlockLightOpacity(var2, var5, var3);
            if ((var6 == 0) && (var4 != 15)) {
              var6 = 1;
            }
            var4 -= var6;
            if (var4 > 0)
            {
              ExtendedBlockStorage var7 = this.storageArrays[(var5 >> 4)];
              if (var7 != null)
              {
                var7.setExtSkylightValue(var2, var5 & 0xF, var3, var4);
                this.worldObj.notifyLightSet(new BlockPos((this.xPosition << 4) + var2, var5, (this.zPosition << 4) + var3));
              }
            }
            var5--;
          } while ((var5 > 0) && (var4 > 0));
        }
        var3++;
      }
    }
    this.isModified = true;
  }
  
  private void propagateSkylightOcclusion(int x, int z)
  {
    this.updateSkylightColumns[(x + z * 16)] = true;
    this.isGapLightingUpdated = true;
  }
  
  private void recheckGaps(boolean p_150803_1_)
  {
    this.worldObj.theProfiler.startSection("recheckGaps");
    if (this.worldObj.isAreaLoaded(new BlockPos(this.xPosition * 16 + 8, 0, this.zPosition * 16 + 8), 16))
    {
      for (int var2 = 0; var2 < 16; var2++) {
        for (int var3 = 0; var3 < 16; var3++) {
          if (this.updateSkylightColumns[(var2 + var3 * 16)] != 0)
          {
            this.updateSkylightColumns[(var2 + var3 * 16)] = false;
            int var4 = getHeight(var2, var3);
            int var5 = this.xPosition * 16 + var2;
            int var6 = this.zPosition * 16 + var3;
            int var7 = Integer.MAX_VALUE;
            EnumFacing var9;
            for (Iterator var8 = EnumFacing.Plane.HORIZONTAL.iterator(); var8.hasNext(); var7 = Math.min(var7, this.worldObj.getChunksLowestHorizon(var5 + var9.getFrontOffsetX(), var6 + var9.getFrontOffsetZ()))) {
              var9 = (EnumFacing)var8.next();
            }
            checkSkylightNeighborHeight(var5, var6, var7);
            var8 = EnumFacing.Plane.HORIZONTAL.iterator();
            while (var8.hasNext())
            {
              EnumFacing var9 = (EnumFacing)var8.next();
              checkSkylightNeighborHeight(var5 + var9.getFrontOffsetX(), var6 + var9.getFrontOffsetZ(), var4);
            }
            if (p_150803_1_)
            {
              this.worldObj.theProfiler.endSection();
              return;
            }
          }
        }
      }
      this.isGapLightingUpdated = false;
    }
    this.worldObj.theProfiler.endSection();
  }
  
  private void checkSkylightNeighborHeight(int x, int p_76599_2_, int z)
  {
    int var4 = this.worldObj.getHorizon(new BlockPos(x, 0, p_76599_2_)).getY();
    if (var4 > z) {
      updateSkylightNeighborHeight(x, p_76599_2_, z, var4 + 1);
    } else if (var4 < z) {
      updateSkylightNeighborHeight(x, p_76599_2_, var4, z + 1);
    }
  }
  
  private void updateSkylightNeighborHeight(int x, int z, int startY, int endY)
  {
    if ((endY > startY) && (this.worldObj.isAreaLoaded(new BlockPos(x, 0, z), 16)))
    {
      for (int var5 = startY; var5 < endY; var5++) {
        this.worldObj.checkLightFor(EnumSkyBlock.SKY, new BlockPos(x, var5, z));
      }
      this.isModified = true;
    }
  }
  
  private void relightBlock(int x, int y, int z)
  {
    int var4 = this.heightMap[(z << 4 | x)] & 0xFF;
    int var5 = var4;
    if (y > var4) {
      var5 = y;
    }
    while ((var5 > 0) && (getBlockLightOpacity(x, var5 - 1, z) == 0)) {
      var5--;
    }
    if (var5 != var4)
    {
      this.worldObj.markBlocksDirtyVertical(x + this.xPosition * 16, z + this.zPosition * 16, var5, var4);
      this.heightMap[(z << 4 | x)] = var5;
      int var6 = this.xPosition * 16 + x;
      int var7 = this.zPosition * 16 + z;
      if (!this.worldObj.provider.getHasNoSky())
      {
        if (var5 < var4) {
          for (int var8 = var5; var8 < var4; var8++)
          {
            ExtendedBlockStorage var9 = this.storageArrays[(var8 >> 4)];
            if (var9 != null)
            {
              var9.setExtSkylightValue(x, var8 & 0xF, z, 15);
              this.worldObj.notifyLightSet(new BlockPos((this.xPosition << 4) + x, var8, (this.zPosition << 4) + z));
            }
          }
        }
        for (int var8 = var4; var8 < var5; var8++)
        {
          ExtendedBlockStorage var9 = this.storageArrays[(var8 >> 4)];
          if (var9 != null)
          {
            var9.setExtSkylightValue(x, var8 & 0xF, z, 0);
            this.worldObj.notifyLightSet(new BlockPos((this.xPosition << 4) + x, var8, (this.zPosition << 4) + z));
          }
        }
        var8 = 15;
        while ((var5 > 0) && (var8 > 0))
        {
          var5--;
          int var13 = getBlockLightOpacity(x, var5, z);
          if (var13 == 0) {
            var13 = 1;
          }
          var8 -= var13;
          if (var8 < 0) {
            var8 = 0;
          }
          ExtendedBlockStorage var10 = this.storageArrays[(var5 >> 4)];
          if (var10 != null) {
            var10.setExtSkylightValue(x, var5 & 0xF, z, var8);
          }
        }
      }
      int var8 = this.heightMap[(z << 4 | x)];
      int var13 = var4;
      int var14 = var8;
      if (var8 < var4)
      {
        var13 = var8;
        var14 = var4;
      }
      if (var8 < this.heightMapMinimum) {
        this.heightMapMinimum = var8;
      }
      if (!this.worldObj.provider.getHasNoSky())
      {
        Iterator var11 = EnumFacing.Plane.HORIZONTAL.iterator();
        while (var11.hasNext())
        {
          EnumFacing var12 = (EnumFacing)var11.next();
          updateSkylightNeighborHeight(var6 + var12.getFrontOffsetX(), var7 + var12.getFrontOffsetZ(), var13, var14);
        }
        updateSkylightNeighborHeight(var6, var7, var13, var14);
      }
      this.isModified = true;
    }
  }
  
  public int getBlockLightOpacity(BlockPos pos)
  {
    return getBlock(pos).getLightOpacity();
  }
  
  private int getBlockLightOpacity(int p_150808_1_, int p_150808_2_, int p_150808_3_)
  {
    return getBlock0(p_150808_1_, p_150808_2_, p_150808_3_).getLightOpacity();
  }
  
  private Block getBlock0(int x, int y, int z)
  {
    Block var4 = Blocks.air;
    if ((y >= 0) && (y >> 4 < this.storageArrays.length))
    {
      ExtendedBlockStorage var5 = this.storageArrays[(y >> 4)];
      if (var5 != null) {
        try
        {
          var4 = var5.getBlockByExtId(x, y & 0xF, z);
        }
        catch (Throwable var8)
        {
          CrashReport var7 = CrashReport.makeCrashReport(var8, "Getting block");
          throw new ReportedException(var7);
        }
      }
    }
    return var4;
  }
  
  public Block getBlock(final int x, final int y, final int z)
  {
    try
    {
      return getBlock0(x & 0xF, y, z & 0xF);
    }
    catch (ReportedException var6)
    {
      CrashReportCategory var5 = var6.getCrashReport().makeCategory("Block being got");
      var5.addCrashSectionCallable("Location", new Callable()
      {
        private static final String __OBFID = "CL_00000374";
        
        public String call()
        {
          return CrashReportCategory.getCoordinateInfo(new BlockPos(Chunk.this.xPosition * 16 + x, y, Chunk.this.zPosition * 16 + z));
        }
      });
      throw var6;
    }
  }
  
  public Block getBlock(final BlockPos pos)
  {
    try
    {
      return getBlock0(pos.getX() & 0xF, pos.getY(), pos.getZ() & 0xF);
    }
    catch (ReportedException var4)
    {
      CrashReportCategory var3 = var4.getCrashReport().makeCategory("Block being got");
      var3.addCrashSectionCallable("Location", new Callable()
      {
        private static final String __OBFID = "CL_00002011";
        
        public String func_177455_a()
        {
          return CrashReportCategory.getCoordinateInfo(pos);
        }
        
        public Object call()
        {
          return func_177455_a();
        }
      });
      throw var4;
    }
  }
  
  public IBlockState getBlockState(final BlockPos pos)
  {
    if (this.worldObj.getWorldType() == WorldType.DEBUG_WORLD)
    {
      IBlockState var7 = null;
      if (pos.getY() == 60) {
        var7 = Blocks.barrier.getDefaultState();
      }
      if (pos.getY() == 70) {
        var7 = ChunkProviderDebug.func_177461_b(pos.getX(), pos.getZ());
      }
      return var7 == null ? Blocks.air.getDefaultState() : var7;
    }
    try
    {
      if ((pos.getY() >= 0) && (pos.getY() >> 4 < this.storageArrays.length))
      {
        ExtendedBlockStorage var2 = this.storageArrays[(pos.getY() >> 4)];
        if (var2 != null)
        {
          int var8 = pos.getX() & 0xF;
          int var9 = pos.getY() & 0xF;
          int var5 = pos.getZ() & 0xF;
          return var2.get(var8, var9, var5);
        }
      }
      return Blocks.air.getDefaultState();
    }
    catch (Throwable var6)
    {
      CrashReport var3 = CrashReport.makeCrashReport(var6, "Getting block state");
      CrashReportCategory var4 = var3.makeCategory("Block being got");
      var4.addCrashSectionCallable("Location", new Callable()
      {
        private static final String __OBFID = "CL_00002010";
        
        public String func_177448_a()
        {
          return CrashReportCategory.getCoordinateInfo(pos);
        }
        
        public Object call()
        {
          return func_177448_a();
        }
      });
      throw new ReportedException(var3);
    }
  }
  
  private int getBlockMetadata(int p_76628_1_, int p_76628_2_, int p_76628_3_)
  {
    if (p_76628_2_ >> 4 >= this.storageArrays.length) {
      return 0;
    }
    ExtendedBlockStorage var4 = this.storageArrays[(p_76628_2_ >> 4)];
    return var4 != null ? var4.getExtBlockMetadata(p_76628_1_, p_76628_2_ & 0xF, p_76628_3_) : 0;
  }
  
  public int getBlockMetadata(BlockPos pos)
  {
    return getBlockMetadata(pos.getX() & 0xF, pos.getY(), pos.getZ() & 0xF);
  }
  
  public IBlockState setBlockState(BlockPos p_177436_1_, IBlockState p_177436_2_)
  {
    int var3 = p_177436_1_.getX() & 0xF;
    int var4 = p_177436_1_.getY();
    int var5 = p_177436_1_.getZ() & 0xF;
    int var6 = var5 << 4 | var3;
    if (var4 >= this.precipitationHeightMap[var6] - 1) {
      this.precipitationHeightMap[var6] = 64537;
    }
    int var7 = this.heightMap[var6];
    IBlockState var8 = getBlockState(p_177436_1_);
    if (var8 == p_177436_2_) {
      return null;
    }
    Block var9 = p_177436_2_.getBlock();
    Block var10 = var8.getBlock();
    ExtendedBlockStorage var11 = this.storageArrays[(var4 >> 4)];
    boolean var12 = false;
    if (var11 == null)
    {
      if (var9 == Blocks.air) {
        return null;
      }
      var11 = this.storageArrays[(var4 >> 4)] = new ExtendedBlockStorage(var4 >> 4 << 4, !this.worldObj.provider.getHasNoSky());
      var12 = var4 >= var7;
    }
    var11.set(var3, var4 & 0xF, var5, p_177436_2_);
    if (var10 != var9) {
      if (!this.worldObj.isRemote) {
        var10.breakBlock(this.worldObj, p_177436_1_, var8);
      } else if ((var10 instanceof ITileEntityProvider)) {
        this.worldObj.removeTileEntity(p_177436_1_);
      }
    }
    if (var11.getBlockByExtId(var3, var4 & 0xF, var5) != var9) {
      return null;
    }
    if (var12)
    {
      generateSkylightMap();
    }
    else
    {
      int var13 = var9.getLightOpacity();
      int var14 = var10.getLightOpacity();
      if (var13 > 0)
      {
        if (var4 >= var7) {
          relightBlock(var3, var4 + 1, var5);
        }
      }
      else if (var4 == var7 - 1) {
        relightBlock(var3, var4, var5);
      }
      if ((var13 != var14) && ((var13 < var14) || (getLightFor(EnumSkyBlock.SKY, p_177436_1_) > 0) || (getLightFor(EnumSkyBlock.BLOCK, p_177436_1_) > 0))) {
        propagateSkylightOcclusion(var3, var5);
      }
    }
    if ((var10 instanceof ITileEntityProvider))
    {
      TileEntity var15 = func_177424_a(p_177436_1_, EnumCreateEntityType.CHECK);
      if (var15 != null) {
        var15.updateContainingBlockInfo();
      }
    }
    if ((!this.worldObj.isRemote) && (var10 != var9)) {
      var9.onBlockAdded(this.worldObj, p_177436_1_, p_177436_2_);
    }
    if ((var9 instanceof ITileEntityProvider))
    {
      TileEntity var15 = func_177424_a(p_177436_1_, EnumCreateEntityType.CHECK);
      if (var15 == null)
      {
        var15 = ((ITileEntityProvider)var9).createNewTileEntity(this.worldObj, var9.getMetaFromState(p_177436_2_));
        this.worldObj.setTileEntity(p_177436_1_, var15);
      }
      if (var15 != null) {
        var15.updateContainingBlockInfo();
      }
    }
    this.isModified = true;
    return var8;
  }
  
  public int getLightFor(EnumSkyBlock p_177413_1_, BlockPos p_177413_2_)
  {
    int var3 = p_177413_2_.getX() & 0xF;
    int var4 = p_177413_2_.getY();
    int var5 = p_177413_2_.getZ() & 0xF;
    ExtendedBlockStorage var6 = this.storageArrays[(var4 >> 4)];
    return p_177413_1_ == EnumSkyBlock.BLOCK ? var6.getExtBlocklightValue(var3, var4 & 0xF, var5) : p_177413_1_ == EnumSkyBlock.SKY ? var6.getExtSkylightValue(var3, var4 & 0xF, var5) : this.worldObj.provider.getHasNoSky() ? 0 : var6 == null ? 0 : canSeeSky(p_177413_2_) ? p_177413_1_.defaultLightValue : p_177413_1_.defaultLightValue;
  }
  
  public void setLightFor(EnumSkyBlock p_177431_1_, BlockPos p_177431_2_, int p_177431_3_)
  {
    int var4 = p_177431_2_.getX() & 0xF;
    int var5 = p_177431_2_.getY();
    int var6 = p_177431_2_.getZ() & 0xF;
    ExtendedBlockStorage var7 = this.storageArrays[(var5 >> 4)];
    if (var7 == null)
    {
      var7 = this.storageArrays[(var5 >> 4)] = new ExtendedBlockStorage(var5 >> 4 << 4, !this.worldObj.provider.getHasNoSky());
      generateSkylightMap();
    }
    this.isModified = true;
    if (p_177431_1_ == EnumSkyBlock.SKY)
    {
      if (!this.worldObj.provider.getHasNoSky()) {
        var7.setExtSkylightValue(var4, var5 & 0xF, var6, p_177431_3_);
      }
    }
    else if (p_177431_1_ == EnumSkyBlock.BLOCK) {
      var7.setExtBlocklightValue(var4, var5 & 0xF, var6, p_177431_3_);
    }
  }
  
  public int setLight(BlockPos p_177443_1_, int p_177443_2_)
  {
    int var3 = p_177443_1_.getX() & 0xF;
    int var4 = p_177443_1_.getY();
    int var5 = p_177443_1_.getZ() & 0xF;
    ExtendedBlockStorage var6 = this.storageArrays[(var4 >> 4)];
    if (var6 == null) {
      return (!this.worldObj.provider.getHasNoSky()) && (p_177443_2_ < EnumSkyBlock.SKY.defaultLightValue) ? EnumSkyBlock.SKY.defaultLightValue - p_177443_2_ : 0;
    }
    int var7 = this.worldObj.provider.getHasNoSky() ? 0 : var6.getExtSkylightValue(var3, var4 & 0xF, var5);
    var7 -= p_177443_2_;
    int var8 = var6.getExtBlocklightValue(var3, var4 & 0xF, var5);
    if (var8 > var7) {
      var7 = var8;
    }
    return var7;
  }
  
  public void addEntity(Entity entityIn)
  {
    this.hasEntities = true;
    int var2 = MathHelper.floor_double(entityIn.posX / 16.0D);
    int var3 = MathHelper.floor_double(entityIn.posZ / 16.0D);
    if ((var2 != this.xPosition) || (var3 != this.zPosition))
    {
      logger.warn("Wrong location! (" + var2 + ", " + var3 + ") should be (" + this.xPosition + ", " + this.zPosition + "), " + entityIn, new Object[] { entityIn });
      entityIn.setDead();
    }
    int var4 = MathHelper.floor_double(entityIn.posY / 16.0D);
    if (var4 < 0) {
      var4 = 0;
    }
    if (var4 >= this.entityLists.length) {
      var4 = this.entityLists.length - 1;
    }
    entityIn.addedToChunk = true;
    entityIn.chunkCoordX = this.xPosition;
    entityIn.chunkCoordY = var4;
    entityIn.chunkCoordZ = this.zPosition;
    this.entityLists[var4].add(entityIn);
  }
  
  public void removeEntity(Entity p_76622_1_)
  {
    removeEntityAtIndex(p_76622_1_, p_76622_1_.chunkCoordY);
  }
  
  public void removeEntityAtIndex(Entity p_76608_1_, int p_76608_2_)
  {
    if (p_76608_2_ < 0) {
      p_76608_2_ = 0;
    }
    if (p_76608_2_ >= this.entityLists.length) {
      p_76608_2_ = this.entityLists.length - 1;
    }
    this.entityLists[p_76608_2_].remove(p_76608_1_);
  }
  
  public boolean canSeeSky(BlockPos pos)
  {
    int var2 = pos.getX() & 0xF;
    int var3 = pos.getY();
    int var4 = pos.getZ() & 0xF;
    return var3 >= this.heightMap[(var4 << 4 | var2)];
  }
  
  private TileEntity createNewTileEntity(BlockPos pos)
  {
    Block var2 = getBlock(pos);
    return !var2.hasTileEntity() ? null : ((ITileEntityProvider)var2).createNewTileEntity(this.worldObj, getBlockMetadata(pos));
  }
  
  public TileEntity func_177424_a(BlockPos p_177424_1_, EnumCreateEntityType p_177424_2_)
  {
    TileEntity var3 = (TileEntity)this.chunkTileEntityMap.get(p_177424_1_);
    if (var3 == null)
    {
      if (p_177424_2_ == EnumCreateEntityType.IMMEDIATE)
      {
        var3 = createNewTileEntity(p_177424_1_);
        this.worldObj.setTileEntity(p_177424_1_, var3);
      }
      else if (p_177424_2_ == EnumCreateEntityType.QUEUED)
      {
        this.field_177447_w.add(p_177424_1_);
      }
    }
    else if (var3.isInvalid())
    {
      this.chunkTileEntityMap.remove(p_177424_1_);
      return null;
    }
    return var3;
  }
  
  public void addTileEntity(TileEntity tileEntityIn)
  {
    addTileEntity(tileEntityIn.getPos(), tileEntityIn);
    if (this.isChunkLoaded) {
      this.worldObj.addTileEntity(tileEntityIn);
    }
  }
  
  public void addTileEntity(BlockPos pos, TileEntity tileEntityIn)
  {
    tileEntityIn.setWorldObj(this.worldObj);
    tileEntityIn.setPos(pos);
    if ((getBlock(pos) instanceof ITileEntityProvider))
    {
      if (this.chunkTileEntityMap.containsKey(pos)) {
        ((TileEntity)this.chunkTileEntityMap.get(pos)).invalidate();
      }
      tileEntityIn.validate();
      this.chunkTileEntityMap.put(pos, tileEntityIn);
    }
  }
  
  public void removeTileEntity(BlockPos pos)
  {
    if (this.isChunkLoaded)
    {
      TileEntity var2 = (TileEntity)this.chunkTileEntityMap.remove(pos);
      if (var2 != null) {
        var2.invalidate();
      }
    }
  }
  
  public void onChunkLoad()
  {
    this.isChunkLoaded = true;
    this.worldObj.addTileEntities(this.chunkTileEntityMap.values());
    for (int var1 = 0; var1 < this.entityLists.length; var1++)
    {
      Iterator var2 = this.entityLists[var1].iterator();
      while (var2.hasNext())
      {
        Entity var3 = (Entity)var2.next();
        var3.onChunkLoad();
      }
      this.worldObj.loadEntities(this.entityLists[var1]);
    }
  }
  
  public void onChunkUnload()
  {
    this.isChunkLoaded = false;
    Iterator var1 = this.chunkTileEntityMap.values().iterator();
    while (var1.hasNext())
    {
      TileEntity var2 = (TileEntity)var1.next();
      this.worldObj.markTileEntityForRemoval(var2);
    }
    for (int var3 = 0; var3 < this.entityLists.length; var3++) {
      this.worldObj.unloadEntities(this.entityLists[var3]);
    }
  }
  
  public void setChunkModified()
  {
    this.isModified = true;
  }
  
  public void func_177414_a(Entity p_177414_1_, AxisAlignedBB p_177414_2_, List p_177414_3_, Predicate p_177414_4_)
  {
    int var5 = MathHelper.floor_double((p_177414_2_.minY - 2.0D) / 16.0D);
    int var6 = MathHelper.floor_double((p_177414_2_.maxY + 2.0D) / 16.0D);
    var5 = MathHelper.clamp_int(var5, 0, this.entityLists.length - 1);
    var6 = MathHelper.clamp_int(var6, 0, this.entityLists.length - 1);
    for (int var7 = var5; var7 <= var6; var7++)
    {
      Iterator var8 = this.entityLists[var7].iterator();
      while (var8.hasNext())
      {
        Entity var9 = (Entity)var8.next();
        if ((var9 != p_177414_1_) && (var9.getEntityBoundingBox().intersectsWith(p_177414_2_)) && ((p_177414_4_ == null) || (p_177414_4_.apply(var9))))
        {
          p_177414_3_.add(var9);
          Entity[] var10 = var9.getParts();
          if (var10 != null) {
            for (int var11 = 0; var11 < var10.length; var11++)
            {
              var9 = var10[var11];
              if ((var9 != p_177414_1_) && (var9.getEntityBoundingBox().intersectsWith(p_177414_2_)) && ((p_177414_4_ == null) || (p_177414_4_.apply(var9)))) {
                p_177414_3_.add(var9);
              }
            }
          }
        }
      }
    }
  }
  
  public void func_177430_a(Class p_177430_1_, AxisAlignedBB p_177430_2_, List p_177430_3_, Predicate p_177430_4_)
  {
    int var5 = MathHelper.floor_double((p_177430_2_.minY - 2.0D) / 16.0D);
    int var6 = MathHelper.floor_double((p_177430_2_.maxY + 2.0D) / 16.0D);
    var5 = MathHelper.clamp_int(var5, 0, this.entityLists.length - 1);
    var6 = MathHelper.clamp_int(var6, 0, this.entityLists.length - 1);
    for (int var7 = var5; var7 <= var6; var7++)
    {
      Iterator var8 = this.entityLists[var7].func_180215_b(p_177430_1_).iterator();
      while (var8.hasNext())
      {
        Entity var9 = (Entity)var8.next();
        if ((var9.getEntityBoundingBox().intersectsWith(p_177430_2_)) && ((p_177430_4_ == null) || (p_177430_4_.apply(var9)))) {
          p_177430_3_.add(var9);
        }
      }
    }
  }
  
  public boolean needsSaving(boolean p_76601_1_)
  {
    if (p_76601_1_)
    {
      if (((this.hasEntities) && (this.worldObj.getTotalWorldTime() != this.lastSaveTime)) || (this.isModified)) {
        return true;
      }
    }
    else if ((this.hasEntities) && (this.worldObj.getTotalWorldTime() >= this.lastSaveTime + 600L)) {
      return true;
    }
    return this.isModified;
  }
  
  public Random getRandomWithSeed(long seed)
  {
    return new Random(this.worldObj.getSeed() + this.xPosition * this.xPosition * 4987142 + this.xPosition * 5947611 + this.zPosition * this.zPosition * 4392871L + this.zPosition * 389711 ^ seed);
  }
  
  public boolean isEmpty()
  {
    return false;
  }
  
  public void populateChunk(IChunkProvider p_76624_1_, IChunkProvider p_76624_2_, int p_76624_3_, int p_76624_4_)
  {
    boolean var5 = p_76624_1_.chunkExists(p_76624_3_, p_76624_4_ - 1);
    boolean var6 = p_76624_1_.chunkExists(p_76624_3_ + 1, p_76624_4_);
    boolean var7 = p_76624_1_.chunkExists(p_76624_3_, p_76624_4_ + 1);
    boolean var8 = p_76624_1_.chunkExists(p_76624_3_ - 1, p_76624_4_);
    boolean var9 = p_76624_1_.chunkExists(p_76624_3_ - 1, p_76624_4_ - 1);
    boolean var10 = p_76624_1_.chunkExists(p_76624_3_ + 1, p_76624_4_ + 1);
    boolean var11 = p_76624_1_.chunkExists(p_76624_3_ - 1, p_76624_4_ + 1);
    boolean var12 = p_76624_1_.chunkExists(p_76624_3_ + 1, p_76624_4_ - 1);
    if ((var6) && (var7) && (var10)) {
      if (!this.isTerrainPopulated) {
        p_76624_1_.populate(p_76624_2_, p_76624_3_, p_76624_4_);
      } else {
        p_76624_1_.func_177460_a(p_76624_2_, this, p_76624_3_, p_76624_4_);
      }
    }
    if ((var8) && (var7) && (var11))
    {
      Chunk var13 = p_76624_1_.provideChunk(p_76624_3_ - 1, p_76624_4_);
      if (!var13.isTerrainPopulated) {
        p_76624_1_.populate(p_76624_2_, p_76624_3_ - 1, p_76624_4_);
      } else {
        p_76624_1_.func_177460_a(p_76624_2_, var13, p_76624_3_ - 1, p_76624_4_);
      }
    }
    if ((var5) && (var6) && (var12))
    {
      Chunk var13 = p_76624_1_.provideChunk(p_76624_3_, p_76624_4_ - 1);
      if (!var13.isTerrainPopulated) {
        p_76624_1_.populate(p_76624_2_, p_76624_3_, p_76624_4_ - 1);
      } else {
        p_76624_1_.func_177460_a(p_76624_2_, var13, p_76624_3_, p_76624_4_ - 1);
      }
    }
    if ((var9) && (var5) && (var8))
    {
      Chunk var13 = p_76624_1_.provideChunk(p_76624_3_ - 1, p_76624_4_ - 1);
      if (!var13.isTerrainPopulated) {
        p_76624_1_.populate(p_76624_2_, p_76624_3_ - 1, p_76624_4_ - 1);
      } else {
        p_76624_1_.func_177460_a(p_76624_2_, var13, p_76624_3_ - 1, p_76624_4_ - 1);
      }
    }
  }
  
  public BlockPos func_177440_h(BlockPos p_177440_1_)
  {
    int var2 = p_177440_1_.getX() & 0xF;
    int var3 = p_177440_1_.getZ() & 0xF;
    int var4 = var2 | var3 << 4;
    BlockPos var5 = new BlockPos(p_177440_1_.getX(), this.precipitationHeightMap[var4], p_177440_1_.getZ());
    if (var5.getY() == 64537)
    {
      int var6 = getTopFilledSegment() + 15;
      var5 = new BlockPos(p_177440_1_.getX(), var6, p_177440_1_.getZ());
      int var7 = -1;
      while ((var5.getY() > 0) && (var7 == -1))
      {
        Block var8 = getBlock(var5);
        Material var9 = var8.getMaterial();
        if ((!var9.blocksMovement()) && (!var9.isLiquid())) {
          var5 = var5.offsetDown();
        } else {
          var7 = var5.getY() + 1;
        }
      }
      this.precipitationHeightMap[var4] = var7;
    }
    return new BlockPos(p_177440_1_.getX(), this.precipitationHeightMap[var4], p_177440_1_.getZ());
  }
  
  public void func_150804_b(boolean p_150804_1_)
  {
    if ((this.isGapLightingUpdated) && (!this.worldObj.provider.getHasNoSky()) && (!p_150804_1_)) {
      recheckGaps(this.worldObj.isRemote);
    }
    this.field_150815_m = true;
    if ((!this.isLightPopulated) && (this.isTerrainPopulated)) {
      func_150809_p();
    }
    while (!this.field_177447_w.isEmpty())
    {
      BlockPos var2 = (BlockPos)this.field_177447_w.poll();
      if ((func_177424_a(var2, EnumCreateEntityType.CHECK) == null) && (getBlock(var2).hasTileEntity()))
      {
        TileEntity var3 = createNewTileEntity(var2);
        this.worldObj.setTileEntity(var2, var3);
        this.worldObj.markBlockRangeForRenderUpdate(var2, var2);
      }
    }
  }
  
  public boolean isPopulated()
  {
    return (this.field_150815_m) && (this.isTerrainPopulated) && (this.isLightPopulated);
  }
  
  public ChunkCoordIntPair getChunkCoordIntPair()
  {
    return new ChunkCoordIntPair(this.xPosition, this.zPosition);
  }
  
  public boolean getAreLevelsEmpty(int p_76606_1_, int p_76606_2_)
  {
    if (p_76606_1_ < 0) {
      p_76606_1_ = 0;
    }
    if (p_76606_2_ >= 256) {
      p_76606_2_ = 255;
    }
    for (int var3 = p_76606_1_; var3 <= p_76606_2_; var3 += 16)
    {
      ExtendedBlockStorage var4 = this.storageArrays[(var3 >> 4)];
      if ((var4 != null) && (!var4.isEmpty())) {
        return false;
      }
    }
    return true;
  }
  
  public void setStorageArrays(ExtendedBlockStorage[] newStorageArrays)
  {
    if (this.storageArrays.length != newStorageArrays.length) {
      logger.warn("Could not set level chunk sections, array length is " + newStorageArrays.length + " instead of " + this.storageArrays.length);
    } else {
      for (int var2 = 0; var2 < this.storageArrays.length; var2++) {
        this.storageArrays[var2] = newStorageArrays[var2];
      }
    }
  }
  
  public void func_177439_a(byte[] p_177439_1_, int p_177439_2_, boolean p_177439_3_)
  {
    int var4 = 0;
    boolean var5 = !this.worldObj.provider.getHasNoSky();
    int var8;
    for (int var6 = 0; var6 < this.storageArrays.length; var6++) {
      if ((p_177439_2_ & 1 << var6) != 0)
      {
        if (this.storageArrays[var6] == null) {
          this.storageArrays[var6] = new ExtendedBlockStorage(var6 << 4, var5);
        }
        char[] var7 = this.storageArrays[var6].getData();
        for (var8 = 0; var8 < var7.length; var8++)
        {
          var7[var8] = ((char)((p_177439_1_[(var4 + 1)] & 0xFF) << 8 | p_177439_1_[var4] & 0xFF));
          var4 += 2;
        }
      }
      else if ((p_177439_3_) && (this.storageArrays[var6] != null))
      {
        this.storageArrays[var6] = null;
      }
    }
    for (var6 = 0; var6 < this.storageArrays.length; var6++) {
      if (((p_177439_2_ & 1 << var6) != 0) && (this.storageArrays[var6] != null))
      {
        NibbleArray var10 = this.storageArrays[var6].getBlocklightArray();
        System.arraycopy(p_177439_1_, var4, var10.getData(), 0, var10.getData().length);
        var4 += var10.getData().length;
      }
    }
    if (var5) {
      for (var6 = 0; var6 < this.storageArrays.length; var6++) {
        if (((p_177439_2_ & 1 << var6) != 0) && (this.storageArrays[var6] != null))
        {
          NibbleArray var10 = this.storageArrays[var6].getSkylightArray();
          System.arraycopy(p_177439_1_, var4, var10.getData(), 0, var10.getData().length);
          var4 += var10.getData().length;
        }
      }
    }
    if (p_177439_3_)
    {
      System.arraycopy(p_177439_1_, var4, this.blockBiomeArray, 0, this.blockBiomeArray.length);
      var8 = var4 + this.blockBiomeArray.length;
    }
    for (var6 = 0; var6 < this.storageArrays.length; var6++) {
      if ((this.storageArrays[var6] != null) && ((p_177439_2_ & 1 << var6) != 0)) {
        this.storageArrays[var6].removeInvalidBlocks();
      }
    }
    this.isLightPopulated = true;
    this.isTerrainPopulated = true;
    generateHeightMap();
    Iterator var9 = this.chunkTileEntityMap.values().iterator();
    while (var9.hasNext())
    {
      TileEntity var11 = (TileEntity)var9.next();
      var11.updateContainingBlockInfo();
    }
  }
  
  public BiomeGenBase getBiome(BlockPos pos, WorldChunkManager chunkManager)
  {
    int var3 = pos.getX() & 0xF;
    int var4 = pos.getZ() & 0xF;
    int var5 = this.blockBiomeArray[(var4 << 4 | var3)] & 0xFF;
    if (var5 == 255)
    {
      BiomeGenBase var6 = chunkManager.func_180300_a(pos, BiomeGenBase.plains);
      var5 = var6.biomeID;
      this.blockBiomeArray[(var4 << 4 | var3)] = ((byte)(var5 & 0xFF));
    }
    BiomeGenBase var6 = BiomeGenBase.getBiome(var5);
    return var6 == null ? BiomeGenBase.plains : var6;
  }
  
  public byte[] getBiomeArray()
  {
    return this.blockBiomeArray;
  }
  
  public void setBiomeArray(byte[] biomeArray)
  {
    if (this.blockBiomeArray.length != biomeArray.length) {
      logger.warn("Could not set level chunk biomes, array length is " + biomeArray.length + " instead of " + this.blockBiomeArray.length);
    } else {
      for (int var2 = 0; var2 < this.blockBiomeArray.length; var2++) {
        this.blockBiomeArray[var2] = biomeArray[var2];
      }
    }
  }
  
  public void resetRelightChecks()
  {
    this.queuedLightChecks = 0;
  }
  
  public void enqueueRelightChecks()
  {
    BlockPos var1 = new BlockPos(this.xPosition << 4, 0, this.zPosition << 4);
    for (int var2 = 0; var2 < 8; var2++)
    {
      if (this.queuedLightChecks >= 4096) {
        return;
      }
      int var3 = this.queuedLightChecks % 16;
      int var4 = this.queuedLightChecks / 16 % 16;
      int var5 = this.queuedLightChecks / 256;
      this.queuedLightChecks += 1;
      for (int var6 = 0; var6 < 16; var6++)
      {
        BlockPos var7 = var1.add(var4, (var3 << 4) + var6, var5);
        boolean var8 = (var6 == 0) || (var6 == 15) || (var4 == 0) || (var4 == 15) || (var5 == 0) || (var5 == 15);
        if (((this.storageArrays[var3] == null) && (var8)) || ((this.storageArrays[var3] != null) && (this.storageArrays[var3].getBlockByExtId(var4, var6, var5).getMaterial() == Material.air)))
        {
          EnumFacing[] var9 = EnumFacing.values();
          int var10 = var9.length;
          for (int var11 = 0; var11 < var10; var11++)
          {
            EnumFacing var12 = var9[var11];
            BlockPos var13 = var7.offset(var12);
            if (this.worldObj.getBlockState(var13).getBlock().getLightValue() > 0) {
              this.worldObj.checkLight(var13);
            }
          }
          this.worldObj.checkLight(var7);
        }
      }
    }
  }
  
  public void func_150809_p()
  {
    this.isTerrainPopulated = true;
    this.isLightPopulated = true;
    BlockPos var1 = new BlockPos(this.xPosition << 4, 0, this.zPosition << 4);
    if (!this.worldObj.provider.getHasNoSky()) {
      if (this.worldObj.isAreaLoaded(var1.add(-1, 0, -1), var1.add(16, 63, 16)))
      {
        for (int var2 = 0; var2 < 16; var2++) {
          for (int var3 = 0; var3 < 16; var3++) {
            if (!func_150811_f(var2, var3))
            {
              this.isLightPopulated = false;
              break label116;
            }
          }
        }
        label116:
        if (this.isLightPopulated)
        {
          Iterator var5 = EnumFacing.Plane.HORIZONTAL.iterator();
          while (var5.hasNext())
          {
            EnumFacing var6 = (EnumFacing)var5.next();
            int var4 = var6.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 16 : 1;
            this.worldObj.getChunkFromBlockCoords(var1.offset(var6, var4)).func_180700_a(var6.getOpposite());
          }
          func_177441_y();
        }
      }
      else
      {
        this.isLightPopulated = false;
      }
    }
  }
  
  private void func_177441_y()
  {
    for (int var1 = 0; var1 < this.updateSkylightColumns.length; var1++) {
      this.updateSkylightColumns[var1] = true;
    }
    recheckGaps(false);
  }
  
  private void func_180700_a(EnumFacing p_180700_1_)
  {
    if (this.isTerrainPopulated)
    {
      if (p_180700_1_ == EnumFacing.EAST) {
        for (int var2 = 0; var2 < 16; var2++) {
          func_150811_f(15, var2);
        }
      }
      if (p_180700_1_ == EnumFacing.WEST) {
        for (int var2 = 0; var2 < 16; var2++) {
          func_150811_f(0, var2);
        }
      }
      if (p_180700_1_ == EnumFacing.SOUTH) {
        for (int var2 = 0; var2 < 16; var2++) {
          func_150811_f(var2, 15);
        }
      }
      if (p_180700_1_ == EnumFacing.NORTH) {
        for (int var2 = 0; var2 < 16; var2++) {
          func_150811_f(var2, 0);
        }
      }
    }
  }
  
  private boolean func_150811_f(int p_150811_1_, int p_150811_2_)
  {
    BlockPos var3 = new BlockPos(this.xPosition << 4, 0, this.zPosition << 4);
    int var4 = getTopFilledSegment();
    boolean var5 = false;
    boolean var6 = false;
    for (int var7 = var4 + 16 - 1; (var7 > 63) || ((var7 > 0) && (!var6)); var7--)
    {
      BlockPos var8 = var3.add(p_150811_1_, var7, p_150811_2_);
      int var9 = getBlockLightOpacity(var8);
      if ((var9 == 255) && (var7 < 63)) {
        var6 = true;
      }
      if ((!var5) && (var9 > 0)) {
        var5 = true;
      } else if ((var5) && (var9 == 0) && (!this.worldObj.checkLight(var8))) {
        return false;
      }
    }
    for (; var7 > 0; var7--)
    {
      BlockPos var8 = var3.add(p_150811_1_, var7, p_150811_2_);
      if (getBlock(var8).getLightValue() > 0) {
        this.worldObj.checkLight(var8);
      }
    }
    return true;
  }
  
  public boolean isLoaded()
  {
    return this.isChunkLoaded;
  }
  
  public void func_177417_c(boolean p_177417_1_)
  {
    this.isChunkLoaded = p_177417_1_;
  }
  
  public World getWorld()
  {
    return this.worldObj;
  }
  
  public int[] getHeightMap()
  {
    return this.heightMap;
  }
  
  public void setHeightMap(int[] newHeightMap)
  {
    if (this.heightMap.length != newHeightMap.length) {
      logger.warn("Could not set level chunk heightmap, array length is " + newHeightMap.length + " instead of " + this.heightMap.length);
    } else {
      for (int var2 = 0; var2 < this.heightMap.length; var2++) {
        this.heightMap[var2] = newHeightMap[var2];
      }
    }
  }
  
  public Map getTileEntityMap()
  {
    return this.chunkTileEntityMap;
  }
  
  public ClassInheratanceMultiMap[] getEntityLists()
  {
    return this.entityLists;
  }
  
  public boolean isTerrainPopulated()
  {
    return this.isTerrainPopulated;
  }
  
  public void setTerrainPopulated(boolean terrainPopulated)
  {
    this.isTerrainPopulated = terrainPopulated;
  }
  
  public boolean isLightPopulated()
  {
    return this.isLightPopulated;
  }
  
  public void setLightPopulated(boolean lightPopulated)
  {
    this.isLightPopulated = lightPopulated;
  }
  
  public void setModified(boolean modified)
  {
    this.isModified = modified;
  }
  
  public void setHasEntities(boolean hasEntitiesIn)
  {
    this.hasEntities = hasEntitiesIn;
  }
  
  public void setLastSaveTime(long saveTime)
  {
    this.lastSaveTime = saveTime;
  }
  
  public int getLowestHeight()
  {
    return this.heightMapMinimum;
  }
  
  public long getInhabitedTime()
  {
    return this.inhabitedTime;
  }
  
  public void setInhabitedTime(long newInhabitedTime)
  {
    this.inhabitedTime = newInhabitedTime;
  }
  
  public static enum EnumCreateEntityType
  {
    private static final EnumCreateEntityType[] $VALUES = { IMMEDIATE, QUEUED, CHECK };
    private static final String __OBFID = "CL_00002009";
  }
}
