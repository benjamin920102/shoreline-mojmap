package net.shoreline.client.impl.event.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class CapesEvent extends Event
{
    private final GameProfile gameProfile;
    private ResourceLocation texture;
    private boolean optifine;

    public CapesEvent(GameProfile gameProfile)
    {
        this.gameProfile = gameProfile;
    }

    public GameProfile getGameProfile()
    {
        return gameProfile;
    }

    public void setTexture(ResourceLocation texture)
    {
        this.texture = texture;
    }

    public ResourceLocation getTexture()
    {
        return texture;
    }

    public void setShowOptifine(boolean optifine)
    {
        this.optifine = optifine;
    }

    public boolean getShowOptifine()
    {
        return optifine;
    }
}
