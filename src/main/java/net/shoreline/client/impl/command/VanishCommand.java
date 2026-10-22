package net.shoreline.client.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.mixin.accessor.AccessorEntity;
import net.shoreline.client.util.chat.ChatUtil;

public class VanishCommand extends Command {
   private Entity mount;

   public VanishCommand() {
      super("Vanish", "Desyncs the riding entity", literal("vanish"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      ((LiteralArgumentBuilder)builder.then(argument("mount", StringArgumentType.string()).suggests(suggest(new String[]{"mount", "remount"})).executes(c -> {
         String dismount = StringArgumentType.getString(c, "mount");
         if (dismount.equalsIgnoreCase("dismount")) {
            if (mc.player.isHandsBusy() && mc.player.getVehicle() != null) {
               if (this.mount != null) {
                  ChatUtil.error("Entity vanished, must remount before mounting!");
                  return 0;
               }

               this.mount = mc.player.getVehicle();
               mc.player.removeVehicle();
               mc.level.removeEntity(this.mount.getId(), RemovalReason.DISCARDED);
            }
         } else if (dismount.equalsIgnoreCase("remount")) {
            if (this.mount == null) {
               ChatUtil.error("No vanished entity!");
               return 0;
            }

            ((AccessorEntity)this.mount).hookUnsetRemoved();
            mc.level.addEntity(this.mount);
            mc.player.startRiding(this.mount, true);
            this.mount = null;
         }

         return 1;
      }))).executes(c -> {
         ChatUtil.error("Invalid usage! Usage: " + this.getUsage());
         return 1;
      });
   }
}
