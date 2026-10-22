package net.shoreline.client.mixin.gui.hud;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.shoreline.client.impl.event.gui.hud.PlayerListColumnsEvent;
import net.shoreline.client.impl.event.gui.hud.PlayerListEvent;
import net.shoreline.client.impl.event.gui.hud.PlayerListIconEvent;
import net.shoreline.client.impl.event.gui.hud.PlayerListNameEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerTabOverlay.class})
public abstract class MixinPlayerListHud {
   @Shadow
   @Final
   private static Comparator<PlayerInfo> PLAYER_COMPARATOR;
   @Shadow
   @Final
   private Minecraft minecraft;

   @Shadow
   protected abstract List<PlayerInfo> getPlayerInfos();

   @Shadow
   protected abstract Component decorateName(PlayerInfo var1, MutableComponent var2);

   @Inject(
      method = {"getNameForDisplay(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetPlayerName(PlayerInfo entry, CallbackInfoReturnable<Component> cir) {
      Component text;
      if (entry.getTabListDisplayName() != null) {
         text = this.decorateName(entry, entry.getTabListDisplayName().copy());
      } else {
         text = this.decorateName(entry, PlayerTeam.formatNameForTeam(entry.getTeam(), Component.literal(entry.getProfile().getName())));
      }

      PlayerListNameEvent playerListNameEvent = new PlayerListNameEvent(text, entry.getProfile().getId());
      EventBus.INSTANCE.dispatch(playerListNameEvent);
      if (playerListNameEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(playerListNameEvent.getPlayerName());
      }
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I"
      )
   )
   private int hookRender(Font instance, FormattedText text) {
      PlayerListIconEvent.Width playerListIconEvent = new PlayerListIconEvent.Width(text.getString());
      EventBus.INSTANCE.dispatch(playerListIconEvent);
      return playerListIconEvent.isCanceled() ? instance.width(text) + 12 : instance.width(text);
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"
      )
   )
   private int hookRender(GuiGraphics instance, Font textRenderer, Component text, int x, int y, int color) {
      PlayerListIconEvent.Render playerListIconEvent = new PlayerListIconEvent.Render(text, instance, textRenderer, x, y, color);
      EventBus.INSTANCE.dispatch(playerListIconEvent);
      int x1 = playerListIconEvent.isCanceled() ? x + 12 : x;
      instance.drawString(textRenderer, text, x1, y, color);
      return x1;
   }

   @Inject(
      method = {"getPlayerInfos()Ljava/util/List;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookCollectPlayerEntries(CallbackInfoReturnable<List<PlayerInfo>> cir) {
      PlayerListEvent playerListEvent = new PlayerListEvent();
      EventBus.INSTANCE.dispatch(playerListEvent);
      if (playerListEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(
            this.minecraft.player.connection.getListedOnlinePlayers().stream().sorted(PLAYER_COMPARATOR).limit(playerListEvent.getSize()).toList()
         );
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/lang/Math;min(II)I",
         shift = Shift.BEFORE
      )}
   )
   private void hookRender(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
      int newP = 1;
      int newO;
      int totalPlayers = newO = this.getPlayerInfos().size();
      PlayerListColumnsEvent playerListColumsEvent = new PlayerListColumnsEvent();
      EventBus.INSTANCE.dispatch(playerListColumsEvent);
      if (playerListColumsEvent.isCanceled()) {
         while (newO > playerListColumsEvent.getTabHeight()) {
            newP++;
            newO = (totalPlayers + newP - 1) / newP;
         }

         o.set(newO);
         p.set(newP);
      }
   }
}
