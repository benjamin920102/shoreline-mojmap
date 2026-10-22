package net.shoreline.client.mixin.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.impl.event.network.CapesEvent;
import net.shoreline.client.impl.event.network.LoadCapeEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public class MixinPlayerListEntry implements Globals
{

    @Shadow
    @Final
    private GameProfile profile;
    @Unique
    private ResourceLocation capeTexture;
    @Unique
    private boolean capeTextureLoaded;

    /**
     * @param profile
     * @param secureChatEnforced
     * @param ci
     */
    @Inject(method = "<init>(Lcom/mojang/authlib/GameProfile;Z)V", at = @At("TAIL"))
    private void hookInit(GameProfile profile, boolean secureChatEnforced, CallbackInfo ci)
    {
        if (capeTextureLoaded)
        {
            return;
        }
        LoadCapeEvent loadCapeEvent = new LoadCapeEvent(profile, identifier ->
        {
            capeTexture = identifier;
        });
        EventBus.INSTANCE.dispatch(loadCapeEvent);
        capeTextureLoaded = true;
    }

    /**
     * @param cir
     */
    @Inject(method = "getSkin", at = @At("TAIL"), cancellable = true)
    private void hookGetSkinTextures(CallbackInfoReturnable<PlayerSkin> cir)
    {
        CapesEvent capesEvent = new CapesEvent(profile);
        EventBus.INSTANCE.dispatch(capesEvent);
        if (!capesEvent.isCanceled())
        {
            return;
        }
        ResourceLocation identifier = capesEvent.getShowOptifine() ? capeTexture : null;
        if (capesEvent.getTexture() != null)
        {
            identifier = capesEvent.getTexture();
        }
        if (identifier != null)
        {
            PlayerSkin t = cir.getReturnValue();
            PlayerSkin customCapeTexture = new PlayerSkin(t.texture(), t.textureUrl(), identifier, identifier, t.model(), t.secure());
            cir.setReturnValue(customCapeTexture);
        }
    }
}
