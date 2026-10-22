package net.shoreline.client.mixin.gui.screen;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(Screen.class)
public abstract class MixinScreen
{
    //
    @Shadow
    public int width;
    //
    @Shadow
    public int height;
    //
    @Shadow
    @Final
    private List<Renderable> renderables;

    @Shadow
    @Final
    protected Component title;

    @Shadow
    @Nullable
    protected Minecraft minecraft;

    /**
     * @param drawableElement
     * @param <T>
     * @return
     */
    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addDrawableChild(T drawableElement);

    /**
     *
     */
    @Shadow
    public void tick()
    {

    }

    /**
     * @return
     */
    @Unique
    public List<Renderable> getDrawables()
    {
        return renderables;
    }
}
