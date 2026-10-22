package net.shoreline.client.impl.gui.account;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shoreline.client.init.Managers;

public final class AccountEncryptionScreen extends Screen {
   private static final String SPECIAL_CHARACTERS = "!@#$%^&*()_+-={}[]\\|'\";:/?.>,<`";
   private static final String[] REQUIREMENTS = new String[]{"8+ Characters", "A Special Character", "A Number", "An Uppercase Letter"};
   private final Screen parent;
   private EditBox passwordTextField;

   public AccountEncryptionScreen(Screen parent) {
      super(Component.nullToEmpty("Account Encryption"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.clearWidgets();
      this.passwordTextField = new EditBox(this.minecraft.font, 145, 20, Component.empty());
      this.passwordTextField.setHint(Component.nullToEmpty("Enter Password..."));
      this.passwordTextField.setPosition(this.width / 2 - this.passwordTextField.getWidth() / 2, this.height / 2 - 60);
      this.addRenderableWidget(this.passwordTextField);
      this.addRenderableWidget(
         Button.builder(Component.nullToEmpty("Encrypt"), action -> {})
            .bounds(this.width / 2 - 72, this.passwordTextField.getY() + 90, 145, 20)
            .tooltip(
               Tooltip.create(Component.nullToEmpty("This will require you to enter a password every time you enter the account manager the first time!"))
            )
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.nullToEmpty("Go Back"), action -> this.minecraft.setScreen(this.parent))
            .bounds(this.width / 2 - 72, this.passwordTextField.getY() + 112, 145, 20)
            .build()
      );
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredString(
         this.minecraft.font, "Encrypt Accounts (" + Managers.ACCOUNT.getAccounts().size() + ")", this.width / 2, this.height / 2 - 125, -1
      );
      context.drawString(
         this.minecraft.font,
         (this.isPasswordSecure(this.passwordTextField.getValue()) ? ChatFormatting.GREEN : ChatFormatting.RED) + "*",
         this.width / 2 - this.passwordTextField.getWidth() / 2 - 6,
         this.passwordTextField.getY() + 10 - 9 / 2,
         -1
      );
      context.drawString(
         this.minecraft.font,
         "Minimum Requirements:",
         this.passwordTextField.getX() + 1,
         this.passwordTextField.getY() + this.passwordTextField.getHeight() + 10,
         -1
      );

      for (int i = 0; i < REQUIREMENTS.length; i++) {
         String requirement = REQUIREMENTS[i];
         context.drawString(
            this.minecraft.font,
            "- " + requirement,
            this.passwordTextField.getX() + 6,
            this.passwordTextField.getY() + this.passwordTextField.getHeight() + 10 + (9 + 2) * (1 + i),
            -1
         );
      }
   }

   private boolean isPasswordSecure(String password) {
      if (password.length() < 8) {
         return false;
      } else {
         boolean hasUppercase = false;
         boolean hasNumber = false;
         boolean hasSpecial = false;
         char[] characters = password.toCharArray();

         for (char c : characters) {
            if (Character.isUpperCase(c)) {
               hasUppercase = true;
            }

            if ("!@#$%^&*()_+-={}[]\\|'\";:/?.>,<`".indexOf(c) != -1) {
               hasSpecial = true;
            }

            if (c >= '0' && c <= '9') {
               hasNumber = true;
            }
         }

         return hasUppercase && hasNumber && hasSpecial;
      }
   }
}
