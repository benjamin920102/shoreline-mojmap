package net.shoreline.client.mixin.gui.screen;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin({Screen.class})
public abstract class MixinScreen {
   @Shadow
   public int width;
   @Shadow
   public int height;
   @Shadow
   @Final
   private List<Renderable> renderables;
   @Shadow
   @Final
   protected Component title;
   @Shadow
   @Nullable
   protected Minecraft minecraft;

   @Shadow
   protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T var1);

   @Shadow
   public void tick() {
   }

   @Unique
   public List<Renderable> getDrawables() {
      return this.renderables;
   }
}
