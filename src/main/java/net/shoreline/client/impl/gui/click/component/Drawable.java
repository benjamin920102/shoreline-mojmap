package net.shoreline.client.impl.gui.click.component;

import net.minecraft.client.gui.GuiGraphics;

/**
 * @author linus
 * @since 1.0
 */
public interface Drawable
{
    /**
     * @param context
     * @param mouseX
     * @param mouseY
     * @param delta
     */
    void render(GuiGraphics context, float mouseX, float mouseY, float delta);
}
