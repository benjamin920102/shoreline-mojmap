package net.shoreline.client.mixin.render.block;

import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At.Shift;

@Mixin(
   value = {LiquidBlockRenderer.class},
   priority = Integer.MAX_VALUE
)
public class MixinFluidRenderer {
   @ModifyVariable(
      method = {"tesselate(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V"},
      at = @At(
         value = "CONSTANT",
         args = {"intValue=16"},
         ordinal = 0,
         shift = Shift.BEFORE
      ),
      ordinal = 0
   )
   private int hookI(int i) {
      return i;
   }
}
