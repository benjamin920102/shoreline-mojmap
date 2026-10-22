package net.shoreline.client.impl.module.misc;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.config.setting.StringConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.network.GameJoinEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;

public class AntiAFKModule extends RotationModule {
   Config<Float> secondsConfig = this.register(new NumberConfig<>("AFKTime", "The time before attempting actions", 5.0F, 20.0F, 120.0F));
   Config<Boolean> messageConfig = this.register(new BooleanConfig("Message", "Messages in chat to prevent AFK kick", true));
   Config<Boolean> tabCompleteConfig = this.register(new BooleanConfig("TabComplete", "Uses tab complete in chat to prevent AFK kick", true));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates the player to prevent AFK kick", true));
   Config<Boolean> jumpConfig = this.register(new BooleanConfig("Jump", "Jumps to prevent AFK kick", true));
   Config<Boolean> autoReplyConfig = this.register(new BooleanConfig("AutoReply", "Replies to players messaging you in chat", false));
   Config<String> replyConfig = this.register(
      new StringConfig("Reply", "The reply message for AutoReply", "I am currently AFK.", () -> this.autoReplyConfig.getValue())
   );
   Config<Float> delayConfig = this.register(new NumberConfig<>("Delay", "The delay between actions", 5.0F, 100.0F, 1000.0F));
   private final Timer afkTimer = new CacheTimer();
   private final Timer actionTimer = new CacheTimer();

   public AntiAFKModule() {
      super("AntiAFK", "Prevents the player from being kicked for AFK", ModuleCategory.MISCELLANEOUS);
   }

   @Override
   public void onEnable() {
      this.afkTimer.reset();
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      this.afkTimer.reset();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      for (KeyMapping keyBinding : mc.options.keyMappings) {
         if (keyBinding.isDown()) {
            this.afkTimer.reset();
            break;
         }
      }

      if (this.afkTimer.passed(this.secondsConfig.getValue() * 1000.0F) && this.actionTimer.passed(this.delayConfig.getValue())) {
         if (this.jumpConfig.getValue()) {
            mc.player.jumpFromGround();
         }

         if (this.rotateConfig.getValue()) {
            this.setRotationClient(
               mc.player.getYRot() + RANDOM.nextFloat(90.0F) * (RANDOM.nextBoolean() ? 1.0F : -1.0F),
               mc.player.getXRot() + RANDOM.nextFloat(90.0F) * (RANDOM.nextBoolean() ? 1.0F : -1.0F)
            );
         }

         this.actionTimer.reset();
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof ClientboundPlayerChatPacket packet && this.autoReplyConfig.getValue()) {
         String[] words = packet.body().content().split(" ");
         if (words[1].startsWith("whispers:")) {
            ChatUtil.serverSendCommand("r [Shoreline] " + this.replyConfig.getValue());
         }
      }
   }
}
