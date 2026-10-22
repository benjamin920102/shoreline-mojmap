package net.shoreline.client.mixin.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.shoreline.client.impl.event.gui.screen.ConnectScreenEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ConnectScreen.class})
public class MixinConnectScreen {
   @Inject(
      method = {"connect(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/client/multiplayer/TransferState;)V"},
      at = {@At("HEAD")}
   )
   private void onConnect(Minecraft client, ServerAddress address, ServerData info, TransferState cookieStorage, CallbackInfo ci) {
      ConnectScreenEvent connectScreenEvent = new ConnectScreenEvent(address, info);
      EventBus.INSTANCE.dispatch(connectScreenEvent);
   }
}
