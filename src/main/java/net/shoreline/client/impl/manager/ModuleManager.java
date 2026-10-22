package net.shoreline.client.impl.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.shoreline.client.Shoreline;
import net.shoreline.client.ShorelineMod;
import net.shoreline.client.api.module.Module;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.module.client.AnticheatModule;
import net.shoreline.client.impl.module.client.BaritoneModule;
import net.shoreline.client.impl.module.client.CapesModule;
import net.shoreline.client.impl.module.client.ChatModule;
import net.shoreline.client.impl.module.client.ClickGuiModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.client.FontModule;
import net.shoreline.client.impl.module.client.HUDModule;
import net.shoreline.client.impl.module.client.RichPresenceModule;
import net.shoreline.client.impl.module.client.RotationsModule;
import net.shoreline.client.impl.module.client.SocialsModule;
import net.shoreline.client.impl.module.combat.AuraModule;
import net.shoreline.client.impl.module.combat.AutoAnchorModule;
import net.shoreline.client.impl.module.combat.AutoArmorModule;
import net.shoreline.client.impl.module.combat.AutoBowReleaseModule;
import net.shoreline.client.impl.module.combat.AutoCrawlTrapModule;
import net.shoreline.client.impl.module.combat.AutoCrystalModule;
import net.shoreline.client.impl.module.combat.AutoLogModule;
import net.shoreline.client.impl.module.combat.AutoTotemModule;
import net.shoreline.client.impl.module.combat.AutoTrapModule;
import net.shoreline.client.impl.module.combat.AutoWebModule;
import net.shoreline.client.impl.module.combat.AutoXPModule;
import net.shoreline.client.impl.module.combat.BasePlaceModule;
import net.shoreline.client.impl.module.combat.BowAimModule;
import net.shoreline.client.impl.module.combat.ClickCrystalModule;
import net.shoreline.client.impl.module.combat.CriticalsModule;
import net.shoreline.client.impl.module.combat.HoleFillModule;
import net.shoreline.client.impl.module.combat.KeepSprintModule;
import net.shoreline.client.impl.module.combat.NoHitDelayModule;
import net.shoreline.client.impl.module.combat.ReplenishModule;
import net.shoreline.client.impl.module.combat.SelfBowModule;
import net.shoreline.client.impl.module.combat.SelfFillModule;
import net.shoreline.client.impl.module.combat.SelfTrapModule;
import net.shoreline.client.impl.module.combat.SurroundModule;
import net.shoreline.client.impl.module.combat.TriggerModule;
import net.shoreline.client.impl.module.exploit.AntiHungerModule;
import net.shoreline.client.impl.module.exploit.BacktrackModule;
import net.shoreline.client.impl.module.exploit.ChorusControlModule;
import net.shoreline.client.impl.module.exploit.ChorusInvincibilityModule;
import net.shoreline.client.impl.module.exploit.ClientSpoofModule;
import net.shoreline.client.impl.module.exploit.CrasherModule;
import net.shoreline.client.impl.module.exploit.DisablerModule;
import net.shoreline.client.impl.module.exploit.ExtendedFireworkModule;
import net.shoreline.client.impl.module.exploit.FakeLatencyModule;
import net.shoreline.client.impl.module.exploit.FastLatencyModule;
import net.shoreline.client.impl.module.exploit.FastProjectileModule;
import net.shoreline.client.impl.module.exploit.GodModeModule;
import net.shoreline.client.impl.module.exploit.InventorySyncModule;
import net.shoreline.client.impl.module.exploit.NewChunksModule;
import net.shoreline.client.impl.module.exploit.NoMineAnimationModule;
import net.shoreline.client.impl.module.exploit.PacketCancelerModule;
import net.shoreline.client.impl.module.exploit.PacketFlyModule;
import net.shoreline.client.impl.module.exploit.PhaseModule;
import net.shoreline.client.impl.module.exploit.ReachModule;
import net.shoreline.client.impl.module.misc.AntiAFKModule;
import net.shoreline.client.impl.module.misc.AntiAimModule;
import net.shoreline.client.impl.module.misc.AntiSpamModule;
import net.shoreline.client.impl.module.misc.AntiVanishModule;
import net.shoreline.client.impl.module.misc.AutoAcceptModule;
import net.shoreline.client.impl.module.misc.AutoAnvilRenameModule;
import net.shoreline.client.impl.module.misc.AutoEatModule;
import net.shoreline.client.impl.module.misc.AutoFishModule;
import net.shoreline.client.impl.module.misc.AutoFrameDupeModule;
import net.shoreline.client.impl.module.misc.AutoMountModule;
import net.shoreline.client.impl.module.misc.AutoReconnectModule;
import net.shoreline.client.impl.module.misc.AutoRespawnModule;
import net.shoreline.client.impl.module.misc.BetterChatModule;
import net.shoreline.client.impl.module.misc.BetterInvModule;
import net.shoreline.client.impl.module.misc.ChatNotifierModule;
import net.shoreline.client.impl.module.misc.ChestStealerModule;
import net.shoreline.client.impl.module.misc.ChestSwapModule;
import net.shoreline.client.impl.module.misc.FakePlayerModule;
import net.shoreline.client.impl.module.misc.InvCleanerModule;
import net.shoreline.client.impl.module.misc.MiddleClickModule;
import net.shoreline.client.impl.module.misc.NoPacketKickModule;
import net.shoreline.client.impl.module.misc.NoSoundLagModule;
import net.shoreline.client.impl.module.misc.PMSoundModule;
import net.shoreline.client.impl.module.misc.PacketLoggerModule;
import net.shoreline.client.impl.module.misc.ServerModule;
import net.shoreline.client.impl.module.misc.ShulkerceptionModule;
import net.shoreline.client.impl.module.misc.SkinBlinkModule;
import net.shoreline.client.impl.module.misc.SpammerModule;
import net.shoreline.client.impl.module.misc.SwingModule;
import net.shoreline.client.impl.module.misc.TimerModule;
import net.shoreline.client.impl.module.misc.TrueDurabilityModule;
import net.shoreline.client.impl.module.misc.UnfocusedFPSModule;
import net.shoreline.client.impl.module.misc.XCarryModule;
import net.shoreline.client.impl.module.movement.AntiLevitationModule;
import net.shoreline.client.impl.module.movement.AutoWalkModule;
import net.shoreline.client.impl.module.movement.ElytraFlyModule;
import net.shoreline.client.impl.module.movement.EntityControlModule;
import net.shoreline.client.impl.module.movement.EntitySpeedModule;
import net.shoreline.client.impl.module.movement.FakeLagModule;
import net.shoreline.client.impl.module.movement.FastFallModule;
import net.shoreline.client.impl.module.movement.FastSwimModule;
import net.shoreline.client.impl.module.movement.FireworkBoostModule;
import net.shoreline.client.impl.module.movement.FlightModule;
import net.shoreline.client.impl.module.movement.IceSpeedModule;
import net.shoreline.client.impl.module.movement.JesusModule;
import net.shoreline.client.impl.module.movement.LongJumpModule;
import net.shoreline.client.impl.module.movement.NoAccelModule;
import net.shoreline.client.impl.module.movement.NoFallModule;
import net.shoreline.client.impl.module.movement.NoJumpDelayModule;
import net.shoreline.client.impl.module.movement.NoSlowModule;
import net.shoreline.client.impl.module.movement.ParkourModule;
import net.shoreline.client.impl.module.movement.SafeWalkModule;
import net.shoreline.client.impl.module.movement.SpeedModule;
import net.shoreline.client.impl.module.movement.SprintModule;
import net.shoreline.client.impl.module.movement.StepModule;
import net.shoreline.client.impl.module.movement.TickShiftModule;
import net.shoreline.client.impl.module.movement.TridentFlyModule;
import net.shoreline.client.impl.module.movement.VelocityModule;
import net.shoreline.client.impl.module.movement.YawModule;
import net.shoreline.client.impl.module.render.AmbienceModule;
import net.shoreline.client.impl.module.render.AnimationsModule;
import net.shoreline.client.impl.module.render.BlockHighlightModule;
import net.shoreline.client.impl.module.render.BreadcrumbsModule;
import net.shoreline.client.impl.module.render.BreakHighlightModule;
import net.shoreline.client.impl.module.render.ChamsModule;
import net.shoreline.client.impl.module.render.CrosshairModule;
import net.shoreline.client.impl.module.render.CrystalModelModule;
import net.shoreline.client.impl.module.render.ESPModule;
import net.shoreline.client.impl.module.render.ExtraTabModule;
import net.shoreline.client.impl.module.render.FreeLookModule;
import net.shoreline.client.impl.module.render.FreecamModule;
import net.shoreline.client.impl.module.render.FullbrightModule;
import net.shoreline.client.impl.module.render.HoleESPModule;
import net.shoreline.client.impl.module.render.KillEffectsModule;
import net.shoreline.client.impl.module.render.NameProtectModule;
import net.shoreline.client.impl.module.render.NametagsModule;
import net.shoreline.client.impl.module.render.NoBobModule;
import net.shoreline.client.impl.module.render.NoRenderModule;
import net.shoreline.client.impl.module.render.NoRotateModule;
import net.shoreline.client.impl.module.render.NoWeatherModule;
import net.shoreline.client.impl.module.render.ParticlesModule;
import net.shoreline.client.impl.module.render.PhaseESPModule;
import net.shoreline.client.impl.module.render.SearchModule;
import net.shoreline.client.impl.module.render.ShadersModule;
import net.shoreline.client.impl.module.render.SkyboxModule;
import net.shoreline.client.impl.module.render.StorageESPModule;
import net.shoreline.client.impl.module.render.TooltipsModule;
import net.shoreline.client.impl.module.render.TracersModule;
import net.shoreline.client.impl.module.render.TrajectoriesModule;
import net.shoreline.client.impl.module.render.TrueSightModule;
import net.shoreline.client.impl.module.render.ViewClipModule;
import net.shoreline.client.impl.module.render.ViewModelModule;
import net.shoreline.client.impl.module.render.WaypointsModule;
import net.shoreline.client.impl.module.render.ZoomModule;
import net.shoreline.client.impl.module.world.AirPlaceModule;
import net.shoreline.client.impl.module.world.AntiInteractModule;
import net.shoreline.client.impl.module.world.AutoMineModule;
import net.shoreline.client.impl.module.world.AutoToolModule;
import net.shoreline.client.impl.module.world.AvoidModule;
import net.shoreline.client.impl.module.world.FastPlaceModule;
import net.shoreline.client.impl.module.world.MultitaskModule;
import net.shoreline.client.impl.module.world.NoGlitchBlocksModule;
import net.shoreline.client.impl.module.world.NukerModule;
import net.shoreline.client.impl.module.world.ScaffoldModule;
import net.shoreline.client.impl.module.world.SpeedmineModule;
import net.shoreline.client.init.Managers;

