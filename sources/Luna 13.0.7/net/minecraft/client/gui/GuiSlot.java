package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

public abstract class GuiSlot
{
  protected final Minecraft mc;
  protected int width;
  protected int height;
  protected int top;
  protected int bottom;
  protected int right;
  protected int left;
  protected final int slotHeight;
  private int scrollUpButtonID;
  private int scrollDownButtonID;
  protected int mouseX;
  protected int mouseY;
  protected boolean field_148163_i = true;
  protected float initialClickY = -2.0F;
  protected float scrollMultiplier;
  protected float amountScrolled;
  protected int selectedElement = -1;
  protected long lastClicked;
  protected boolean field_178041_q = true;
  protected boolean showSelectionBox = true;
  protected boolean hasListHeader;
  protected int headerPadding;
  private boolean enabled = true;
  private static final String __OBFID = "CL_00000679";
  
  public GuiSlot(Minecraft mcIn, int width, int height, int p_i1052_4_, int p_i1052_5_, int p_i1052_6_)
  {
    this.mc = mcIn;
    this.width = width;
    this.height = height;
    this.top = p_i1052_4_;
    this.bottom = p_i1052_5_;
    this.slotHeight = p_i1052_6_;
    this.left = 0;
    this.right = width;
  }
  
  public void setDimensions(int p_148122_1_, int p_148122_2_, int p_148122_3_, int p_148122_4_)
  {
    this.width = p_148122_1_;
    this.height = p_148122_2_;
    this.top = p_148122_3_;
    this.bottom = p_148122_4_;
    this.left = 0;
    this.right = p_148122_1_;
  }
  
  public void setShowSelectionBox(boolean p_148130_1_)
  {
    this.showSelectionBox = p_148130_1_;
  }
  
  protected void setHasListHeader(boolean p_148133_1_, int p_148133_2_)
  {
    this.hasListHeader = p_148133_1_;
    this.headerPadding = p_148133_2_;
    if (!p_148133_1_) {
      this.headerPadding = 0;
    }
  }
  
  protected abstract int getSize();
  
  protected abstract void elementClicked(int paramInt1, boolean paramBoolean, int paramInt2, int paramInt3);
  
  protected abstract boolean isSelected(int paramInt);
  
  protected int getContentHeight()
  {
    return getSize() * this.slotHeight + this.headerPadding;
  }
  
  protected abstract void drawBackground();
  
  protected void func_178040_a(int p_178040_1_, int p_178040_2_, int p_178040_3_) {}
  
  protected abstract void drawSlot(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6);
  
  protected void drawListHeader(int p_148129_1_, int p_148129_2_, Tessellator p_148129_3_) {}
  
  protected void func_148132_a(int p_148132_1_, int p_148132_2_) {}
  
  protected void func_148142_b(int p_148142_1_, int p_148142_2_) {}
  
  public int getSlotIndexFromScreenCoords(int p_148124_1_, int p_148124_2_)
  {
    int var3 = this.left + this.width / 2 - getListWidth() / 2;
    int var4 = this.left + this.width / 2 + getListWidth() / 2;
    int var5 = p_148124_2_ - this.top - this.headerPadding + (int)this.amountScrolled - 4;
    int var6 = var5 / this.slotHeight;
    return (p_148124_1_ < getScrollBarX()) && (p_148124_1_ >= var3) && (p_148124_1_ <= var4) && (var6 >= 0) && (var5 >= 0) && (var6 < getSize()) ? var6 : -1;
  }
  
  public void registerScrollButtons(int p_148134_1_, int p_148134_2_)
  {
    this.scrollUpButtonID = p_148134_1_;
    this.scrollDownButtonID = p_148134_2_;
  }
  
  protected void bindAmountScrolled()
  {
    int var1 = func_148135_f();
    if (var1 < 0) {
      var1 /= 2;
    }
    if ((!this.field_148163_i) && (var1 < 0)) {
      var1 = 0;
    }
    this.amountScrolled = MathHelper.clamp_float(this.amountScrolled, 0.0F, var1);
  }
  
  public int func_148135_f()
  {
    return Math.max(0, getContentHeight() - (this.bottom - this.top - 4));
  }
  
  public int getAmountScrolled()
  {
    return (int)this.amountScrolled;
  }
  
