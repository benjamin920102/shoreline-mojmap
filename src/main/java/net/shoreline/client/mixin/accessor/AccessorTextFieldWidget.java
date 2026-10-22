package net.shoreline.client.mixin.accessor;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(EditBox.class)
public interface AccessorTextFieldWidget
{
    /**
     * @return
     * @see EditBox#isBordered
     */
    @Accessor("bordered")
    boolean isDrawsBackground();
}
