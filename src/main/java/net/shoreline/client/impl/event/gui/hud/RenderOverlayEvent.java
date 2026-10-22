package net.shoreline.client.impl.event.gui.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

public class RenderOverlayEvent extends Event {
   private final GuiGraphics context;

   public RenderOverlayEvent(GuiGraphics context) {
      this.context = context;
   }

   public GuiGraphics getContext() {
      return this.context;
   }

   @Cancelable
   public static class Block extends RenderOverlayEvent {
      public Block(GuiGraphics context) {
         super(context);
      }
   }

   @Cancelable
   public static class BossBar extends RenderOverlayEvent {
      public BossBar(GuiGraphics context) {
         super(context);
      }
   }

   @Cancelable
   public static class Fire extends RenderOverlayEvent {
      public Fire(GuiGraphics context) {
         super(context);
      }
   }

   @Cancelable
   public static class Frostbite extends RenderOverlayEvent {
      public Frostbite(GuiGraphics context) {
         super(context);
      }
   }

   @Cancelable
   public static class Hotbar extends RenderOverlayEvent {
      public Hotbar(GuiGraphics context) {
         super(context);
      }
   }

   @Cancelable
   public static class ItemName extends RenderOverlayEvent {
      private int x;
      private int y;

      public ItemName(GuiGraphics context) {
         super(context);
      }

      public boolean isUpdateXY() {
         return this.x != 0 && this.y != 0;
      }

      public int getX() {
         return this.x;
      }

      public void setX(int x) {
         this.x = x;
      }

      public int getY() {
         return this.y;
      }

      public void setY(int y) {
         this.y = y;
      }
   }

   @Cancelable
   public static class Portal extends RenderOverlayEvent {
      public Portal(GuiGraphics context) {
         super(context);
      }
   }

   public static class Post extends RenderOverlayEvent {
      private final float tickDelta;

      public Post(GuiGraphics context, float tickDelta) {
         super(context);
         this.tickDelta = tickDelta;
      }

      public float getTickDelta() {
         return this.tickDelta;
      }
   }

   @Cancelable
   public static class Pumpkin extends RenderOverlayEvent {
      public Pumpkin(GuiGraphics context) {
         super(context);
      }
   }

   @Cancelable
   public static class Spyglass extends RenderOverlayEvent {
      public Spyglass(GuiGraphics context) {
         super(context);
      }
   }

   @Cancelable
   public static class StatusEffect extends RenderOverlayEvent {
      public StatusEffect(GuiGraphics context) {
         super(context);
      }
   }

   @Cancelable
   public static class Water extends RenderOverlayEvent {
      public Water(GuiGraphics context) {
         super(context);
      }
   }
}
