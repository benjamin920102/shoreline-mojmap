package net.shoreline.client.impl.gui.account.list;

import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.api.account.type.MinecraftAccount;
import net.shoreline.client.api.account.type.impl.CrackedAccount;
import net.shoreline.client.api.account.type.impl.MicrosoftAccount;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.network.TextureDownloader;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

/**
 * @author xgraza
 * @since 03/28/24
 */
public class AccountEntry extends ObjectSelectionList.Entry<AccountEntry> implements Globals
{
    private static final TextureDownloader FACE_DOWNLOADER = new TextureDownloader();

    private final MinecraftAccount account;
    private long lastClickTime = -1;

    public AccountEntry(MinecraftAccount account)
    {
        this.account = account;
    }

    @Override
    public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
    {
        context.drawString(mc.font,
                Component.nullToEmpty(account.username()),
                x + 20, y + (entryHeight / 2) - (mc.font.lineHeight / 2), hovered ? 0x55ff55 : -1);
        if (account instanceof CrackedAccount || (account instanceof MicrosoftAccount msa && msa.getUsernameOrNull() != null))
        {
            final String id = "face_" + account.username().toLowerCase();
            if (!FACE_DOWNLOADER.exists(id))
            {
                if (!FACE_DOWNLOADER.isDownloading(id))
                {
                    FACE_DOWNLOADER.downloadTexture(id,
                            "https://minotar.net/helm/" + account.username() + "/15", false);
                }
                return;
            }
            final ResourceLocation texture = FACE_DOWNLOADER.get(id);
            if (texture != null)
            {
                context.blit(texture, x + 2, y + 2, 0, 0, 15, 15, 15, 15);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == GLFW_MOUSE_BUTTON_1)
        {
            // fuck this game
            final long time = System.currentTimeMillis() - lastClickTime;
            if (time > 0L && time < 500L)
            {
                final User session = account.login();
                if (session != null)
                {
                    Managers.ACCOUNT.setSession(session);
                }
            }
            lastClickTime = System.currentTimeMillis();
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public Component getNarration()
    {
        if (account instanceof MicrosoftAccount msa && msa.username() == null)
        {
            return null;
        }
        return Component.nullToEmpty(account.username());
    }

    public MinecraftAccount getAccount()
    {
        return account;
    }
}
