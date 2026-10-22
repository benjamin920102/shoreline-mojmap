package net.shoreline.client.impl.gui.account;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shoreline.client.impl.gui.account.list.AccountEntry;
import net.shoreline.client.impl.gui.account.list.AccountListWidget;
import net.shoreline.client.impl.manager.client.AccountManager;
import net.shoreline.client.init.Managers;

/**
 * @author xgraza
 * @since 03/28/24
 * <p>
 * The code in this file may or not cause a seizure
 */
public final class AccountSelectorScreen extends Screen
{

    private final Screen parent;
    private AccountListWidget accountListWidget;
    private EditBox searchWidget;

    public AccountSelectorScreen(final Screen parent)
    {
        super(Component.nullToEmpty("Account Selector"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        accountListWidget = new AccountListWidget(minecraft, width, height - 64 - 32, 32, 25);
        clearWidgets();
        accountListWidget.setRectangle(width, height - 64 - 32, 0, 32);
        accountListWidget.populateEntries();
        accountListWidget.setSearchFilter(null);
        addRenderableWidget(searchWidget = new EditBox(minecraft.font, 135, 20, Component.nullToEmpty("Search...")));
        searchWidget.setPosition(width / 2 - searchWidget.getWidth() / 2, 4);
        searchWidget.setHint(Component.nullToEmpty("Search..."));
        final int buttonWidth = 110;
        final int buttonHeight = 20;
        // my head hurts
        addRenderableWidget(Button.builder(Component.nullToEmpty("Add"),
                        (action) -> minecraft.setScreen(new AccountAddAccountScreen(this)))
                .bounds(width / 2 + 2, accountListWidget.getHeight() + 40, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(Component.nullToEmpty("Login"), (action) ->
        {
            final AccountEntry entry = accountListWidget.getSelected();
            if (entry != null)
            {
                final User session = entry.getAccount().login();
                if (session != null)
                {
                    Managers.ACCOUNT.setSession(session);
                }
            }
        }).bounds(width / 2 - buttonWidth - 2, accountListWidget.getHeight() + 40, buttonWidth, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.nullToEmpty("Back"),
                        (action) -> minecraft.setScreen(parent))
                .bounds(width / 2 - buttonWidth - 2,
                        accountListWidget.getHeight() + 40 + buttonHeight + 2,
                        buttonWidth, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.nullToEmpty("Delete"), (action) ->
        {
            final AccountEntry entry = accountListWidget.getSelected();
            if (entry == null)
            {
                return;
            }
            // bypass if the user is holding down shift
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(),
                    InputConstants.KEY_LSHIFT))
            {
                Managers.ACCOUNT.unregister(entry.getAccount());
                minecraft.setScreen(this);
                return;
            }
            minecraft.setScreen(new ConfirmScreen((value) ->
            {
                if (value)
                {
                    Managers.ACCOUNT.unregister(entry.getAccount());
                }
                minecraft.setScreen(this);
            }, Component.nullToEmpty("Delete account?"),
                    Component.nullToEmpty("Are you sure you would like to delete " + entry.getAccount().username() + "?"),
                    Component.nullToEmpty("Yes"),
                    Component.nullToEmpty("No")));
        }).bounds(width / 2 + 2,
                accountListWidget.getHeight() + 40 + buttonHeight + 2,
                buttonWidth, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.nullToEmpty(Managers.ACCOUNT.isEncrypted() ? "Decrypt" : "Encrypt"), (action) ->
        {
            if (Managers.ACCOUNT.isEncrypted())
            {
            }
            else
            {
                minecraft.setScreen(new AccountEncryptionScreen(this));
            }
        }).bounds(width - buttonWidth - 4, 6, buttonWidth, buttonHeight).build());
    }

    @Override
    public void added()
    {
        if (accountListWidget != null)
        {
            accountListWidget.populateEntries();
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        super.render(context, mouseX, mouseY, delta);
        accountListWidget.render(context, mouseX, mouseY, delta);
        context.drawString(minecraft.font, Component.nullToEmpty(getLoginInfo()), 2, 2, 0xAAAAAA);
        if (searchWidget.isHoveredOrFocused())
        {
            String content = searchWidget.getValue();
            if (content == null || content.isEmpty())
            {
                accountListWidget.setSearchFilter(null);
                return;
            }
            accountListWidget.setSearchFilter(content.replaceAll("\\s*", ""));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        accountListWidget.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        accountListWidget.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private String getLoginInfo()
    {
        return AccountManager.MSA_AUTHENTICATOR.getLoginStage().isEmpty() ? "Logged in as " + minecraft.getUser().getName() : AccountManager.MSA_AUTHENTICATOR.getLoginStage();
    }
}