public final class ModuleManager {
   private final Map<String, Module> modules = Collections.synchronizedMap(new LinkedHashMap<>());

   public ModuleManager() {
      this.register(
         new AnticheatModule(),
         new CapesModule(),
         new RichPresenceModule(),
         new ChatModule(),
         new ClickGuiModule(),
         new ColorsModule(),
         new FontModule(),
         new HUDModule(),
         new RotationsModule(),
         new SocialsModule(),
         new AuraModule(),
         new AutoAnchorModule(),
         new AutoArmorModule(),
         new AutoBowReleaseModule(),
         new AutoCrawlTrapModule(),
         new AutoCrystalModule(),
         new AutoLogModule(),
         new AutoTotemModule(),
         new AutoTrapModule(),
         new AutoWebModule(),
         new AutoXPModule(),
         new BasePlaceModule(),
         new BowAimModule(),
         new ClickCrystalModule(),
         new CriticalsModule(),
         new HoleFillModule(),
         new KeepSprintModule(),
         new NoHitDelayModule(),
         new ReplenishModule(),
         new SelfBowModule(),
         new SelfFillModule(),
         new SelfTrapModule(),
         new SurroundModule(),
         new TriggerModule(),
         new AntiHungerModule(),
         new BacktrackModule(),
         new ChorusControlModule(),
         new ChorusInvincibilityModule(),
         new ClientSpoofModule(),
         new CrasherModule(),
         new DisablerModule(),
         new ExtendedFireworkModule(),
         new FakeLatencyModule(),
         new FastLatencyModule(),
         new FastProjectileModule(),
         new GodModeModule(),
         new InventorySyncModule(),
         new NewChunksModule(),
         new NoMineAnimationModule(),
         new PacketCancelerModule(),
         new PacketFlyModule(),
         new PhaseModule(),
         new ReachModule(),
         new AntiAFKModule(),
         new AntiAimModule(),
         new AntiSpamModule(),
         new AntiVanishModule(),
         new AutoAcceptModule(),
         new AutoAnvilRenameModule(),
         new AutoEatModule(),
         new AutoFishModule(),
         new AutoFrameDupeModule(),
         new AutoMountModule(),
         new AutoReconnectModule(),
         new AutoRespawnModule(),
         new BetterChatModule(),
         new BetterInvModule(),
         new ChatNotifierModule(),
         new ChestSwapModule(),
         new ChestStealerModule(),
         new FakePlayerModule(),
         new InvCleanerModule(),
         new MiddleClickModule(),
         new NoPacketKickModule(),
         new NoSoundLagModule(),
         new PacketLoggerModule(),
         new PMSoundModule(),
         new ServerModule(),
         new ShulkerceptionModule(),
         new SkinBlinkModule(),
         new SpammerModule(),
         new SwingModule(),
         new TimerModule(),
         new TrueDurabilityModule(),
         new UnfocusedFPSModule(),
         new XCarryModule(),
         new AntiLevitationModule(),
         new AutoWalkModule(),
         new ElytraFlyModule(),
         new EntityControlModule(),
         new EntitySpeedModule(),
         new FakeLagModule(),
         new FastFallModule(),
         new FastSwimModule(),
         new FireworkBoostModule(),
         new FlightModule(),
         new IceSpeedModule(),
         new JesusModule(),
         new LongJumpModule(),
         new NoAccelModule(),
         new NoFallModule(),
         new NoJumpDelayModule(),
         new NoSlowModule(),
         new ParkourModule(),
         new SafeWalkModule(),
         new SpeedModule(),
         new SprintModule(),
         new StepModule(),
         new TickShiftModule(),
         new TridentFlyModule(),
         new VelocityModule(),
         new YawModule(),
         new AmbienceModule(),
         new AnimationsModule(),
         new BlockHighlightModule(),
         new BreadcrumbsModule(),
         new BreakHighlightModule(),
         new ChamsModule(),
         new CrosshairModule(),
         new CrystalModelModule(),
         new ESPModule(),
         new ExtraTabModule(),
         new FreecamModule(),
         new FreeLookModule(),
         new FullbrightModule(),
         new HoleESPModule(),
         new KillEffectsModule(),
         new NameProtectModule(),
         new NametagsModule(),
         new NoBobModule(),
         new NoRenderModule(),
         new NoRotateModule(),
         new NoWeatherModule(),
         new ParticlesModule(),
         new PhaseESPModule(),
         new SearchModule(),
         new ShadersModule(),
         new SkyboxModule(),
         new StorageESPModule(),
         new TooltipsModule(),
         new TracersModule(),
         new TrajectoriesModule(),
         new TrueSightModule(),
         new ViewClipModule(),
         new ViewModelModule(),
         new WaypointsModule(),
         new ZoomModule(),
         new AirPlaceModule(),
         new AntiInteractModule(),
         new AutoMineModule(),
         new AutoToolModule(),
         new AvoidModule(),
         new FastPlaceModule(),
         new MultitaskModule(),
         new NoGlitchBlocksModule(),
         new NukerModule(),
         new ScaffoldModule(),
         new SpeedmineModule()
      );
      if (ShorelineMod.isBaritonePresent()) {
         this.register(new BaritoneModule());
      }

      for (Module module : this.getModules()) {
         if (module instanceof ToggleModule t) {
            Managers.MACRO.register(t.getKeybinding());
         }
      }

      Shoreline.info("Registered {} modules!", this.modules.size());
   }

   public void postInit() {
   }

   private void register(Module... modules) {
      for (Module module : modules) {
         this.register(module);
      }
   }

   private void register(Module module) {
      this.modules.put(module.getId(), module);
   }

   public Module getModuleById(String id) {
      return this.modules.get(id);
   }

   public List<Module> getModules() {
      return new ArrayList<>(this.modules.values());
   }
}