  public boolean isMouseYWithinSlotBounds(int p_148141_1_)
  {
    return (p_148141_1_ >= this.top) && (p_148141_1_ <= this.bottom) && (this.mouseX >= this.left) && (this.mouseX <= this.right);
  }
  
  public void scrollBy(int p_148145_1_)
  {
    this.amountScrolled += p_148145_1_;
    bindAmountScrolled();
    this.initialClickY = -2.0F;
  }
  
  public void actionPerformed(GuiButton p_148147_1_)
  {
    if (p_148147_1_.enabled) {
      if (p_148147_1_.id == this.scrollUpButtonID)
      {
        this.amountScrolled -= this.slotHeight * 2 / 3;
        this.initialClickY = -2.0F;
        bindAmountScrolled();
      }
      else if (p_148147_1_.id == this.scrollDownButtonID)
      {
        this.amountScrolled += this.slotHeight * 2 / 3;
        this.initialClickY = -2.0F;
        bindAmountScrolled();
      }
    }
  }
  
  public void drawScreen(int p_148128_1_, int p_148128_2_, float p_148128_3_)
  {
    if (this.field_178041_q)
    {
      this.mouseX = p_148128_1_;
      this.mouseY = p_148128_2_;
      drawBackground();
      int var4 = getScrollBarX();
      int var5 = var4 + 6;
      bindAmountScrolled();
      GlStateManager.disableLighting();
      GlStateManager.disableFog();
      Tessellator var6 = Tessellator.getInstance();
      WorldRenderer var7 = var6.getWorldRenderer();
      this.mc.getTextureManager().bindTexture(Gui.optionsBackground);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      float var8 = 32.0F;
      var7.startDrawingQuads();
      var7.func_178991_c(2105376);
      var7.addVertexWithUV(this.left, this.bottom, 0.0D, this.left / var8, (this.bottom + (int)this.amountScrolled) / var8);
      var7.addVertexWithUV(this.right, this.bottom, 0.0D, this.right / var8, (this.bottom + (int)this.amountScrolled) / var8);
      var7.addVertexWithUV(this.right, this.top, 0.0D, this.right / var8, (this.top + (int)this.amountScrolled) / var8);
      var7.addVertexWithUV(this.left, this.top, 0.0D, this.left / var8, (this.top + (int)this.amountScrolled) / var8);
      var6.draw();
      int var9 = this.left + this.width / 2 - getListWidth() / 2 + 2;
      int var10 = this.top + 4 - (int)this.amountScrolled;
      if (this.hasListHeader) {
        drawListHeader(var9, var10, var6);
      }
      drawSelectionBox(var9, var10, p_148128_1_, p_148128_2_);
      GlStateManager.disableDepth();
      byte var11 = 4;
      overlayBackground(0, this.top, 255, 255);
      overlayBackground(this.bottom, this.height, 255, 255);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
      GlStateManager.disableAlpha();
      GlStateManager.shadeModel(7425);
      GlStateManager.func_179090_x();
      var7.startDrawingQuads();
      var7.func_178974_a(0, 0);
      var7.addVertexWithUV(this.left, this.top + var11, 0.0D, 0.0D, 1.0D);
      var7.addVertexWithUV(this.right, this.top + var11, 0.0D, 1.0D, 1.0D);
      var7.func_178974_a(0, 255);
      var7.addVertexWithUV(this.right, this.top, 0.0D, 1.0D, 0.0D);
      var7.addVertexWithUV(this.left, this.top, 0.0D, 0.0D, 0.0D);
      var6.draw();
      var7.startDrawingQuads();
      var7.func_178974_a(0, 255);
      var7.addVertexWithUV(this.left, this.bottom, 0.0D, 0.0D, 1.0D);
      var7.addVertexWithUV(this.right, this.bottom, 0.0D, 1.0D, 1.0D);
      var7.func_178974_a(0, 0);
      var7.addVertexWithUV(this.right, this.bottom - var11, 0.0D, 1.0D, 0.0D);
      var7.addVertexWithUV(this.left, this.bottom - var11, 0.0D, 0.0D, 0.0D);
      var6.draw();
      int var12 = func_148135_f();
      if (var12 > 0)
      {
        int var13 = (this.bottom - this.top) * (this.bottom - this.top) / getContentHeight();
        var13 = MathHelper.clamp_int(var13, 32, this.bottom - this.top - 8);
        int var14 = (int)this.amountScrolled * (this.bottom - this.top - var13) / var12 + this.top;
        if (var14 < this.top) {
          var14 = this.top;
        }
        var7.startDrawingQuads();
        var7.func_178974_a(0, 255);
        var7.addVertexWithUV(var4, this.bottom, 0.0D, 0.0D, 1.0D);
        var7.addVertexWithUV(var5, this.bottom, 0.0D, 1.0D, 1.0D);
        var7.addVertexWithUV(var5, this.top, 0.0D, 1.0D, 0.0D);
        var7.addVertexWithUV(var4, this.top, 0.0D, 0.0D, 0.0D);
        var6.draw();
        var7.startDrawingQuads();
        var7.func_178974_a(8421504, 255);
        var7.addVertexWithUV(var4, var14 + var13, 0.0D, 0.0D, 1.0D);
        var7.addVertexWithUV(var5, var14 + var13, 0.0D, 1.0D, 1.0D);
        var7.addVertexWithUV(var5, var14, 0.0D, 1.0D, 0.0D);
        var7.addVertexWithUV(var4, var14, 0.0D, 0.0D, 0.0D);
        var6.draw();
        var7.startDrawingQuads();
        var7.func_178974_a(12632256, 255);
        var7.addVertexWithUV(var4, var14 + var13 - 1, 0.0D, 0.0D, 1.0D);
        var7.addVertexWithUV(var5 - 1, var14 + var13 - 1, 0.0D, 1.0D, 1.0D);
        var7.addVertexWithUV(var5 - 1, var14, 0.0D, 1.0D, 0.0D);
        var7.addVertexWithUV(var4, var14, 0.0D, 0.0D, 0.0D);
        var6.draw();
      }
      func_148142_b(p_148128_1_, p_148128_2_);
      GlStateManager.func_179098_w();
      GlStateManager.shadeModel(7424);
      GlStateManager.enableAlpha();
      GlStateManager.disableBlend();
    }
  }
  
