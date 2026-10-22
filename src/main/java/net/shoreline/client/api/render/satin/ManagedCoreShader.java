package net.shoreline.client.api.render.satin;

import net.minecraft.client.renderer.ShaderInstance;
import net.shoreline.client.api.render.satin.uniform.UniformFinder;

public interface ManagedCoreShader extends UniformFinder {
   ShaderInstance getProgram();

   void release();
}
