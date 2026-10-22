package net.shoreline.client.init;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.Shoreline;
import net.shoreline.client.impl.font.AWTFontRenderer;
import net.shoreline.client.impl.font.VanillaTextRenderer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Fonts
{
    public static final VanillaTextRenderer VANILLA = new VanillaTextRenderer();

    public static final String DEFAULT_FONT_FILE_PATH = "assets/shoreline/font/verdana.ttf";
    public static String FONT_FILE_PATH = "assets/shoreline/font/verdana.ttf";

    public static AWTFontRenderer CLIENT;
    public static AWTFontRenderer CLIENT_UNSCALED;
    public static float FONT_SIZE = 9.0f;

    private static boolean initialized;

    public static void init()
    {
        if (initialized)
        {
            return;
        }
        Shoreline.CONFIG.loadFonts();
        Fonts.loadFonts();
        Shoreline.info("Loaded fonts!");
        initialized = true;
    }

    public static void loadFonts()
    {
        try
        {
            ResourceLocation identifier = ResourceLocation.fromNamespaceAndPath("shoreline", "font/verdana.ttf");
            InputStream stream = Minecraft.getInstance().getResourceManager().getResource(identifier).get().open();
            CLIENT = new AWTFontRenderer(FONT_FILE_PATH.startsWith("assets") ? stream : new FileInputStream(FONT_FILE_PATH), FONT_SIZE);
            CLIENT_UNSCALED = new AWTFontRenderer(FONT_FILE_PATH.startsWith("assets") ? stream : new FileInputStream(FONT_FILE_PATH), 9.0f);
        }
        catch (IOException e)
        {
            // mhm
        }
    }

    public static void closeFonts()
    {
        CLIENT.close();
        CLIENT_UNSCALED.close();
    }

    public static void setSize(float size)
    {
        FONT_SIZE = size;
        try
        {
            CLIENT = new AWTFontRenderer(new FileInputStream(FONT_FILE_PATH), FONT_SIZE);
        }
        catch (IOException e)
        {
            // mhm
        }
    }

    public static boolean isInitialized()
    {
        return initialized;
    }
}