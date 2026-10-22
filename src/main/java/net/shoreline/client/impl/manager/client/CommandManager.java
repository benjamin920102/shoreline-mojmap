package net.shoreline.client.impl.manager.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.api.module.Module;
import net.shoreline.client.impl.command.BindCommand;
import net.shoreline.client.impl.command.ConfigCommand;
import net.shoreline.client.impl.command.ConnectCommand;
import net.shoreline.client.impl.command.CoordsCommand;
import net.shoreline.client.impl.command.CustomFontCommand;
import net.shoreline.client.impl.command.DisableAllCommand;
import net.shoreline.client.impl.command.DrawnCommand;
import net.shoreline.client.impl.command.FriendCommand;
import net.shoreline.client.impl.command.HClipCommand;
import net.shoreline.client.impl.command.HelpCommand;
import net.shoreline.client.impl.command.HideAllCommand;
import net.shoreline.client.impl.command.HistoryCommand;
import net.shoreline.client.impl.command.LeaveCommand;
import net.shoreline.client.impl.command.LoadCommand;
import net.shoreline.client.impl.command.MacroCommand;
import net.shoreline.client.impl.command.ModuleCommand;
import net.shoreline.client.impl.command.ModulesCommand;
import net.shoreline.client.impl.command.NbtCommand;
import net.shoreline.client.impl.command.NotifyCommand;
import net.shoreline.client.impl.command.OpenFolderCommand;
import net.shoreline.client.impl.command.PrefixCommand;
import net.shoreline.client.impl.command.QueueCommand;
import net.shoreline.client.impl.command.RegearCommand;
import net.shoreline.client.impl.command.ReloadSoundCommand;
import net.shoreline.client.impl.command.ResetCommand;
import net.shoreline.client.impl.command.ResetGuiCommand;
import net.shoreline.client.impl.command.SaveCommand;
import net.shoreline.client.impl.command.SkinGrabCommand;
import net.shoreline.client.impl.command.StatsCommand;
import net.shoreline.client.impl.command.ToggleCommand;
import net.shoreline.client.impl.command.VClipCommand;
import net.shoreline.client.impl.command.VanishCommand;
import net.shoreline.client.impl.command.WaypointCommand;
import net.shoreline.client.impl.command.YawCommand;
import net.shoreline.client.impl.event.gui.chat.ChatMessageEvent;
import net.shoreline.client.impl.event.gui.screen.SuggestChatEvent;
import net.shoreline.client.impl.event.keyboard.KeyboardInputEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class CommandManager implements Globals {
   private final List<Command> commands = new ArrayList<>();
   private String prefix = ".";
   private int prefixKey = 46;
   private final CommandDispatcher<SharedSuggestionProvider> dispatcher = new CommandDispatcher();
   private final SharedSuggestionProvider source = new ClientSuggestionProvider(null, mc);

   public CommandManager() {
      EventBus.INSTANCE.subscribe(this);
      this.register(
         new BindCommand(),
         new ConfigCommand(),
         new ConnectCommand(),
         new CoordsCommand(),
         new DisableAllCommand(),
         new DrawnCommand(),
         new CustomFontCommand(),
         new FriendCommand(),
         new HClipCommand(),
         new HelpCommand(),
         new HideAllCommand(),
         new HistoryCommand(),
         new LeaveCommand(),
         new LoadCommand(),
         new MacroCommand(),
         new ModulesCommand(),
         new NbtCommand(),
         new NotifyCommand(),
         new OpenFolderCommand(),
         new PrefixCommand(),
         new QueueCommand(),
         new RegearCommand(),
         new ResetCommand(),
         new ResetGuiCommand(),
         new SaveCommand(),
         new ReloadSoundCommand(),
         new SkinGrabCommand(),
         new StatsCommand(),
         new ToggleCommand(),
         new VanishCommand(),
         new VClipCommand(),
         new WaypointCommand(),
         new YawCommand()
      );

      for (Module module : Managers.MODULE.getModules()) {
         this.register(new ModuleCommand(module));
      }

      Shoreline.info("Registered {} commands!", this.commands.size());

      for (Command command : this.commands) {
         for (LiteralArgumentBuilder<SharedSuggestionProvider> builder : command.getCommandBuilders()) {
            command.buildCommand(builder);
            this.dispatcher.register(builder);
         }
      }
   }

   @EventListener(
      priority = 999
   )
   public void onChatMessage(ChatMessageEvent.Client event) {
      String text = event.getMessage().trim();
      if (text.startsWith(this.prefix)) {
         String literal = text.substring(1);
         event.cancel();
         mc.gui.getChat().addRecentChat(text);

         try {
            this.dispatcher.execute(this.dispatcher.parse(literal, this.source));
         } catch (Exception var5) {
         }
      }
   }

   @EventListener
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (event.getAction() == 1 && event.getKeycode() == this.prefixKey && mc.screen == null) {
         event.cancel();
         mc.setScreen(new ChatScreen(""));
      }
   }

   @EventListener
   public void onChatSuggest(SuggestChatEvent event) {
      event.setPrefix(this.prefix);
      event.setDispatcher(this.dispatcher);
      event.setSource(this.source);
   }

   private LiteralArgumentBuilder<Object> redirectBuilder(String alias, LiteralCommandNode<Object> destination) {
      LiteralArgumentBuilder<Object> literalArgumentBuilder = (LiteralArgumentBuilder<Object>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)LiteralArgumentBuilder.literal(
                  alias.toLowerCase()
               )
               .requires(destination.getRequirement()))
            .forward(destination.getRedirect(), destination.getRedirectModifier(), destination.isFork()))
         .executes(destination.getCommand());

      for (CommandNode<Object> child : destination.getChildren()) {
         literalArgumentBuilder.then(child);
      }

      return literalArgumentBuilder;
   }

   private void register(Command... commands) {
      for (Command command : commands) {
         this.register(command);
      }
   }

   private void register(Command command) {
      this.commands.add(command);
   }

   public List<Command> getCommands() {
      return this.commands;
   }

   public Command getCommand(String name) {
      for (Command command : this.commands) {
         if (command.getName().equalsIgnoreCase(name)) {
            return command;
         }
      }

      return null;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void setPrefix(String prefix, int prefixKey) {
      this.prefix = prefix;
      this.prefixKey = prefixKey;
   }

   public CommandDispatcher<SharedSuggestionProvider> getDispatcher() {
      return this.dispatcher;
   }

   public SharedSuggestionProvider getSource() {
      return this.source;
   }
}
