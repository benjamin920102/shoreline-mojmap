package net.shoreline.client.api.render.satin;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.shoreline.client.api.render.satin.uniform.Uniform1f;
import net.shoreline.client.api.render.satin.uniform.Uniform1i;
import net.shoreline.client.api.render.satin.uniform.Uniform2f;
import net.shoreline.client.api.render.satin.uniform.Uniform2i;
import net.shoreline.client.api.render.satin.uniform.Uniform3f;
import net.shoreline.client.api.render.satin.uniform.Uniform3i;
import net.shoreline.client.api.render.satin.uniform.Uniform4f;
import net.shoreline.client.api.render.satin.uniform.Uniform4i;
import net.shoreline.client.api.render.satin.uniform.UniformFinder;
import net.shoreline.client.api.render.satin.uniform.UniformMat4;

public abstract class ResettableManagedShaderBase<S extends AutoCloseable> implements UniformFinder {
   private final ResourceLocation location;
   private final Map<String, ManagedUniform> managedUniforms = new HashMap<>();
   private final List<ManagedUniformBase> allUniforms = new ArrayList<>();
   private boolean errored;
   protected S shader;

   public ResettableManagedShaderBase(ResourceLocation location) {
      this.location = location;
   }

   public void initializeOrLog(ResourceProvider mgr) {
      try {
         this.initialize(mgr);
      } catch (IOException var3) {
         this.errored = true;
         this.logInitError(var3);
      }
   }

   protected abstract void logInitError(IOException var1);

   protected void initialize(ResourceProvider resourceManager) throws IOException {
      this.release();
      Minecraft mc = Minecraft.getInstance();
      this.shader = this.parseShader(resourceManager, mc, this.location);
      this.setup(mc.getWindow().getWidth(), mc.getWindow().getHeight());
   }

   protected abstract S parseShader(ResourceProvider var1, Minecraft var2, ResourceLocation var3) throws IOException;

   public void release() {
      if (this.isInitialized()) {
         try {
            assert this.shader != null;

            this.shader.close();
            this.shader = null;
         } catch (Exception var2) {
            throw new RuntimeException("Failed to release shader " + this.location, var2);
         }
      }

      this.errored = false;
   }

   protected Collection<ManagedUniformBase> getManagedUniforms() {
      return this.allUniforms;
   }

   protected abstract boolean setupUniform(ManagedUniformBase var1, S var2);

   public boolean isInitialized() {
      return this.shader != null;
   }

   public boolean isErrored() {
      return this.errored;
   }

   public ResourceLocation getLocation() {
      return this.location;
   }

   protected <U extends ManagedUniformBase> U manageUniform(Map<String, U> uniformMap, Function<String, U> factory, String uniformName, String uniformKind) {
      U existing = (U)uniformMap.get(uniformName);
      if (existing != null) {
         return existing;
      } else {
         U ret = (U)factory.apply(uniformName);
         if (this.shader != null) {
            boolean found = this.setupUniform(ret, this.shader);
            if (!found) {
               LogUtils.getLogger().warn("No {} found with name {} in shader {}", new Object[]{uniformKind, uniformName, this.location});
            }
         }

         uniformMap.put(uniformName, ret);
         this.allUniforms.add(ret);
         return ret;
      }
   }

   @Override
   public Uniform1i findUniform1i(String uniformName) {
      return this.manageUniform(this.managedUniforms, name -> new ManagedUniform(name, 1), uniformName, "uniform");
   }

   @Override
   public Uniform2i findUniform2i(String uniformName) {
      return this.manageUniform(this.managedUniforms, name -> new ManagedUniform(name, 2), uniformName, "uniform");
   }

   @Override
   public Uniform3i findUniform3i(String uniformName) {
      return this.manageUniform(this.managedUniforms, name -> new ManagedUniform(name, 3), uniformName, "uniform");
   }

   @Override
   public Uniform4i findUniform4i(String uniformName) {
      return this.manageUniform(this.managedUniforms, name -> new ManagedUniform(name, 4), uniformName, "uniform");
   }

   @Override
   public Uniform1f findUniform1f(String uniformName) {
      return this.manageUniform(this.managedUniforms, name -> new ManagedUniform(name, 1), uniformName, "uniform");
   }

   @Override
   public Uniform2f findUniform2f(String uniformName) {
      return this.manageUniform(this.managedUniforms, name -> new ManagedUniform(name, 2), uniformName, "uniform");
   }

   @Override
   public Uniform3f findUniform3f(String uniformName) {
      return this.manageUniform(this.managedUniforms, name -> new ManagedUniform(name, 3), uniformName, "uniform");
   }

   @Override
   public Uniform4f findUniform4f(String uniformName) {
      return this.manageUniform(this.managedUniforms, name -> new ManagedUniform(name, 4), uniformName, "uniform");
   }

   @Override
   public UniformMat4 findUniformMat4(String uniformName) {
      return this.manageUniform(this.managedUniforms, name -> new ManagedUniform(name, 16), uniformName, "uniform");
   }

   public abstract void setup(int var1, int var2);

   @Override
   public String toString() {
      return "%s[%s]".formatted(this.getClass().getSimpleName(), this.location);
   }
}
