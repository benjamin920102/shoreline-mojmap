package net.shoreline.client.impl.gui.account;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shoreline.client.init.Managers;

/**
 * @author xgraza
 * @since 03/31/24
 */
public final class AccountEncryptionScreen extends Screen
{
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()_+-={}[]\\|'\";:/?.>,<`";
    private static final String[] REQUIREMENTS = {"8+ Characters", "A Special Character", "A Number", "An Uppercase Letter"};

    private final Screen parent;
    private EditBox passwordTextField;

    public AccountEncryptionScreen(final Screen parent)
    {
        super(Component.nullToEmpty("Account Encryption"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        clearWidgets();

        passwordTextField = new EditBox(minecraft.font, 145, 20, Component.empty());
        passwordTextField.setHint(Component.nullToEmpty("Enter Password..."));
        passwordTextField.setPosition(width / 2 - passwordTextField.getWidth() / 2, height / 2 - 60);
        addRenderableWidget(passwordTextField);

        addRenderableWidget(Button.builder(Component.nullToEmpty("Encrypt"), (action) ->
                {
                    // TODO
                }).bounds(width / 2 - 145 / 2, passwordTextField.getY() + 90, 145, 20)
                .tooltip(Tooltip.create(Component.nullToEmpty("This will require you to enter a password every time you enter the account manager the first time!")))
                .build());
        addRenderableWidget(Button.builder(Component.nullToEmpty("Go Back"), (action) -> minecraft.setScreen(parent))
                .bounds(width / 2 - 145 / 2, passwordTextField.getY() + 112, 145, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        // wow this code is god awful
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredString(minecraft.font,
                "Encrypt Accounts (" + Managers.ACCOUNT.getAccounts().size() + ")", width / 2, height / 2 - 125, -1);
        context.drawString(minecraft.font,
                (isPasswordSecure(passwordTextField.getValue()) ? ChatFormatting.GREEN : ChatFormatting.RED) + "*",
                width / 2 - passwordTextField.getWidth() / 2 - 6,
                passwordTextField.getY() + 10 - (minecraft.font.lineHeight / 2), -1);

        {
            context.drawString(minecraft.font, "Minimum Requirements:",
                    passwordTextField.getX() + 1,
                    passwordTextField.getY() + passwordTextField.getHeight() + 10,
                    -1);
            for (int i = 0; i < REQUIREMENTS.length; ++i)
            {
                final String requirement = REQUIREMENTS[i];
                context.drawString(minecraft.font, "- " + requirement,
                        passwordTextField.getX() + 6,
                        passwordTextField.getY() + passwordTextField.getHeight() + 10 + ((font.lineHeight + 2) * (1 + i)),
                        -1);
            }
        }
    }

    /**
     * Checks if the password is "secure"
     *
     * @return if the password is greater than 8 characters, contains a special character, a number, and an uppercase letter
     */
    private boolean isPasswordSecure(final String password)
    {
        if (password.length() < 8)
        {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        final char[] characters = password.toCharArray();
        for (final char c : characters)
        {
            if (Character.isUpperCase(c))
            {
                hasUppercase = true;
            }
            if (SPECIAL_CHARACTERS.indexOf(c) != -1)
            {
                hasSpecial = true;
            }
            if (c >= 48 && c <= 57)
            {
                hasNumber = true;
            }
        }
        return hasUppercase && hasNumber && hasSpecial;
    }
}