  public void func_178039_p()
  {
    if (isMouseYWithinSlotBounds(this.mouseY))
    {
      if ((Mouse.isButtonDown(0)) && (getEnabled()))
      {
        if (this.initialClickY == -1.0F)
        {
          boolean var1 = true;
          if ((this.mouseY >= this.top) && (this.mouseY <= this.bottom))
          {
            int var2 = this.width / 2 - getListWidth() / 2;
            int var3 = this.width / 2 + getListWidth() / 2;
            int var4 = this.mouseY - this.top - this.headerPadding + (int)this.amountScrolled - 4;
            int var5 = var4 / this.slotHeight;
            if ((this.mouseX >= var2) && (this.mouseX <= var3) && (var5 >= 0) && (var4 >= 0) && (var5 < getSize()))
            {
              boolean var6 = (var5 == this.selectedElement) && (Minecraft.getSystemTime() - this.lastClicked < 250L);
              elementClicked(var5, var6, this.mouseX, this.mouseY);
              this.selectedElement = var5;
              this.lastClicked = Minecraft.getSystemTime();
            }
            else if ((this.mouseX >= var2) && (this.mouseX <= var3) && (var4 < 0))
            {
              func_148132_a(this.mouseX - var2, this.mouseY - this.top + (int)this.amountScrolled - 4);
              var1 = false;
            }
            int var11 = getScrollBarX();
            int var7 = var11 + 6;
            if ((this.mouseX >= var11) && (this.mouseX <= var7))
            {
              this.scrollMultiplier = -1.0F;
              int var8 = func_148135_f();
              if (var8 < 1) {
                var8 = 1;
              }
              int var9 = (int)((this.bottom - this.top) * (this.bottom - this.top) / getContentHeight());
              var9 = MathHelper.clamp_int(var9, 32, this.bottom - this.top - 8);
              this.scrollMultiplier /= (this.bottom - this.top - var9) / var8;
            }
            else
            {
              this.scrollMultiplier = 1.0F;
            }
            if (var1) {
              this.initialClickY = this.mouseY;
            } else {
              this.initialClickY = -2.0F;
            }
          }
          else
          {
            this.initialClickY = -2.0F;
          }
        }
        else if (this.initialClickY >= 0.0F)
        {
          this.amountScrolled -= (this.mouseY - this.initialClickY) * this.scrollMultiplier;
          this.initialClickY = this.mouseY;
        }
      }
      else {
        this.initialClickY = -1.0F;
      }
      int var10 = Mouse.getEventDWheel();
      if (var10 != 0)
      {
        if (var10 > 0) {
          var10 = -1;
        } else if (var10 < 0) {
          var10 = 1;
        }
        this.amountScrolled += var10 * this.slotHeight / 2;
      }
    }
  }
  
