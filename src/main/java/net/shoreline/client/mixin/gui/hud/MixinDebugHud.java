package net.shoreline.client.mixin.gui.hud;

import java.util.List;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({DebugScreenOverlay.class})
public abstract class MixinDebugHud {
   @Shadow
   protected abstract List<String> getGameInformation();

   @Redirect(
      method = {"drawGameInformation(Lnet/minecraft/client/gui/GuiGraphics;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;getGameInformation()Ljava/util/List;"
      )
   )
   private List<String> redirectRightTextEarly(DebugScreenOverlay instance) {
      List<String> list = this.getGameInformation();
      String modName = String.format(
         "%s %s (%s%s%s)", "Shoreline", "1.0", "beta", !"beta".equals("dev") ? "-25" : "", !"fa4cb0f".equals("null") ? "-fa4cb0f" : ""
      );
      list.add(1, modName);
      return list;
   }
}
