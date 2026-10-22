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

public final class AccountSelectorScreen extends Screen {
   private final Screen parent;
   private AccountListWidget accountListWidget;
   private EditBox searchWidget;

   public AccountSelectorScreen(Screen parent) {
      super(Component.nullToEmpty("Account Selector"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.accountListWidget = new AccountListWidget(this.minecraft, this.width, this.height - 64 - 32, 32, 25);
      this.clearWidgets();
      this.accountListWidget.setRectangle(this.width, this.height - 64 - 32, 0, 32);
      this.accountListWidget.populateEntries();
      this.accountListWidget.setSearchFilter(null);
      this.addRenderableWidget(this.searchWidget = new EditBox(this.minecraft.font, 135, 20, Component.nullToEmpty("Search...")));
      this.searchWidget.setPosition(this.width / 2 - this.searchWidget.getWidth() / 2, 4);
      this.searchWidget.setHint(Component.nullToEmpty("Search..."));
      int buttonWidth = 110;
      int buttonHeight = 20;
      this.addRenderableWidget(
         Button.builder(Component.nullToEmpty("Add"), action -> this.minecraft.setScreen(new AccountAddAccountScreen(this)))
            .bounds(this.width / 2 + 2, this.accountListWidget.getHeight() + 40, 110, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(Component.nullToEmpty("Login"), action -> {
         AccountEntry entry = this.accountListWidget.getSelected();
         if (entry != null) {
            User session = entry.getAccount().login();
            if (session != null) {
               Managers.ACCOUNT.setSession(session);
            }
         }
      }).bounds(this.width / 2 - 110 - 2, this.accountListWidget.getHeight() + 40, 110, 20).build());
      this.addRenderableWidget(
         Button.builder(Component.nullToEmpty("Back"), action -> this.minecraft.setScreen(this.parent))
            .bounds(this.width / 2 - 110 - 2, this.accountListWidget.getHeight() + 40 + 20 + 2, 110, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(
               Component.nullToEmpty("Delete"),
               action -> {
                  AccountEntry entry = this.accountListWidget.getSelected();
                  if (entry != null) {
                     if (InputConstants.isKeyDown(this.minecraft.getWindow().getWindow(), 340)) {
                        Managers.ACCOUNT.unregister(entry.getAccount());
                        this.minecraft.setScreen(this);
                     } else {
                        this.minecraft
                           .setScreen(
                              new ConfirmScreen(
                                 value -> {
                                    if (value) {
                                       Managers.ACCOUNT.unregister(entry.getAccount());
                                    }

                                    this.minecraft.setScreen(this);
                                 },
                                 Component.nullToEmpty("Delete account?"),
                                 Component.nullToEmpty("Are you sure you would like to delete " + entry.getAccount().username() + "?"),
                                 Component.nullToEmpty("Yes"),
                                 Component.nullToEmpty("No")
                              )
                           );
                     }
                  }
               }
            )
            .bounds(this.width / 2 + 2, this.accountListWidget.getHeight() + 40 + 20 + 2, 110, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(Component.nullToEmpty(Managers.ACCOUNT.isEncrypted() ? "Decrypt" : "Encrypt"), action -> {
         if (!Managers.ACCOUNT.isEncrypted()) {
            this.minecraft.setScreen(new AccountEncryptionScreen(this));
         }
      }).bounds(this.width - 110 - 4, 6, 110, 20).build());
   }

   @Override
   public void added() {
      if (this.accountListWidget != null) {
         this.accountListWidget.populateEntries();
      }
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.accountListWidget.render(context, mouseX, mouseY, delta);
      context.drawString(this.minecraft.font, Component.nullToEmpty(this.getLoginInfo()), 2, 2, 11184810);
      if (this.searchWidget.isHoveredOrFocused()) {
         String content = this.searchWidget.getValue();
         if (content == null || content.isEmpty()) {
            this.accountListWidget.setSearchFilter(null);
            return;
         }

         this.accountListWidget.setSearchFilter(content.replaceAll("\\s*", ""));
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.accountListWidget.mouseClicked(mouseX, mouseY, button);
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.accountListWidget.mouseReleased(mouseX, mouseY, button);
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private String getLoginInfo() {
      return AccountManager.MSA_AUTHENTICATOR.getLoginStage().isEmpty()
         ? "Logged in as " + this.minecraft.getUser().getName()
         : AccountManager.MSA_AUTHENTICATOR.getLoginStage();
   }
}
