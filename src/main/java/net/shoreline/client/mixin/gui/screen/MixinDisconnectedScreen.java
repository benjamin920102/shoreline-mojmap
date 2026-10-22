package net.shoreline.client.mixin.gui.screen;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.shoreline.client.util.Globals;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({DisconnectedScreen.class})
public abstract class MixinDisconnectedScreen extends MixinScreen implements Globals {
}
