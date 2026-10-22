package net.shoreline.client.impl.event.gui.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class PlayerListIconEvent extends Event {
   @Cancelable
   public static class Render extends PlayerListIconEvent {
      private final Component playerName;
      private final GuiGraphics context;
      private final Font textRenderer;
      private final double x;
      private final double y;
      private final int color;

      public Render(Component playerName, GuiGraphics context, Font textRenderer, double x, double y, int color) {
         this.context = context;
         this.textRenderer = textRenderer;
         this.playerName = playerName;
         this.x = x;
         this.y = y;
         this.color = color;
      }

      public GuiGraphics getContext() {
         return this.context;
      }

      public PoseStack getMatrixStack() {
         return this.context.pose();
      }

      public Font getTextRenderer() {
         return this.textRenderer;
      }

      public double getX() {
         return this.x;
      }

      public double getY() {
         return this.y;
      }

      public int getColor() {
         return this.color;
      }

      public Component getPlayerNameText() {
         return this.playerName;
      }
   }

   @Cancelable
   public static class Width extends PlayerListIconEvent {
      private final String text;

      public Width(String text) {
         this.text = text;
      }

      public String getText() {
         return this.text;
      }
   }
}
