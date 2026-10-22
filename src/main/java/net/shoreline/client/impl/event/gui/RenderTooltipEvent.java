package net.shoreline.client.impl.event.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.StageEvent;

@Cancelable
public class RenderTooltipEvent extends StageEvent {
   public final GuiGraphics context;
   private final ItemStack stack;
   private final int x;
   private final int y;

   public RenderTooltipEvent(GuiGraphics context, ItemStack stack, int x, int y) {
      this.context = context;
      this.stack = stack;
      this.x = x;
      this.y = y;
   }

   public GuiGraphics getContext() {
      return this.context;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }
}
