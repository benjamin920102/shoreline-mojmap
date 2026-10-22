package net.shoreline.client.mixin.gui.screen;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.shoreline.client.impl.gui.account.AccountSelectorScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TitleScreen.class})
public abstract class MixinTitleScreen extends Screen {
   @Shadow
   @Nullable
   private SplashRenderer splash;
   @Shadow
   @Final
   public static Component COPYRIGHT_TEXT;
   @Shadow
   @Nullable
   private RealmsNotificationsScreen realmsNotificationsScreen;
   @Shadow
   private long fadeInStart;
   @Shadow
   @Final
   private boolean fading;

   @Shadow
   protected abstract void createDemoMenuOptions(int var1, int var2);

   @Shadow
   protected abstract void createNormalMenuOptions(int var1, int var2);

   @Shadow
   protected abstract boolean realmsNotificationsEnabled();

   public MixinTitleScreen(Component title) {
      super(title);
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
      at = {@At("TAIL")}
   )
   public void hookRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo info) {
      float f = this.fading ? (float)(Util.getMillis() - this.fadeInStart) / 1000.0F : 1.0F;
      float g = this.fading ? Mth.clamp(f - 1.0F, 0.0F, 1.0F) : 1.0F;
      int i = Mth.ceil(g * 255.0F) << 24;
      if ((i & -67108864) != 0) {
         context.drawString(
            this.minecraft.font,
            String.format("%s %s (%s%s%s)", "Shoreline", "1.0", "beta", !"beta".equals("dev") ? "-25" : "", !"fa4cb0f".equals("null") ? "-fa4cb0f" : ""),
            2,
            this.height - 9 * 2 - 2,
            16777215 | i
         );
      }
   }

   @Inject(
      method = {"init()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookInit(CallbackInfo ci) {
      ci.cancel();
      if (this.splash == null) {
         this.splash = this.minecraft.getSplashManager().getSplash();
      }

      int i = this.font.width(COPYRIGHT_TEXT);
      int j = this.width - i - 2;
      int k = 24;
      int l = this.height / 4 + 48;
      if (this.minecraft.isDemo()) {
         this.createDemoMenuOptions(l, 24);
      } else {
         this.createNormalMenuOptions(l, 24);
      }

      SpriteIconButton textIconButtonWidget = this.addRenderableWidget(
         CommonButtons.language(
            20, button -> this.minecraft.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), true
         )
      );
      textIconButtonWidget.setPosition(this.width / 2 - 124, l + 72 + 24);
      this.addRenderableWidget(
         Button.builder(Component.translatable("menu.options"), button -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)))
            .bounds(this.width / 2 - 100, l + 72 + 24, 98, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("menu.quit"), button -> this.minecraft.stop()).bounds(this.width / 2 + 2, l + 72 + 24, 98, 20).build()
      );
      SpriteIconButton textIconButtonWidget2 = this.addRenderableWidget(
         CommonButtons.accessibility(20, button -> this.minecraft.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), true)
      );
      textIconButtonWidget2.setPosition(this.width / 2 + 104, l + 72 + 24);
      this.addRenderableWidget(
         new PlainTextButton(j, this.height - 10, i, 10, COPYRIGHT_TEXT, button -> this.minecraft.setScreen(new CreditsAndAttributionScreen(this)), this.font)
      );
      if (this.realmsNotificationsScreen == null) {
         this.realmsNotificationsScreen = new RealmsNotificationsScreen();
      }

      if (this.realmsNotificationsEnabled()) {
         this.realmsNotificationsScreen.init(this.minecraft, this.width, this.height);
      }
   }

   @Inject(
      method = {"createNormalMenuOptions(II)V"},
      at = {@At(
         target = "Lnet/minecraft/client/gui/screens/TitleScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
         value = "INVOKE",
         shift = Shift.AFTER,
         ordinal = 2
      )}
   )
   public void hookInit(int y, int spacingY, CallbackInfo ci) {
      Button widget = Button.builder(Component.nullToEmpty("Account Manager"), action -> this.minecraft.setScreen(new AccountSelectorScreen(this)))
         .bounds(this.width / 2 - 100, y + spacingY * 3, 200, 20)
         .tooltip(Tooltip.create(Component.nullToEmpty("Allows you to switch your in-game account")))
         .build();
      widget.active = true;
      this.addRenderableWidget(widget);
   }
}
