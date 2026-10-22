package net.shoreline.client.util.network;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.Shoreline;
import net.shoreline.client.util.Globals;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xgraza
 * @since 03/28/24
 */
public final class TextureDownloader implements Globals
{

    private final CloseableHttpClient client = HttpClients.createDefault();

    private final Map<String, ResourceLocation> cache = new ConcurrentHashMap<>();
    private final Set<String> downloading = new HashSet<>();

    public void downloadTexture(final String id, final String url, final boolean force)
    {
        if (!downloading.add(id) || cache.containsKey(id)) return;

        Shoreline.EXECUTOR.execute(() ->
        {
            final HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = client.execute(request))
            {
                final InputStream stream = response.getEntity().getContent();
                final NativeImage image = NativeImage.read(stream);
                final ResourceLocation textureIdentifier = mc.getTextureManager().register(id,
                        new DynamicTexture(image));

                cache.put(id, textureIdentifier);
            }
            catch (IOException e)
            {
                e.printStackTrace();

                if (force)
                {
                    downloading.remove(id);
                }
            }
        });
    }

    public void removeTexture(final String id)
    {
        final ResourceLocation identifier = cache.get(id);
        if (identifier != null)
        {
            mc.getTextureManager().release(identifier);
            cache.remove(id);
        }
    }

    public ResourceLocation get(final String id)
    {
        return cache.get(id);
    }

    public boolean exists(final String id)
    {
        return cache.containsKey(id);
    }

    public boolean isDownloading(final String id)
    {
        return downloading.contains(id);
    }
}
