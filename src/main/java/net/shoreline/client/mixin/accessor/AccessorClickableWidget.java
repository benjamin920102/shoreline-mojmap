package net.shoreline.client.mixin.accessor;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 *
 */
@Mixin(AbstractWidget.class)
public interface AccessorClickableWidget
{
    /**
     * @param message
     */
    @Accessor("message")
    void setMessage(Component message);
}
