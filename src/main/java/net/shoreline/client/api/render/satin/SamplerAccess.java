package net.shoreline.client.api.render.satin;

import java.util.List;

public interface SamplerAccess {
   boolean hasSampler(String var1);

   List<String> getSamplerNames();

   List<Integer> getSamplerShaderLocs();
}
