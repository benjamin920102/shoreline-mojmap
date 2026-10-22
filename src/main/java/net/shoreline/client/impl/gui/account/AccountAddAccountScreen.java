package net.shoreline.client.impl.gui.account;

import java.awt.Color;
import java.io.IOException;
import java.net.URISyntaxException;
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

public final class AccountAddAccountScreen extends Screen {
   private final Screen parent;
   private EditBox email;
   private EditBox password;

   public AccountAddAccountScreen(Screen parent) {
      super(Component.nullToEmpty("Add or Create an Alt Account"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.clearWidgets();
      this.addRenderableWidget(this.email = new EditBox(this.minecraft.font, this.width / 2 - 75, this.height / 2 - 30, 150, 20, Component.nullToEmpty("")));
      this.email.setHint(Component.nullToEmpty("Email or Username..."));
      this.addRenderableWidget(this.password = new EditBox(this.minecraft.font, this.width / 2 - 75, this.height / 2 - 5, 150, 20, Component.nullToEmpty("")));
      this.password.setHint(Component.nullToEmpty("Password (Optional)"));
      this.addRenderableWidget(Button.builder(Component.nullToEmpty("Add"), action -> {
         String accountEmail = this.email.getValue();
         if (accountEmail.length() >= 3) {
            String accountPassword = this.password.getValue();
            MinecraftAccount account;
            if (!accountPassword.isEmpty()) {
               account = new MicrosoftAccount(accountEmail, accountPassword);
            } else {
               account = new CrackedAccount(accountEmail);
            }

            Managers.ACCOUNT.register(account);
            this.minecraft.setScreen(this.parent);
         }
      }).bounds(this.width / 2 - 72, this.height / 2 + 20, 145, 20).build());
      this.addRenderableWidget(Button.builder(Component.nullToEmpty("Browser..."), action -> {
         try {
            AccountManager.MSA_AUTHENTICATOR.loginWithBrowser(token -> Shoreline.EXECUTOR.execute(() -> {
               MicrosoftAccount account = new MicrosoftAccount(token);
               User session = account.login();
               if (session != null) {
                  Managers.ACCOUNT.setSession(session);
                  Managers.ACCOUNT.register(account);
                  this.minecraft.setScreen(this.parent);
               } else {
                  AccountManager.MSA_AUTHENTICATOR.setLoginStage("Could not login to account");
               }
            }));
         } catch (URISyntaxException | MSAAuthException | IOException var3) {
            var3.printStackTrace();
         }
      }).bounds(this.width / 2 - 72, this.height / 2 + 20 + 22, 145, 20).build());
      this.addRenderableWidget(
         Button.builder(Component.nullToEmpty("Go Back"), action -> this.minecraft.setScreen(this.parent))
            .bounds(this.width / 2 - 72, this.height / 2 + 20 + 44, 145, 20)
            .build()
      );
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      context.drawString(
         this.minecraft.font,
         "*",
         this.email.getX() - 10,
         this.email.getY() + this.email.getHeight() / 2 - 9 / 2,
         (this.email.getValue().length() >= 3 ? Color.green : Color.red).getRGB()
      );
      context.drawCenteredString(this.minecraft.font, "Add an Account", this.width / 2, this.height / 2 - 120, -1);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.minecraft.setScreen(this.parent);
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }
}
