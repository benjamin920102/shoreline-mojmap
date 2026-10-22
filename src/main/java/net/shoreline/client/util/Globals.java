package net.shoreline.client.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;

/**
 * TERRIBLE CODING PRACTICE
 *
 * @author linus
 * @since 1.0
 */
public interface Globals
{
    // Minecraft game instance
    Minecraft mc = Minecraft.getInstance();
    //
    Random RANDOM = ThreadLocalRandom.current();
}
