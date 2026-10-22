package net.shoreline.client.impl.module.client;

import com.mojang.blaze3d.platform.Window;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.ConfigContainer;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.Module;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.ScreenOpenEvent;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.entity.StatusEffectEvent;
import net.shoreline.client.impl.event.gui.hud.RenderOverlayEvent;
import net.shoreline.client.impl.event.gui.screen.RenderOpenChatEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.module.exploit.FastLatencyModule;
import net.shoreline.client.impl.module.misc.TimerModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.StreamUtils;
import net.shoreline.client.util.math.PerSecondCounter;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.player.InventoryUtil;
import net.shoreline.client.util.render.ColorUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.render.animation.Easing;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.client.util.world.BlockUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class HUDModule extends ToggleModule {
   private static HUDModule INSTANCE;
   Config<Boolean> watermarkConfig = this.register(new BooleanConfig("Watermark", "Displays client name and version watermark", true));
   Config<Boolean> serverStatusConfig = this.register(new BooleanConfig("ServerStatus", "Displays warning for server connection status", true));
   Config<Boolean> userInfo = this.register(new BooleanConfig("UserInfo", "Displays your user information", true));
   Config<Boolean> directionConfig = this.register(new BooleanConfig("Direction", "Displays facing direction", true));
   Config<Boolean> rotationConfig = this.register(new BooleanConfig("Rotation", "Displays player yaw and pitch", false, () -> this.directionConfig.getValue()));
   Config<Boolean> armorConfig = this.register(new BooleanConfig("Armor", "Displays player equipped armor and durability", true));
   Config<Boolean> armorDurabilityConfig = this.register(
      new BooleanConfig("ArmorDurability", "Displays player equipped armor durability", false, () -> this.armorConfig.getValue())
   );
   Config<Boolean> totemsConfig = this.register(new BooleanConfig("Totems", "Displays player totems", false));
   Config<HUDModule.VanillaHud> potionHudConfig = this.register(
      new EnumConfig<>("PotionHud", "Renders the Minecraft potion Hud", HUDModule.VanillaHud.HIDE, HUDModule.VanillaHud.values())
   );
   Config<HUDModule.VanillaHud> itemNameConfig = this.register(
      new EnumConfig<>("ItemName", "Renders the Minecraft item name display", HUDModule.VanillaHud.HIDE, HUDModule.VanillaHud.values())
   );
   Config<Boolean> potionEffectsConfig = this.register(new BooleanConfig("PotionEffects", "Displays active potion effects", true));
   Config<HUDModule.PotionColors> potionColorsConfig = this.register(
      new EnumConfig<>(
         "PotionColors",
         "Displays active potion colors",
         HUDModule.PotionColors.NORMAL,
         HUDModule.PotionColors.values(),
         () -> this.potionEffectsConfig.getValue()
      )
   );
   Config<Boolean> durabilityConfig = this.register(new BooleanConfig("Durability", "Displays the current held items durability", false));
   Config<Boolean> coordsConfig = this.register(new BooleanConfig("Coords", "Displays world coordinates", true));
   Config<Boolean> netherCoordsConfig = this.register(
      new BooleanConfig("NetherCoords", "Displays nether coordinates", true, () -> this.coordsConfig.getValue())
   );
   Config<Boolean> serverBrandConfig = this.register(new BooleanConfig("ServerBrand", "Displays the current server brand", false));
   Config<Boolean> chestsConfig = this.register(new BooleanConfig("Chests", "Displays the amount of chests in your render distance", false));
   Config<HUDModule.SpeedHud> speedConfig = this.register(
      new EnumConfig<>("Speed", "Displays the current movement speed of the player", HUDModule.SpeedHud.K_M_H, HUDModule.SpeedHud.values())
   );
   Config<Boolean> pingConfig = this.register(new BooleanConfig("Ping", "Display server response time in ms", true));
   Config<Boolean> tpsConfig = this.register(new BooleanConfig("TPS", "Displays server ticks per second", true));
   Config<Boolean> packetsConfig = this.register(new BooleanConfig("Packets", "Displays outbound packets per second", false));
   Config<Boolean> fpsConfig = this.register(new BooleanConfig("FPS", "Displays game FPS", true));
   Config<Boolean> arraylistConfig = this.register(new BooleanConfig("Arraylist", "Displays a list of all active modules", true));
   Config<Integer> animTimeConfig = this.register(new NumberConfig<>("Arraylist-Time", "Timer for the animation", 0, 1000, 2000, () -> false));
   Config<HUDModule.Ordering> orderingConfig = this.register(
      new EnumConfig<>(
         "Ordering", "The ordering of the arraylist", HUDModule.Ordering.LENGTH, HUDModule.Ordering.values(), () -> this.arraylistConfig.getValue()
      )
   );
   Config<HUDModule.Rendering> renderingConfig = this.register(
      new EnumConfig<>("Rendering", "The rendering mode of the HUD", HUDModule.Rendering.UP, HUDModule.Rendering.values())
   );
   Config<HUDModule.RainbowMode> rainbowModeConfig = this.register(
      new EnumConfig<>("Rainbow", "The rendering mode for rainbow", HUDModule.RainbowMode.OFF, HUDModule.RainbowMode.values())
   );
   Config<Color> gradientColorConfig = this.register(
      new ColorConfig(
         "GradientColor",
         "The color of the rainbow gradient",
         Color.WHITE,
         false,
         false,
         () -> this.rainbowModeConfig.getValue() == HUDModule.RainbowMode.GRADIENT
      )
   );
   Config<Float> rainbowSpeedConfig = this.register(
      new NumberConfig<>(
         "Rainbow-Speed", "The speed for the rainbow color cycling", 0.1F, 50.0F, 100.0F, () -> this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.OFF
      )
   );
   Config<Integer> rainbowSaturationConfig = this.register(
      new NumberConfig<>(
         "Rainbow-Saturation",
         "The saturation of rainbow colors",
         0,
         35,
         100,
         () -> this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.OFF && this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.GRADIENT
      )
   );
   Config<Integer> rainbowBrightnessConfig = this.register(
      new NumberConfig<>(
         "Rainbow-Brightness",
         "The brightness of rainbow colors",
         0,
         100,
         100,
         () -> this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.OFF && this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.GRADIENT
      )
   );
   Config<Float> rainbowDifferenceConfig = this.register(
      new NumberConfig<>(
         "Rainbow-Difference",
         "The difference offset for rainbow colors",
         0.1F,
         40.0F,
         100.0F,
         () -> this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.OFF
      )
   );
   private final DecimalFormat decimal = new DecimalFormat("0.0");
   private final DecimalFormat decimal2 = new DecimalFormat("0.0#");
   private long rainbowOffset;
   private float topLeft;
   private float topRight;
   private float bottomLeft;
   private float bottomRight;
   private boolean renderingUp;
   private final Animation chatOpenAnimation = new Animation(false, 200.0F, Easing.LINEAR);
   private final PerSecondCounter fpsCounter = new PerSecondCounter();
   private final Map<String, HUDModule.HudRenderModule> hudRenderModules = new HashMap<>();
   private final Map<HUDModule.PotionData, Animation> hudRenderPotions = new ConcurrentSkipListMap<>();
   private final Timer serverStatus = new CacheTimer();
   private final Animation statusAnimation = new Animation(false, 300.0F, Easing.LINEAR);

   public HUDModule() {
      super("HUD", "Displays the HUD (heads up display) screen.", ModuleCategory.CLIENT);
      INSTANCE = this;
   }

   public static HUDModule getInstance() {
      return INSTANCE;
   }

   private void arrayListRenderModule(RenderOverlayEvent.Post event, ToggleModule toggleModule, long drawnCount) {
      if (!(toggleModule.getAnimation().getFactor() <= 0.01F) && !toggleModule.isHidden()) {
         HUDModule.HudRenderModule hudRender = this.hudRenderModules.get(toggleModule.getId());
         if (hudRender != null) {
            hudRender.draw(event.getContext(), drawnCount);
         }
      }
   }

   @EventListener
   public void onDeath(EntityDeathEvent event) {
      if (event.getEntity() == mc.player) {
         this.hudRenderPotions.clear();
      }
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.hudRenderPotions.clear();
   }

   @EventListener
   public void onRenderOpenChat(RenderOpenChatEvent event) {
      event.cancel();
      event.setAnimation((float)this.chatOpenAnimation.getFactor());
   }

   @EventListener
   public void onRenderOverlayPost(RenderOverlayEvent.Post event) {
      if (this.hudRenderModules.isEmpty()) {
         for (Module module : Managers.MODULE.getModules()) {
            if (module instanceof ToggleModule toggleModule) {
               this.hudRenderModules.put(module.getId(), new HUDModule.HudRenderModule(toggleModule));
            }
         }
      }

      int updates = Mth.clamp(Math.round(240.0F / mc.getFps()), 1, 10);

      for (int i = 0; i < updates; i++) {
         for (HUDModule.HudRenderModule hudRenderModule : this.hudRenderModules.values()) {
            hudRenderModule.updateAnimation();
         }
      }

      this.fpsCounter.updateCounter();
      if (mc.player != null && mc.level != null) {
         for (Entry<HUDModule.PotionData, Animation> entry : this.hudRenderPotions.entrySet()) {
            HUDModule.PotionData effectInstance = entry.getKey();
            Animation animation = entry.getValue();
            effectInstance.update();
            if (mc.player.getEffect(effectInstance.getType()) == null && animation.getFactor() < 0.01F) {
               this.hudRenderPotions.remove(effectInstance);
            }
         }

         if (mc.options.hideGui) {
            return;
         }

         Window res = mc.getWindow();
         if (this.serverStatusConfig.getValue()) {
            this.statusAnimation.setState(this.serverStatus.passed(1000));
            String warning = String.format("§fServer not responding §7(§r%s.s§7)", this.decimal.format(this.serverStatus.getElapsedTime() / 1000.0));
            int width = RenderManager.textWidth(warning);
            Color color = ColorUtil.interpolateColor(
               Mth.clamp(((float)this.serverStatus.getElapsedTime() - 1000.0F) / 5000.0F, 0.0F, 1.0F), Color.RED, Color.GREEN
            );
            int alpha = (int)(255.0 * this.statusAnimation.getFactor());
            RenderManager.renderText(
               event.getContext(), warning, res.getGuiScaledWidth() / 2.0F - width / 2.0F, 4.0F, ColorUtil.fixTransparency(color.getRGB(), alpha)
            );
         }

         this.rainbowOffset = 0L;
         this.topLeft = 2.0F;
         this.topRight = this.topLeft;
         this.bottomLeft = res.getGuiScaledHeight() - 11.0F;
         this.bottomRight = this.bottomLeft;
         this.renderingUp = this.renderingConfig.getValue() == HUDModule.Rendering.UP;
         this.bottomLeft = this.bottomLeft - (float)(14.0 * this.chatOpenAnimation.getFactor());
         this.bottomRight = this.bottomRight - (float)(14.0 * this.chatOpenAnimation.getFactor());
         if (this.potionHudConfig.getValue() == HUDModule.VanillaHud.MOVE && !mc.player.getActiveEffects().isEmpty()) {
            this.topRight += 27.0F;
         }

         List<Module> modules = Managers.MODULE.getModules();
         long drawnCount = modules.stream().filter(ToggleModule.class::isInstance).map(ToggleModule.class::cast).filter(m -> !m.isHidden()).count();
         if (this.watermarkConfig.getValue()) {
            RenderManager.renderText(
               event.getContext(),
               String.format("%s %s (%s-%s%s)", "Shoreline", "1.0", "beta", "25", !"fa4cb0f".equals("null") ? "-fa4cb0f" : ""),
               2.0F,
               this.topLeft,
               this.getHudColor(drawnCount - this.rainbowOffset)
            );
            this.topLeft = this.topLeft + RenderManager.textHeight();
         }

         if (this.userInfo.getValue()) {
            RenderManager.renderText(event.getContext(), String.format("UID %s", "1"), 2.0F, this.topLeft, this.getHudColor(drawnCount - this.rainbowOffset));
            this.topLeft = this.topLeft + RenderManager.textHeight();
         }

         if (this.arraylistConfig.getValue()) {
            Stream<ToggleModule> moduleStream = modules.stream().filter(ToggleModule.class::isInstance).map(ToggleModule.class::cast);

            moduleStream = switch ((HUDModule.Ordering)this.orderingConfig.getValue()) {
               case LENGTH -> StreamUtils.sortCached(moduleStream, m -> -RenderManager.textWidth(this.getFormattedModule(m)));
               case ALPHABETICAL -> StreamUtils.sortCached(moduleStream, ConfigContainer::getName);
            };
            moduleStream.forEach(t -> this.arrayListRenderModule(event, t, drawnCount));
         }

         if (this.coordsConfig.getValue()) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            boolean nether = mc.level.dimension() == Level.NETHER;
            RenderManager.renderText(
               event.getContext(),
               String.format(
                  "XYZ §f%s, %s, %s " + (this.netherCoordsConfig.getValue() ? "§7[§f%s, %s§7]" : ""),
                  this.decimal.format(x),
                  this.decimal.format(y),
                  this.decimal.format(z),
                  nether ? this.decimal.format(x * 8.0) : this.decimal.format(x / 8.0),
                  nether ? this.decimal.format(z * 8.0) : this.decimal.format(z / 8.0)
               ),
               2.0F,
               this.bottomLeft,
               this.getHudColor(this.rainbowOffset)
            );
            this.bottomLeft = this.bottomLeft - RenderManager.textHeight();
         }

         if (this.directionConfig.getValue()) {
            Direction direction = mc.player.getDirection();
            String dir = EnumFormatter.formatDirection(direction);
            String axis = EnumFormatter.formatAxis(direction.getAxis());
            boolean pos = direction.getAxisDirection() == AxisDirection.POSITIVE;
            String rotationText = String.format(", %s", (int)Mth.wrapDegrees(Managers.ROTATION.getServerYaw()));
            RenderManager.renderText(
               event.getContext(),
               String.format("%s §7[§f%s%s%s§7]", dir, axis, pos ? "+" : "-", this.rotationConfig.getValue() ? rotationText : ""),
               2.0F,
               this.bottomLeft,
               this.getHudColor(this.rainbowOffset)
            );
         }

         if (this.potionEffectsConfig.getValue()) {
            for (Entry<HUDModule.PotionData, Animation> e1 : this.hudRenderPotions.entrySet()) {
               HUDModule.PotionData e = e1.getKey();
               Animation animation = e1.getValue();
               MobEffect effect = e.getType().value();
               if (effect != MobEffects.NIGHT_VISION) {
                  boolean infinite = e.getDuration() == -1;
                  boolean amplifier = e.getAmplifier() + 1 > 1 && !infinite;
                  String duration;
                  if (infinite) {
                     duration = "Inf";
                  } else {
                     int i = Mth.floor((float)e.getDuration());
                     duration = StringUtil.formatTickDuration(i, mc.level.tickRateManager().tickrate());
                  }

                  String text = String.format("%s %s§f%s", effect.getDisplayName().getString(), amplifier ? e.getAmplifier() + 1 + " " : "", duration);
                  int width = RenderManager.textWidth(text);
                  float x = (width + 1.0F) * (float)animation.getFactor();

                  int potionColor = switch ((HUDModule.PotionColors)this.potionColorsConfig.getValue()) {
                     case NORMAL -> ColorUtil.withAlpha(effect.getColor(), (int)(255.0 * animation.getFactor()));
                     case OLD -> ColorUtil.withAlpha(this.getLiquidColor(e.getType().getRegisteredName(), effect), (int)(255.0 * animation.getFactor()));
                     case OFF -> ColorUtil.withAlpha(this.getHudColor(this.rainbowOffset), (int)(255.0 * animation.getFactor()));
                  };
                  RenderManager.renderText(
                     event.getContext(),
                     text,
                     res.getGuiScaledWidth() - x,
                     this.renderingUp ? this.bottomRight : this.topRight,
                     ColorUtil.fixTransparency(potionColor, (float)animation.getFactor())
                  );
                  if (this.renderingUp) {
                     this.bottomRight = this.bottomRight - RenderManager.textHeight();
                  } else {
                     this.topRight = this.topRight + RenderManager.textHeight();
                  }

                  this.rainbowOffset++;
               }
            }
         }

         if (this.serverBrandConfig.getValue()) {
            String brand = mc.player.connection.serverBrand();
            int width = RenderManager.textWidth(brand);
            RenderManager.renderText(
               event.getContext(),
               brand,
               res.getGuiScaledWidth() - width - 1.0F,
               this.renderingUp ? this.bottomRight : this.topRight,
               this.getHudColor(this.rainbowOffset)
            );
            if (this.renderingUp) {
               this.bottomRight = this.bottomRight - RenderManager.textHeight();
            } else {
               this.topRight = this.topRight + RenderManager.textHeight();
            }

            this.rainbowOffset++;
         }

         if (this.speedConfig.getValue() != HUDModule.SpeedHud.OFF) {
            double x = mc.player.getX() - mc.player.xo;
            double z = mc.player.getZ() - mc.player.zo;
            float timer = TimerModule.getInstance().isEnabled() ? TimerModule.getInstance().getTimer() : 1.0F;
            double speed;
            if (this.speedConfig.getValue() == HUDModule.SpeedHud.K_M_H) {
               double dist = Math.sqrt(x * x + z * z) / 1000.0;
               double div = 1.388888888888889E-5;
               speed = dist / div * timer;
            } else {
               x *= 20.0;
               z *= 20.0;
               double dist = Math.sqrt(x * x + z * z);
               speed = Math.abs(dist) * timer;
            }

            String text = String.format("Speed §f%s%s", this.decimal2.format(speed), this.speedConfig.getValue() == HUDModule.SpeedHud.K_M_H ? "km/h" : "b/s");
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(
               event.getContext(),
               text,
               res.getGuiScaledWidth() - width - 1.0F,
               this.renderingUp ? this.bottomRight : this.topRight,
               this.getHudColor(this.rainbowOffset)
            );
            if (this.renderingUp) {
               this.bottomRight = this.bottomRight - RenderManager.textHeight();
            } else {
               this.topRight = this.topRight + RenderManager.textHeight();
            }

            this.rainbowOffset++;
         }

         if (this.durabilityConfig.getValue() && mc.player.getMainHandItem().isDamageableItem()) {
            int n = mc.player.getMainHandItem().getMaxDamage();
            int n2 = mc.player.getMainHandItem().getDamageValue();
            String text1 = "Durability ";
            String text2 = String.valueOf(n - n2);
            int width = RenderManager.textWidth(text1);
            int width2 = RenderManager.textWidth(text2);
            Color color = ColorUtil.hslToColor((float)(n - n2) / n * 120.0F, 100.0F, 50.0F, 1.0F);
            RenderManager.renderText(
               event.getContext(),
               text1,
               res.getGuiScaledWidth() - width - width2 - 1.0F,
               this.renderingUp ? this.bottomRight : this.topRight,
               this.getHudColor(this.rainbowOffset)
            );
            RenderManager.renderText(
               event.getContext(), text2, res.getGuiScaledWidth() - width2 - 1.0F, this.renderingUp ? this.bottomRight : this.topRight, color.getRGB()
            );
            if (this.renderingUp) {
               this.bottomRight = this.bottomRight - RenderManager.textHeight();
            } else {
               this.topRight = this.topRight + RenderManager.textHeight();
            }

            this.rainbowOffset++;
         }

         if (this.pingConfig.getValue() && !mc.isLocalServer()) {
            int latency = FastLatencyModule.getInstance().isEnabled() ? (int)FastLatencyModule.getInstance().getLatency() : Managers.NETWORK.getClientLatency();
            String text = String.format("Ping §f%dms", latency);
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(
               event.getContext(),
               text,
               res.getGuiScaledWidth() - width - 1.0F,
               this.renderingUp ? this.bottomRight : this.topRight,
               this.getHudColor(this.rainbowOffset)
            );
            if (this.renderingUp) {
               this.bottomRight = this.bottomRight - RenderManager.textHeight();
            } else {
               this.topRight = this.topRight + RenderManager.textHeight();
            }

            this.rainbowOffset++;
         }

         if (this.packetsConfig.getValue()) {
            String text = String.format("Packets §f%s<-%s", Managers.NETWORK.getOutgoingPPS(), Managers.NETWORK.getIncomingPPS());
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(
               event.getContext(),
               text,
               res.getGuiScaledWidth() - width - 1.0F,
               this.renderingUp ? this.bottomRight : this.topRight,
               this.getHudColor(this.rainbowOffset)
            );
            if (this.renderingUp) {
               this.bottomRight = this.bottomRight - RenderManager.textHeight();
            } else {
               this.topRight = this.topRight + RenderManager.textHeight();
            }

            this.rainbowOffset++;
         }

         if (this.tpsConfig.getValue()) {
            float curr = Managers.TICK.getTpsCurrent();
            float avg = Managers.TICK.getTpsAverage();
            String text = String.format(
               "TPS §f%s §7[§f%s§7]", this.decimal2.format(curr) + (Managers.TICK.isTicksFilled() ? "" : "*"), this.decimal2.format(avg)
            );
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(
               event.getContext(),
               text,
               res.getGuiScaledWidth() - width - 1.0F,
               this.renderingUp ? this.bottomRight : this.topRight,
               this.getHudColor(this.rainbowOffset)
            );
            if (this.renderingUp) {
               this.bottomRight = this.bottomRight - RenderManager.textHeight();
            } else {
               this.topRight = this.topRight + RenderManager.textHeight();
            }

            this.rainbowOffset++;
         }

         if (this.chestsConfig.getValue()) {
            int singleChests = 0;
            int doubleChests = 0;

            for (BlockEntity blockEntity : BlockUtil.blockEntities()) {
               if (blockEntity instanceof ChestBlockEntity) {
                  BlockState state = blockEntity.getBlockState();
                  if (state.hasProperty(ChestBlock.TYPE)) {
                     ChestType chestType = state.getValue(ChestBlock.TYPE);
                     if (chestType == ChestType.SINGLE) {
                        singleChests++;
                     } else {
                        doubleChests++;
                     }
                  }
               }
            }

            String text = String.format("Chests §f%d", singleChests + doubleChests / 2);
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(
               event.getContext(),
               text,
               res.getGuiScaledWidth() - width - 1.0F,
               this.renderingUp ? this.bottomRight : this.topRight,
               this.getHudColor(this.rainbowOffset)
            );
            if (this.renderingUp) {
               this.bottomRight = this.bottomRight - RenderManager.textHeight();
            } else {
               this.topRight = this.topRight + RenderManager.textHeight();
            }

            this.rainbowOffset++;
         }

         if (this.fpsConfig.getValue()) {
            String text = String.format("FPS §f%d", this.fpsCounter.getPerSecond());
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(
               event.getContext(),
               text,
               res.getGuiScaledWidth() - width - 1.0F,
               this.renderingUp ? this.bottomRight : this.topRight,
               this.getHudColor(this.rainbowOffset)
            );
            this.rainbowOffset++;
         }

         if (this.armorConfig.getValue()) {
            Entity riding = mc.player.getVehicle();
            int xx = res.getGuiScaledWidth() / 2 + 15;
            int y = res.getGuiScaledHeight();
            int n1 = mc.player.getMaxAirSupply();
            int n2 = Math.min(mc.player.getAirSupply(), n1);
            if (mc.player.isEyeInFluid(FluidTags.WATER) || n2 < n1) {
               y -= 65;
            } else if (riding instanceof LivingEntity entity) {
               y -= 45 + (int)Math.ceil((entity.getMaxHealth() - 1.0F) / 20.0F) * 10;
            } else if (mc.player.isCreative()) {
               y -= 45;
            } else {
               y -= 55;
            }

            for (int i = 3; i >= 0; i--) {
               ItemStack armor = mc.player.getInventory().armor.get(i);
               int f = armor.getMaxDamage();
               int f2 = armor.getDamageValue();
               event.getContext().renderItem(armor, xx, y);
               event.getContext().renderItemDecorations(mc.font, armor, xx, y);
               if (this.armorDurabilityConfig.getValue() && !armor.isEmpty()) {
                  event.getContext().pose().scale(0.65F, 0.65F, 1.0F);
                  RenderManager.renderText(
                     event.getContext(),
                     Math.round((float)(f - f2) / f * 100.0F) + "%",
                     (xx + 2.0F) * 1.5384616F,
                     (y - 5.0F) * 1.5384616F,
                     ColorUtil.hslToColor((float)(f - f2) / f * 120.0F, 100.0F, 50.0F, 1.0F).getRGB()
                  );
                  event.getContext().pose().scale(1.5384616F, 1.5384616F, 1.0F);
               }

               xx += 18;
            }
         }

         if (this.totemsConfig.getValue() && !mc.player.isCreative() && !mc.player.isSpectator()) {
            int xx = res.getGuiScaledWidth() / 2 - 8;
            int y = res.getGuiScaledHeight() - 55;
            event.getContext().renderItem(new ItemStack(Items.TOTEM_OF_UNDYING), xx, y - 1);
            String totems = String.valueOf(InventoryUtil.count(Items.TOTEM_OF_UNDYING));
            event.getContext().pose().scale(0.75F, 0.75F, 1.0F);
            event.getContext().pose().translate(0.0F, 0.0F, 200.0F);
            RenderManager.renderText(event.getContext(), totems, (xx + 19 - RenderManager.textWidth(totems)) * 1.333333F, (y + 9) * 1.333333F, -1);
            event.getContext().pose().scale(1.333333F, 1.333333F, 1.0F);
         }
      }
   }

   @EventListener
   public void onStatusEffectAdd(StatusEffectEvent.Add event) {
      MobEffectInstance instance = event.getStatusEffect();
      HUDModule.PotionData data = new HUDModule.PotionData(instance.getEffect(), instance.getAmplifier(), instance.getDuration());
      if (this.hudRenderPotions.keySet().removeIf(d -> data.getType() == d.getType())) {
         this.hudRenderPotions.put(data, new Animation(true, 250.0F, Easing.SINE_IN_OUT));
      } else {
         Animation anim = new Animation(false, 250.0F, Easing.SINE_IN_OUT);
         anim.setState(true);
         this.hudRenderPotions.put(data, anim);
      }
   }

   @EventListener
   public void onStatusEffectRemove(StatusEffectEvent.Remove event) {
      for (HUDModule.PotionData data : this.hudRenderPotions.keySet()) {
         if (data.getType().equals(event.getType())) {
            this.hudRenderPotions.get(data).setState(false);
            break;
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      this.serverStatus.reset();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.level == null || mc.isPaused()) {
         this.serverStatus.reset();
      }
   }

   @EventListener
   public void onChatOpen(ScreenOpenEvent event) {
      if (event.getScreen() == null && this.chatOpenAnimation.getState()) {
         this.chatOpenAnimation.setState(false);
      } else if (event.getScreen() instanceof ChatScreen) {
         this.chatOpenAnimation.setState(true);
      }
   }

   @EventListener
   public void onRenderOverlayStatusEffect(RenderOverlayEvent.StatusEffect event) {
      if (this.potionHudConfig.getValue() == HUDModule.VanillaHud.HIDE) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderOverlayItemName(RenderOverlayEvent.ItemName event) {
      if (this.itemNameConfig.getValue() != HUDModule.VanillaHud.KEEP) {
         event.cancel();
      }

      if (this.itemNameConfig.getValue() == HUDModule.VanillaHud.MOVE) {
         Window window = mc.getWindow();
         int x = window.getGuiScaledWidth() / 2 - 90;
         int y = window.getGuiScaledHeight() - 49;
         boolean armor = !mc.player.getInventory().armor.isEmpty();
         if (mc.player.getAbsorptionAmount() > 0.0F) {
            y -= 9;
         }

         if (armor) {
            y -= 9;
         }

         event.setX(x);
         event.setY(y);
      }
   }

   private int getHudColor(long rainbowOffset) {
      return switch ((HUDModule.RainbowMode)this.rainbowModeConfig.getValue()) {
         case OFF -> ColorsModule.getInstance().getRGB();
         case GRADIENT -> {
            float speed = Math.max(100.0F - this.rainbowSpeedConfig.getValue(), 0.1F);
            float difference = Math.max(100.0F - this.rainbowDifferenceConfig.getValue(), 0.1F);
            double roundY = Math.sin(Math.toRadians((float)rainbowOffset * difference + (double)System.currentTimeMillis() / speed));
            roundY = Math.abs(roundY);
            yield ColorUtil.interpolateColor((float)Mth.clamp(roundY, 0.0, 1.0), ColorsModule.getInstance().getColor(), this.gradientColorConfig.getValue())
               .getRGB();
         }
         case GRADIENT_HUE -> this.rainbow(rainbowOffset);
         case STATIC_HUE -> this.rainbow(1L);
      };
   }

   private String getFormattedModule(Module module) {
      return module.getName() + this.getFormattedModuleData(module.getModuleData());
   }

   private String getFormattedModuleData(String metadata) {
      return !metadata.equals("ARRAYLIST_INFO") ? " §7[§f" + metadata + "§7]" : "";
   }

   public int getLiquidColor(String name, MobEffect effect) {
      String var3 = name.replace("minecraft:", "");

      return switch (var3) {
         case "speed" -> 8171462;
         case "slowness" -> 5926017;
         case "haste" -> 14270531;
         case "mining_fatigue" -> 4866583;
         case "strength" -> 9643043;
         case "instant_health" -> 16262179;
         case "instant_damage" -> 4393481;
         case "jump_boost" -> 2293580;
         case "nausea" -> 5578058;
         case "regeneration" -> 13458603;
         case "resistance" -> 10044730;
         case "fire_resistance" -> 14981690;
         case "water_breathing" -> 3035801;
         case "invisibility" -> 8356754;
         case "blindness" -> 2039587;
         case "night_vision" -> 2039713;
         case "hunger" -> 5797459;
         case "weakness" -> 4738376;
         case "poison" -> 5149489;
         case "wither" -> 3484199;
         case "health_boost" -> 16284963;
         case "absorption" -> 2445989;
         case "saturation" -> 16262179;
         case "glowing" -> 9740385;
         case "levitation" -> 13565951;
         case "luck" -> 3381504;
         case "unluck" -> 12624973;
         default -> effect.getColor();
      };
   }

   private int rainbow(long offset) {
      float hue = (float)(
         ((double)System.currentTimeMillis() * (this.rainbowSpeedConfig.getValue() / 10.0F) + offset * 500L)
            % (30000.0F / (this.rainbowDifferenceConfig.getValue() / 100.0F))
            / (30000.0F / (this.rainbowDifferenceConfig.getValue() / 20.0F))
      );
      return Color.HSBtoRGB(hue, this.rainbowSaturationConfig.getValue().intValue() / 100.0F, this.rainbowBrightnessConfig.getValue().intValue() / 100.0F);
   }

   public int alpha(long offset) {
      float[] hsb = new float[3];
      Color color = ColorsModule.getInstance().getColor();
      Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
      float b = (float)(
         ((double)System.currentTimeMillis() * (this.rainbowSpeedConfig.getValue() / 10.0F) + offset * 500L)
            % (30000.0F / (this.rainbowDifferenceConfig.getValue() / 100.0F))
            / (30000.0F / (this.rainbowDifferenceConfig.getValue() / 20.0F))
      );
      float brightness = hsb[2] * Math.abs(b % 1.0F - 0.55F) + 0.4F;
      return Color.HSBtoRGB(hsb[0], hsb[1], brightness);
   }

   public float getChatAnimation() {
      return (float)this.chatOpenAnimation.getFactor();
   }

   public class HudRenderModule {
      private final ToggleModule module;
      private double x;
      private long startTime;
      private double endpoint;
      private int prevTextWidth;
      private boolean wasDrawing;

      public HudRenderModule(ToggleModule module) {
         this.module = module;
      }

      public void updateAnimation() {
         String text = HUDModule.this.getFormattedModule(this.module);
         int textWidth = RenderManager.textWidth(text);
         boolean drawing = this.module.isEnabled() && !this.module.isHidden();
         if (drawing != this.wasDrawing || this.prevTextWidth != textWidth) {
            this.startTime = System.currentTimeMillis();
            if (drawing) {
               this.endpoint = -textWidth - 2.0F;
            } else {
               this.endpoint = 2.0;
            }

            this.wasDrawing = drawing;
            this.prevTextWidth = textWidth;
         }

         double animationProgress = Math.min(
            (double)((float)(System.currentTimeMillis() - this.startTime) / HUDModule.this.animTimeConfig.getValue().intValue()), 1.0
         );
         double factor = Easing.BOUNCE_IN_OUT.ease(animationProgress);
         this.x = this.x * (1.0 - factor) + this.endpoint * factor;
      }

      public void draw(GuiGraphics context, long drawnCount) {
         int color = ColorUtil.fixTransparency(
            HUDModule.this.getHudColor(drawnCount - HUDModule.this.rainbowOffset), (float)this.module.getAnimation().getFactor()
         );
         RenderManager.renderText(
            context,
            HUDModule.this.getFormattedModule(this.module),
            Globals.mc.getWindow().getGuiScaledWidth() + (float)this.x,
            HUDModule.this.renderingUp ? HUDModule.this.topRight : HUDModule.this.bottomRight,
            color
         );
         if (HUDModule.this.renderingUp) {
            HUDModule.this.topRight = HUDModule.this.topRight + RenderManager.textHeight();
         } else {
            HUDModule.this.bottomRight = HUDModule.this.bottomRight - RenderManager.textHeight();
         }

         HUDModule.this.rainbowOffset++;
      }
   }

   public static enum Ordering {
      LENGTH,
      ALPHABETICAL;
   }

   public static enum PotionColors {
      NORMAL,
      OLD,
      OFF;
   }

   public static class PotionData implements Comparable<HUDModule.PotionData> {
      private final Holder<MobEffect> type;
      private final int amplifier;
      private int duration;

      public PotionData(Holder<MobEffect> type, int amplifier, int duration) {
         this.type = type;
         this.amplifier = amplifier;
         this.duration = duration;
      }

      public void update() {
         MobEffectInstance instance = Globals.mc.player.getEffect(this.type);
         if (instance != null) {
            this.duration = instance.getDuration();
         }
      }

      public String getPotionName() {
         return this.getType().value().getDisplayName().getString();
      }

      public Holder<MobEffect> getType() {
         return this.type;
      }

      public int getAmplifier() {
         return this.amplifier;
      }

      public int getDuration() {
         return this.duration;
      }

      public int compareTo(HUDModule.PotionData other) {
         return other.getPotionName().compareTo(this.getPotionName());
      }
   }

   public static enum RainbowMode {
      OFF,
      GRADIENT,
      GRADIENT_HUE,
      STATIC_HUE;
   }

   public static enum Rendering {
      UP,
      DOWN;
   }

   public static enum SpeedHud {
      K_M_H,
      B_P_S,
      OFF;
   }

   public static enum VanillaHud {
      MOVE,
      HIDE,
      KEEP;
   }
}
