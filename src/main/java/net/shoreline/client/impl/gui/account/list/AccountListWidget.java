package net.shoreline.client.impl.gui.account.list;

import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.shoreline.client.api.account.type.MinecraftAccount;
import net.shoreline.client.init.Managers;

public final class AccountListWidget extends ObjectSelectionList<AccountEntry> {
   private String searchFilter;

   public AccountListWidget(Minecraft minecraftClient, int i, int j, int k, int l) {
      super(minecraftClient, i, j, k, l);
   }

   public void populateEntries() {
      this.clearEntries();
      List<MinecraftAccount> accounts = Managers.ACCOUNT.getAccounts();
      if (!accounts.isEmpty()) {
         for (MinecraftAccount account : accounts) {
            this.addEntry(new AccountEntry(account));
         }

         this.setSelected(this.getEntry(0));
      }
   }

   @Override
   protected void renderListItems(GuiGraphics context, int mouseX, int mouseY, float delta) {
      List<AccountEntry> entries = this.children();
      if (this.searchFilter != null && !this.searchFilter.isEmpty()) {
         entries = entries.stream().filter(entryx -> entryx.getAccount().username().toLowerCase().contains(this.searchFilter.toLowerCase())).toList();
      }

      int x = this.getRowLeft();
      int width = this.getRowWidth();
      int height = this.itemHeight - 4;
      int size = entries.size();

      for (int i = 0; i < size; i++) {
         int y = this.getRowTop(i);
         int m = this.getRowBottom(i);
         if (m >= this.getY() && y <= this.getBottom()) {
            AccountEntry entry = entries.get(i);
            boolean isHovered = Objects.equals(this.getHovered(), entry);
            entry.renderBack(context, i, y, x, width, height, mouseX, mouseY, isHovered, delta);
            if (Objects.equals(this.getSelected(), entry)) {
               int color = this.isFocused() ? -1 : -8355712;
               this.renderSelection(context, y, width, height, color, -16777216);
            }

            boolean selected = this.minecraft.getUser() != null && this.minecraft.getUser().getName().equalsIgnoreCase(entry.getAccount().username());
            entry.render(context, i, y, x, width, height, mouseX, mouseY, selected, delta);
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.updateScrollingState(mouseX, mouseY, button);
      AccountEntry entry = this.getEntryAtPosition(mouseX, mouseY);
      if (entry != null) {
         this.setSelected(entry);
      }

      return this.getSelected() != null ? this.getSelected().mouseClicked(mouseX, mouseY, button) : true;
   }

   public void setSearchFilter(String searchFilter) {
      this.searchFilter = searchFilter;
   }
}