  public void setEnabled(boolean p_148143_1_)
  {
    this.enabled = p_148143_1_;
  }
  
  public boolean getEnabled()
  {
    return this.enabled;
  }
  
  public int getListWidth()
  {
    return 220;
  }
  
  protected void drawSelectionBox(int p_148120_1_, int p_148120_2_, int p_148120_3_, int p_148120_4_)
  {
    int var5 = getSize();
    Tessellator var6 = Tessellator.getInstance();
    WorldRenderer var7 = var6.getWorldRenderer();
    for (int var8 = 0; var8 < var5; var8++)
    {
      int var9 = p_148120_2_ + var8 * this.slotHeight + this.headerPadding;
      int var10 = this.slotHeight - 4;
      if ((var9 > this.bottom) || (var9 + var10 < this.top)) {
        func_178040_a(var8, p_148120_1_, var9);
      }
      if ((this.showSelectionBox) && (isSelected(var8)))
      {
        int var11 = this.left + (this.width / 2 - getListWidth() / 2);
        int var12 = this.left + this.width / 2 + getListWidth() / 2;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179090_x();
        var7.startDrawingQuads();
        var7.func_178991_c(8421504);
        var7.addVertexWithUV(var11, var9 + var10 + 2, 0.0D, 0.0D, 1.0D);
        var7.addVertexWithUV(var12, var9 + var10 + 2, 0.0D, 1.0D, 1.0D);
        var7.addVertexWithUV(var12, var9 - 2, 0.0D, 1.0D, 0.0D);
        var7.addVertexWithUV(var11, var9 - 2, 0.0D, 0.0D, 0.0D);
        var7.func_178991_c(0);
        var7.addVertexWithUV(var11 + 1, var9 + var10 + 1, 0.0D, 0.0D, 1.0D);
        var7.addVertexWithUV(var12 - 1, var9 + var10 + 1, 0.0D, 1.0D, 1.0D);
        var7.addVertexWithUV(var12 - 1, var9 - 1, 0.0D, 1.0D, 0.0D);
        var7.addVertexWithUV(var11 + 1, var9 - 1, 0.0D, 0.0D, 0.0D);
        var6.draw();
        GlStateManager.func_179098_w();
      }
      drawSlot(var8, p_148120_1_, var9, var10, p_148120_3_, p_148120_4_);
    }
  }
  
  protected int getScrollBarX()
  {
    return this.width / 2 + 124;
  }
  
  protected void overlayBackground(int p_148136_1_, int p_148136_2_, int p_148136_3_, int p_148136_4_)
  {
    Tessellator var5 = Tessellator.getInstance();
    WorldRenderer var6 = var5.getWorldRenderer();
    this.mc.getTextureManager().bindTexture(Gui.optionsBackground);
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    float var7 = 32.0F;
    var6.startDrawingQuads();
    var6.func_178974_a(4210752, p_148136_4_);
    var6.addVertexWithUV(this.left, p_148136_2_, 0.0D, 0.0D, p_148136_2_ / var7);
    var6.addVertexWithUV(this.left + this.width, p_148136_2_, 0.0D, this.width / var7, p_148136_2_ / var7);
    var6.func_178974_a(4210752, p_148136_3_);
    var6.addVertexWithUV(this.left + this.width, p_148136_1_, 0.0D, this.width / var7, p_148136_1_ / var7);
    var6.addVertexWithUV(this.left, p_148136_1_, 0.0D, 0.0D, p_148136_1_ / var7);
    var5.draw();
  }
  
  public void setSlotXBoundsFromLeft(int p_148140_1_)
  {
    this.left = p_148140_1_;
    this.right = (p_148140_1_ + this.width);
  }
  
  public int getSlotHeight()
  {
    return this.slotHeight;
  }
}
