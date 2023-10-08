package net.minecraft.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatComponentTranslation extends ChatComponentStyle {
   private final String key;
   private final Object[] formatArgs;
   private final Object syncLock = new Object();
   private long lastTranslationUpdateTimeInMilliseconds = -1L;
   List children = Lists.newArrayList();
   public static final Pattern stringVariablePattern = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

   public ChatComponentTranslation(String translationKey, Object... args) {
      this.key = translationKey;
      this.formatArgs = args;
      Object[] var3 = args;
      int var4 = args.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Object object = var3[var5];
         if (object instanceof IChatComponent) {
            ((IChatComponent)object).getChatStyle().setParentStyle(this.getChatStyle());
         }
      }

   }

   synchronized void ensureInitialized() {
      synchronized(this.syncLock) {
         long i = StatCollector.getLastTranslationUpdateTimeInMilliseconds();
         if (i == this.lastTranslationUpdateTimeInMilliseconds) {
            return;
         }

         this.lastTranslationUpdateTimeInMilliseconds = i;
         this.children.clear();
      }

      try {
         this.initializeFromFormat(StatCollector.translateToLocal(this.key));
      } catch (ChatComponentTranslationFormatException var6) {
         this.children.clear();

         try {
            this.initializeFromFormat(StatCollector.translateToFallback(this.key));
         } catch (ChatComponentTranslationFormatException var5) {
            throw var6;
         }
      }

   }

   protected void initializeFromFormat(String format) {
      boolean flag = false;
      Matcher matcher = stringVariablePattern.matcher(format);
      int i = 0;
      int j = 0;

      try {
         int l;
         for(; matcher.find(j); j = l) {
            int k = matcher.start();
            l = matcher.end();
            if (k > j) {
               ChatComponentText chatcomponenttext = new ChatComponentText(String.format(format.substring(j, k)));
               chatcomponenttext.getChatStyle().setParentStyle(this.getChatStyle());
               this.children.add(chatcomponenttext);
            }

            String s2 = matcher.group(2);
            String s = format.substring(k, l);
            if ("%".equals(s2) && "%%".equals(s)) {
               ChatComponentText chatcomponenttext2 = new ChatComponentText("%");
               chatcomponenttext2.getChatStyle().setParentStyle(this.getChatStyle());
               this.children.add(chatcomponenttext2);
            } else {
               if (!"s".equals(s2)) {
                  throw new ChatComponentTranslationFormatException(this, "Unsupported format: '" + s + "'");
               }

               String s1 = matcher.group(1);
               int i1 = s1 != null ? Integer.parseInt(s1) - 1 : i++;
               if (i1 < this.formatArgs.length) {
                  this.children.add(this.getFormatArgumentAsComponent(i1));
               }
            }
         }

         if (j < format.length()) {
            ChatComponentText chatcomponenttext1 = new ChatComponentText(String.format(format.substring(j)));
            chatcomponenttext1.getChatStyle().setParentStyle(this.getChatStyle());
            this.children.add(chatcomponenttext1);
         }

      } catch (IllegalFormatException var12) {
         throw new ChatComponentTranslationFormatException(this, var12);
      }
   }

   private IChatComponent getFormatArgumentAsComponent(int index) {
      if (index >= this.formatArgs.length) {
         throw new ChatComponentTranslationFormatException(this, index);
      } else {
         Object object = this.formatArgs[index];
         Object ichatcomponent;
         if (object instanceof IChatComponent) {
            ichatcomponent = (IChatComponent)object;
         } else {
            ichatcomponent = new ChatComponentText(object == null ? "null" : object.toString());
            ((IChatComponent)ichatcomponent).getChatStyle().setParentStyle(this.getChatStyle());
         }

         return (IChatComponent)ichatcomponent;
      }
   }

   public IChatComponent setChatStyle(ChatStyle style) {
      super.setChatStyle(style);
      Object[] var2 = this.formatArgs;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Object object = var2[var4];
         if (object instanceof IChatComponent) {
            ((IChatComponent)object).getChatStyle().setParentStyle(this.getChatStyle());
         }
      }

      if (this.lastTranslationUpdateTimeInMilliseconds > -1L) {
         Iterator var6 = this.children.iterator();

         while(var6.hasNext()) {
            IChatComponent ichatcomponent = (IChatComponent)var6.next();
            ichatcomponent.getChatStyle().setParentStyle(style);
         }
      }

      return this;
   }

   public Iterator iterator() {
      this.ensureInitialized();
      return Iterators.concat(createDeepCopyIterator(this.children), createDeepCopyIterator(this.siblings));
   }

   public String getUnformattedTextForChat() {
      this.ensureInitialized();
      StringBuilder stringbuilder = new StringBuilder();
      Iterator var2 = this.children.iterator();

      while(var2.hasNext()) {
         IChatComponent ichatcomponent = (IChatComponent)var2.next();
         stringbuilder.append(ichatcomponent.getUnformattedTextForChat());
      }

      return stringbuilder.toString();
   }

   public ChatComponentTranslation createCopy() {
      Object[] aobject = new Object[this.formatArgs.length];

      for(int i = 0; i < this.formatArgs.length; ++i) {
         if (this.formatArgs[i] instanceof IChatComponent) {
            aobject[i] = ((IChatComponent)this.formatArgs[i]).createCopy();
         } else {
            aobject[i] = this.formatArgs[i];
         }
      }

      ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation(this.key, aobject);
      chatcomponenttranslation.setChatStyle(this.getChatStyle().createShallowCopy());
      Iterator var3 = this.getSiblings().iterator();

      while(var3.hasNext()) {
         IChatComponent ichatcomponent = (IChatComponent)var3.next();
         chatcomponenttranslation.appendSibling(ichatcomponent.createCopy());
      }

      return chatcomponenttranslation;
   }

   public boolean equals(Object p_equals_1_) {
      if (this == p_equals_1_) {
         return true;
      } else if (!(p_equals_1_ instanceof ChatComponentTranslation)) {
         return false;
      } else {
         ChatComponentTranslation chatcomponenttranslation = (ChatComponentTranslation)p_equals_1_;
         return Arrays.equals(this.formatArgs, chatcomponenttranslation.formatArgs) && this.key.equals(chatcomponenttranslation.key) && super.equals(p_equals_1_);
      }
   }

   public int hashCode() {
      int i = super.hashCode();
      i = 31 * i + this.key.hashCode();
      i = 31 * i + Arrays.hashCode(this.formatArgs);
      return i;
   }

   public String toString() {
      return "TranslatableComponent{key='" + this.key + '\'' + ", args=" + Arrays.toString(this.formatArgs) + ", siblings=" + this.siblings + ", style=" + this.getChatStyle() + '}';
   }

   public String getKey() {
      return this.key;
   }

   public Object[] getFormatArgs() {
      return this.formatArgs;
   }
}
