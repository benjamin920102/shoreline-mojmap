package net.shoreline.client.impl.gui.account;

import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.account.msa.exception.MSAAuthException;
import net.shoreline.client.api.account.type.MinecraftAccount;
import net.shoreline.client.api.account.type.impl.CrackedAccount;
import net.shoreline.client.api.account.type.impl.MicrosoftAccount;
import net.shoreline.client.impl.manager.client.AccountManager;
import net.shoreline.client.init.Managers;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

/**
 * @author xgraza
 * @since 03/28/24
 */
public final class AccountAddAccountScreen extends Screen
{
    private final Screen parent;
    private EditBox email, password;

    public AccountAddAccountScreen(final Screen parent)
    {
        super(Component.nullToEmpty("Add or Create an Alt Account"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        clearWidgets();
        addRenderableWidget(email = new EditBox(minecraft.font, width / 2 - 75, height / 2 - 30, 150, 20, Component.nullToEmpty("")));
        email.setHint(Component.nullToEmpty("Email or Username..."));
        addRenderableWidget(password = new EditBox(minecraft.font, width / 2 - 75, height / 2 - 5, 150, 20, Component.nullToEmpty("")));
        password.setHint(Component.nullToEmpty("Password (Optional)"));

        addRenderableWidget(Button.builder(Component.nullToEmpty("Add"), (action) ->
        {
            final String accountEmail = email.getValue();
            if (accountEmail.length() >= 3)
            {
                final String accountPassword = password.getValue();
                MinecraftAccount account;
                if (!accountPassword.isEmpty())
                {
                    account = new MicrosoftAccount(accountEmail, accountPassword);
                }
                else
                {
                    account = new CrackedAccount(accountEmail);
                }

                // Add account to alt manager & display the parent screen
                Managers.ACCOUNT.register(account);
                minecraft.setScreen(parent);
            }
        }).bounds(width / 2 - 72, height / 2 + 20, 145, 20).build());
        addRenderableWidget(Button.builder(Component.nullToEmpty("Browser..."), (action) ->
        {
            try
            {
                AccountManager.MSA_AUTHENTICATOR.loginWithBrowser((token) ->
                        Shoreline.EXECUTOR.execute(() ->
                        {
                            final MicrosoftAccount account = new MicrosoftAccount(token);
                            final User session = account.login();
                            if (session != null)
                            {
                                Managers.ACCOUNT.setSession(session);
                                Managers.ACCOUNT.register(account);
                                minecraft.setScreen(parent);
                            }
                            else
                            {
                                AccountManager.MSA_AUTHENTICATOR.setLoginStage("Could not login to account");
                            }
                        }));
            }
            catch (IOException | URISyntaxException | MSAAuthException e)
            {
                e.printStackTrace();
            }
        }).bounds(width / 2 - 72, height / 2 + 20 + 22, 145, 20).build());
        addRenderableWidget(Button.builder(Component.nullToEmpty("Go Back"), (action) -> minecraft.setScreen(parent))
                .bounds(width / 2 - 72, height / 2 + 20 + (22 * 2), 145, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        super.render(context, mouseX, mouseY, delta);
        context.drawString(minecraft.font, "*",
                email.getX() - 10,
                email.getY() + (email.getHeight() / 2) - (minecraft.font.lineHeight / 2),
                (email.getValue().length() >= 3 ? Color.green : Color.red).getRGB());
        context.drawCenteredString(minecraft.font,
                "Add an Account", width / 2, height / 2 - 120, -1);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW_KEY_ESCAPE)
        {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
