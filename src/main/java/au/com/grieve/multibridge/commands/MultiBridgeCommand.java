package au.com.grieve.multibridge.commands;

import au.com.grieve.multibridge.MultiBridge;
import au.com.grieve.multibridge.objects.Instance;
import au.com.grieve.multibridge.objects.Template;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiBridgeCommand extends Command implements TabExecutor {
  private final MultiBridge plugin;

  public class Arguments {
    List<String> before;
    List<String> args;

    /** Initialise Arguments */
    Arguments(String[] args) {
      this.args = new ArrayList<>(Arrays.asList(args));
    }

    Arguments(List<String> before, List<String> args) {
      this.args = args;
      this.before = before;
    }

    Arguments shift() {
      return shift(1);
    }

    @SuppressWarnings("SameParameterValue")
    Arguments shift(int num) {
      List<String> newBefore = new ArrayList<>();
      if (before != null) {
        newBefore.addAll(before);
      }
      newBefore.addAll(args.subList(0, num));
      return new Arguments(newBefore, args.subList(num, args.size()));
    }
  }

  public MultiBridgeCommand(MultiBridge plugin) {
    super("multibridge", "multibridge.mb", "mb");
    this.plugin = plugin;
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("=== [ MultiBridge Help ] ===").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Main Menu of Multibrdge.").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb")
              .color(ChatColor.RED)
              .append(" template")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb")
              .color(ChatColor.RED)
              .append(" instance")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb")
              .color(ChatColor.RED)
              .append(" global")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb")
              .color(ChatColor.RED)
              .append(" reload")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("Add")
              .color(ChatColor.DARK_AQUA)
              .append(" help")
              .color(ChatColor.YELLOW)
              .append(" to the end of any command to get more help.")
              .color(ChatColor.DARK_AQUA)
              .create());
      return;
    }

    Arguments arguments = new Arguments(args);

    switch (arguments.args.get(0).toLowerCase()) {
      case "template":
        subcommandTemplate(sender, arguments.shift());
        break;
      case "instance":
        subcommandInstance(sender, arguments.shift());
        break;
      case "global":
        subcommandGlobal(sender, arguments.shift());
        break;
      case "reload":
        reload(sender, arguments.shift());
        break;
      default:
        sender.sendMessage(
            new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
        break;
    }
  }

  private void subcommandGlobal(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Global Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Manage global settings.").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb global")
              .color(ChatColor.RED)
              .append(" tag")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    switch (arguments.args.get(0).toLowerCase()) {
      case "tag":
        subcommandGlobalTag(sender, arguments.shift());
        return;
      default:
        sender.sendMessage(
            new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
        break;
    }
  }

  private void subcommandGlobalTag(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Global Tag Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Manage global tags. These are inherited by Instances.")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb global tag")
              .color(ChatColor.RED)
              .append(" set")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb global tag")
              .color(ChatColor.RED)
              .append(" get")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb global tag")
              .color(ChatColor.RED)
              .append(" list")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    switch (arguments.args.get(0).toLowerCase()) {
      case "set":
        globalTagSet(sender, arguments.shift());
        return;
      case "get":
        globalTagGet(sender, arguments.shift());
        return;
      case "list":
        globalTagList(sender, arguments.shift());
        return;
      default:
        sender.sendMessage(
            new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
        break;
    }
  }

  private void subcommandTemplate(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Template Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Manage Templates.").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb template")
              .color(ChatColor.RED)
              .append(" list")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb template")
              .color(ChatColor.RED)
              .append(" download")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    switch (arguments.args.get(0).toLowerCase()) {
      case "list":
        templateList(sender, arguments.shift());
        return;
      case "download":
        templateDownload(sender, arguments.shift());
        break;
      default:
        sender.sendMessage(
            new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
        break;
    }
  }

  private void subcommandInstance(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Instance Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Manage Server Instances.").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance")
              .color(ChatColor.RED)
              .append(" create")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance")
              .color(ChatColor.RED)
              .append(" start")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance")
              .color(ChatColor.RED)
              .append(" stop")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance")
              .color(ChatColor.RED)
              .append(" remove")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance")
              .color(ChatColor.RED)
              .append(" list")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance")
              .color(ChatColor.RED)
              .append(" info")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance")
              .color(ChatColor.RED)
              .append(" tag")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance")
              .color(ChatColor.RED)
              .append(" auto")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance")
              .color(ChatColor.RED)
              .append(" cmd")
              .color(ChatColor.YELLOW)
              .create());
      //            sender.sendMessage(new ComponentBuilder("/mb
      // instance").color(ChatColor.RED).append(" send").color(ChatColor.YELLOW).create());
      return;
    }

    switch (arguments.args.get(0).toLowerCase()) {
      case "create":
        instanceCreate(sender, arguments.shift());
        break;
      case "start":
        instanceStart(sender, arguments.shift());
        break;
      case "stop":
        instanceStop(sender, arguments.shift());
        break;
      case "remove":
        instanceRemove(sender, arguments.shift());
        break;
      case "list":
        instanceList(sender, arguments.shift());
        break;
      case "info":
        instanceInfo(sender, arguments.shift());
        break;
      case "tag":
        subcommandInstanceTag(sender, arguments.shift());
        break;
      case "auto":
        subcommandInstanceAuto(sender, arguments.shift());
        break;
      case "cmd":
        instanceCmd(sender, arguments.shift());
        break;
      default:
        sender.sendMessage(
            new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
        break;
    }
  }

  private void subcommandInstanceTag(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Instance Tag Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Manage Instance tags. Inherits from Global Tags and Template Tags.")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag")
              .color(ChatColor.RED)
              .append(" set")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag")
              .color(ChatColor.RED)
              .append(" get")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag")
              .color(ChatColor.RED)
              .append(" list")
              .color(ChatColor.YELLOW)
              .create());

      return;
    }

    switch (arguments.args.get(0).toLowerCase()) {
      case "set":
        instanceTagSet(sender, arguments.shift());
        return;
      case "get":
        instanceTagGet(sender, arguments.shift());
        return;
      case "list":
        instanceTagList(sender, arguments.shift());
        return;
      default:
        sender.sendMessage(
            new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
        break;
    }
  }

  private void subcommandInstanceAuto(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Instance Auto Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Manage Instance Auto Start/Stop settings.")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto")
              .color(ChatColor.RED)
              .append(" start")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto")
              .color(ChatColor.RED)
              .append(" stop")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto")
              .color(ChatColor.RED)
              .append(" enable")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto")
              .color(ChatColor.RED)
              .append(" disable")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    switch (arguments.args.get(0).toLowerCase()) {
      case "start":
        instanceAutoStart(sender, arguments.shift());
        return;
      case "stop":
        instanceAutoStop(sender, arguments.shift());
        return;
      case "enable":
        instanceAutoEnable(sender, arguments.shift());
        return;
      case "disable":
        instanceAutoDisable(sender, arguments.shift());
        return;
      default:
        sender.sendMessage(
            new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
        break;
    }
  }

  /** Enable Autos */
  private void instanceAutoEnable(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() < 1 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Instance Auto Enable Help ] ---")
              .color(ChatColor.AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("Enable auto start/stop.").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto enable")
              .color(ChatColor.RED)
              .append(" <instance>")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto enable")
              .color(ChatColor.RED)
              .append(" World1")
              .create());
      return;
    }

    Instance instance = plugin.getInstanceManager().getInstance(arguments.args.get(0));

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Instance does not exist").color(ChatColor.RED).create());
      return;
    }

    instance.setAuto(true);

    sender.sendMessage(new ComponentBuilder("Auto Enabled").color(ChatColor.RED).create());
  }

  /** Disable Autos */
  private void instanceAutoDisable(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() < 1 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Instance Auto Disable Help ] ---")
              .color(ChatColor.AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("Disable auto start/stop.").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto disable")
              .color(ChatColor.RED)
              .append(" <instance>")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto disable")
              .color(ChatColor.RED)
              .append(" World1")
              .create());
      return;
    }

    Instance instance = plugin.getInstanceManager().getInstance(arguments.args.get(0));

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Instance does not exist").color(ChatColor.RED).create());
      return;
    }

    instance.setAuto(false);

    sender.sendMessage(new ComponentBuilder("Auto Disabled").color(ChatColor.RED).create());
  }

  /** Set Auto Start */
  private void instanceAutoStart(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() < 2 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Instance Auto Start Help ] ---")
              .color(ChatColor.AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("Setup the Auto Start options for this instance.")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto start")
              .color(ChatColor.RED)
              .append(" <instance> <mode> [<delay>]")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Mode:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("  SERVER_START - ")
              .color(ChatColor.DARK_AQUA)
              .append("Start when Server Starts")
              .color(ChatColor.GREEN)
              .create());
      sender.sendMessage(
          new ComponentBuilder("  SERVER_JOIN - ")
              .color(ChatColor.DARK_AQUA)
              .append("Start when a player joins the server")
              .color(ChatColor.GREEN)
              .create());
      sender.sendMessage(
          new ComponentBuilder("  INSTANCE_JOIN - ")
              .color(ChatColor.DARK_AQUA)
              .append("Start when a player tries to connect to instance")
              .color(ChatColor.GREEN)
              .create());
      sender.sendMessage(
          new ComponentBuilder("  MANUAL - ")
              .color(ChatColor.DARK_AQUA)
              .append("Disabled")
              .color(ChatColor.GREEN)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto start")
              .color(ChatColor.RED)
              .append(" World1 SERVER_START 5")
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto start")
              .color(ChatColor.RED)
              .append(" World1 SERVER_JOIN 5")
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto start")
              .color(ChatColor.RED)
              .append(" World1 INSTANCE_JOIN")
              .create());
      return;
    }

    Instance instance = plugin.getInstanceManager().getInstance(arguments.args.get(0));

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Instance does not exist").color(ChatColor.RED).create());
      return;
    }

    try {
      instance.setStartMode(Instance.StartMode.valueOf(arguments.args.get(1).toUpperCase()));
    } catch (IllegalArgumentException e) {
      sender.sendMessage(new ComponentBuilder("Invalid Mode").color(ChatColor.RED).create());
      return;
    }

    if (arguments.args.size() > 2) {
      instance.setStartDelay(Math.max(0, Integer.valueOf(arguments.args.get(2))));
    }

    sender.sendMessage(new ComponentBuilder("Set Auto Start").color(ChatColor.RED).create());
  }

  /** Set Auto Start */
  private void instanceAutoStop(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() < 2 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Instance Auto Stop Help ] ---")
              .color(ChatColor.AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("Setup the Auto Stop options for this instance.")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto stop")
              .color(ChatColor.RED)
              .append(" <instance> <mode> [<delay>]")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Mode:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("  SERVER_EMPTY - ")
              .color(ChatColor.DARK_AQUA)
              .append("Start when Server is empty of players")
              .color(ChatColor.GREEN)
              .create());
      sender.sendMessage(
          new ComponentBuilder("  INSTANCE_EMPTY - ")
              .color(ChatColor.DARK_AQUA)
              .append("When the Instance is empty")
              .color(ChatColor.GREEN)
              .create());
      sender.sendMessage(
          new ComponentBuilder("  MANUAL - ")
              .color(ChatColor.DARK_AQUA)
              .append("Disabled")
              .color(ChatColor.GREEN)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto stop")
              .color(ChatColor.RED)
              .append(" World1 SERVER_EMPTY 5")
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto stop")
              .color(ChatColor.RED)
              .append(" World1 INSTANCE_EMPTY 5")
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance auto stop")
              .color(ChatColor.RED)
              .append(" World1 MANUAL")
              .create());
      return;
    }

    Instance instance = plugin.getInstanceManager().getInstance(arguments.args.get(0));

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Instance does not exist").color(ChatColor.RED).create());
      return;
    }

    try {
      instance.setStopMode(Instance.StopMode.valueOf(arguments.args.get(1).toUpperCase()));
    } catch (IllegalArgumentException e) {
      sender.sendMessage(new ComponentBuilder("Invalid Mode").color(ChatColor.RED).create());
      return;
    }

    if (arguments.args.size() > 2) {
      instance.setStopDelay(Math.max(0, Integer.valueOf(arguments.args.get(2))));
    }

    sender.sendMessage(new ComponentBuilder("Set Auto Stop").color(ChatColor.RED).create());
  }

  @Override
  public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
    return Collections.emptyList();
  }

  /** List all Templates available */
  private void templateList(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() > 0 && arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ List Templates Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("List installed templates").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb template list")
              .color(ChatColor.RED)
              .append(" [<page>]")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(new ComponentBuilder("/mb template list").color(ChatColor.RED).create());
      sender.sendMessage(
          new ComponentBuilder("/mb template list")
              .color(ChatColor.RED)
              .append(" 2")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    int page = 1;
    if (arguments.args.size() > 0) {
      try {
        page = Math.max(1, Integer.parseInt(arguments.args.get(0)));
      } catch (NumberFormatException e) {
        sender.sendMessage(
            new ComponentBuilder("Invalid Page Number").color(ChatColor.RED).create());
        return;
      }
    }

    sender.sendMessage(new ComponentBuilder("--- Templates ---").color(ChatColor.AQUA).create());

    plugin.getTemplateManager().getTemplates().keySet().stream()
        .sorted()
        .skip(10 * (page - 1))
        .limit(10)
        .forEach(
            s -> sender.sendMessage(new ComponentBuilder(s).color(ChatColor.DARK_AQUA).create()));
  }

  /** Template Download */
  private void templateDownload(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() < 2 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Download Template Help ] ---")
              .color(ChatColor.AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("Download a template zip file").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb template download")
              .color(ChatColor.RED)
              .append(" <name> <URL>")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb template download")
              .color(ChatColor.RED)
              .append(" example http://example.org/template.zip")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    String name = arguments.args.get(0);
    String url = arguments.args.get(1);

    sender.sendMessage(
        new ComponentBuilder("Downloading Template: ")
            .color(ChatColor.GREEN)
            .append(url)
            .color(ChatColor.YELLOW)
            .append(" -> ")
            .color(ChatColor.DARK_AQUA)
            .append(name)
            .color(ChatColor.YELLOW)
            .create());

    plugin
        .getProxy()
        .getScheduler()
        .runAsync(
            plugin,
            () -> {
              Template template;
              try {
                template = plugin.getTemplateManager().downloadTemplate(name, new URL(url));
              } catch (IOException e) {
                sender.sendMessage(
                    new ComponentBuilder("Failed to download template: ")
                        .color(ChatColor.RED)
                        .append(e.getMessage())
                        .color(ChatColor.YELLOW)
                        .create());
                return;
              }

              if (template == null) {
                sender.sendMessage(
                    new ComponentBuilder("Invalid Template: ")
                        .color(ChatColor.RED)
                        .append(name)
                        .color(ChatColor.YELLOW)
                        .create());
                return;
              }

              // Success
              sender.sendMessage(
                  new ComponentBuilder("Downloaded Template: ")
                      .color(ChatColor.GREEN)
                      .append(name)
                      .color(ChatColor.YELLOW)
                      .create());
            });
  }

  /** Convert an Instance State to a ComponentBuilder Message */
  private BaseComponent[] instanceStateToMessage(Instance.State state) {
    switch (state) {
      case STARTING:
        return new ComponentBuilder("STARTING").color(ChatColor.LIGHT_PURPLE).create();
      case STARTED:
        return new ComponentBuilder("STARTED").color(ChatColor.GREEN).create();
      case STOPPING:
        return new ComponentBuilder("STOPPING").color(ChatColor.LIGHT_PURPLE).create();
      case STOPPED:
        return new ComponentBuilder("STOPPED").color(ChatColor.GRAY).create();
      case BUSY:
        return new ComponentBuilder("BUSY").color(ChatColor.YELLOW).create();
    }

    return new ComponentBuilder("UNKNOWN").color(ChatColor.RED).create();
  }

  /** List all Instances available */
  private void instanceList(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() > 0 && arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ List Instances Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("List available instances").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance list")
              .color(ChatColor.RED)
              .append(" [<page>]")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(new ComponentBuilder("/mb instance list").color(ChatColor.RED).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance list")
              .color(ChatColor.RED)
              .append(" 2")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    int page = 1;
    if (arguments.args.size() > 0) {
      try {
        page = Math.max(1, Integer.parseInt(arguments.args.get(0)));
      } catch (NumberFormatException e) {
        sender.sendMessage(
            new ComponentBuilder("Invalid Page Number").color(ChatColor.RED).create());
        return;
      }
    }

    sender.sendMessage(new ComponentBuilder("--- Instances ---").color(ChatColor.AQUA).create());
    Map<String, Instance> instances = plugin.getInstanceManager().getInstances();
    if (instances.size() <= (page - 1) * 10) {
      sender.sendMessage(new ComponentBuilder("No instances found").color(ChatColor.RED).create());
      return;
    }

    instances.values().stream()
        .skip(10 * (page - 1))
        .limit(10)
        .forEach(
            s ->
                sender.sendMessage(
                    new ComponentBuilder(" - [")
                        .color(ChatColor.DARK_GRAY)
                        .append(instanceStateToMessage(s.getState()))
                        .append("]")
                        .color(ChatColor.DARK_GRAY)
                        .append(" " + s.getName())
                        .color(ChatColor.DARK_AQUA)
                        .create()));
  }

  /** Create a new Server Instance */
  private void instanceCreate(CommandSender sender, Arguments arguments) {
    // Help
    if (arguments.args.size() < 2 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Create Instance Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Create a new Instance from a Template")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance create")
              .color(ChatColor.RED)
              .append(" <instance_name> <template_name>")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance create")
              .color(ChatColor.RED)
              .append(" World1 vanilla.1.12")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    // Arguments
    final String instanceName = arguments.args.get(0);
    final String templateName = arguments.args.get(1);

    // Async It
    plugin
        .getProxy()
        .getScheduler()
        .runAsync(
            plugin,
            () -> {
              // Create a new Instance
              Instance instance;
              try {
                instance = plugin.getInstanceManager().create(templateName, instanceName);
              } catch (IOException e) {
                sender.sendMessage(
                    new ComponentBuilder("Unable to create new Instance: ")
                        .color(ChatColor.RED)
                        .append(e.getMessage())
                        .color(ChatColor.YELLOW)
                        .create());
                return;
              }

              // Null instance means it was cancelled silently
              if (instance == null) {
                return;
              }

              // Success
              sender.sendMessage(
                  new ComponentBuilder("New Instance Created: ")
                      .color(ChatColor.GREEN)
                      .append(instanceName)
                      .color(ChatColor.YELLOW)
                      .create());
            });
  }

  /** Remove an Instance */
  private void instanceRemove(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Remove Instance Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Remove an existing instance. This will delete the physical files.")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance remove")
              .color(ChatColor.RED)
              .append(" <instance_name>")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance remove")
              .color(ChatColor.RED)
              .append(" World1")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    // Arguments
    final String instanceName = arguments.args.get(0);

    // Schedule the task
    plugin
        .getProxy()
        .getScheduler()
        .runAsync(
            plugin,
            () -> {
              Instance instance = plugin.getInstanceManager().getInstance(instanceName);

              if (instance == null) {
                sender.sendMessage(
                    new ComponentBuilder("Instance does not exist").color(ChatColor.RED).create());
                return;
              }

              if (instance.isRunning()) {
                sender.sendMessage(
                    new ComponentBuilder("Instance is currently running")
                        .color(ChatColor.RED)
                        .create());
                return;
              }

              // Remove it
              try {
                plugin.getInstanceManager().remove(instance);
              } catch (IOException e) {
                sender.sendMessage(
                    new ComponentBuilder("Unable to remove Instance: " + e.getMessage())
                        .color(ChatColor.RED)
                        .create());
                return;
              }

              // Success
              sender.sendMessage(
                  new ComponentBuilder("Instance Removed").color(ChatColor.GREEN).create());
            });
  }

  /** Start existing Server Instance */
  private void instanceStart(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Start Instance Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Manually start an Instance").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance start")
              .color(ChatColor.RED)
              .append(" <instance_name>")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance start")
              .color(ChatColor.RED)
              .append(" World1")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    // Arguments
    final String instanceName = arguments.args.get(0);
    Instance instance = plugin.getInstanceManager().getInstance(instanceName);

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
      return;
    }

    if (instance.isRunning()) {
      sender.sendMessage(
          new ComponentBuilder("Instance is already running").color(ChatColor.RED).create());
      return;
    }

    // Success
    sender.sendMessage(new ComponentBuilder("Instance Starting").color(ChatColor.GREEN).create());

    // Schedule the task
    plugin
        .getProxy()
        .getScheduler()
        .runAsync(
            plugin,
            () -> {
              // Start Instance
              try {
                instance.start();
                sender.sendMessage(
                    new ComponentBuilder("Instance Started").color(ChatColor.GREEN).create());
              } catch (IOException e) {
                sender.sendMessage(
                    new ComponentBuilder("Unable to start Instance: ")
                        .color(ChatColor.RED)
                        .append(e.getMessage())
                        .color(ChatColor.YELLOW)
                        .create());
              }
            });
  }

  /** Send cmmand to running Server Instance */
  private void instanceCmd(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Instance CMD Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Send command to instance console")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance cmd")
              .color(ChatColor.RED)
              .append(" <instance_name> <command>...")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance cmd")
              .color(ChatColor.RED)
              .append(" World1 op Bundie")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    // Arguments
    final String instanceName = arguments.args.get(0);
    Instance instance = plugin.getInstanceManager().getInstance(instanceName);

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
      return;
    }

    if (!instance.isRunning()) {
      sender.sendMessage(
          new ComponentBuilder("Instance is not running").color(ChatColor.RED).create());
      return;
    }

    // Schedule the task
    plugin
        .getProxy()
        .getScheduler()
        .runAsync(
            plugin,
            () -> {

              // Start Instance
              try {
                instance.sendCommand(String.join(" ", arguments.shift().args));

                // Success
                sender.sendMessage(
                    new ComponentBuilder("Sending command").color(ChatColor.GREEN).create());
              } catch (IOException e) {
                sender.sendMessage(
                    new ComponentBuilder("Unable to send command: ")
                        .color(ChatColor.RED)
                        .append(e.getMessage())
                        .color(ChatColor.YELLOW)
                        .create());
              }
            });
  }

  /** Stop existing Server Instance */
  private void instanceStop(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Stop Instance Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Manually stop an Instance").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance stop")
              .color(ChatColor.RED)
              .append(" <instance_name>")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance stop")
              .color(ChatColor.RED)
              .append(" World1")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    // Arguments
    final String instanceName = arguments.args.get(0);
    Instance instance = plugin.getInstanceManager().getInstance(instanceName);

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
      return;
    }

    if (!instance.isRunning()) {
      sender.sendMessage(
          new ComponentBuilder("Instance not running").color(ChatColor.RED).create());
      return;
    }

    sender.sendMessage(new ComponentBuilder("Stopping Instance").color(ChatColor.GREEN).create());

    // Schedule the task
    plugin
        .getProxy()
        .getScheduler()
        .runAsync(
            plugin,
            () -> {
              // Stop Instance
              try {
                instance.stop();
                sender.sendMessage(
                    new ComponentBuilder("Instance Stopped").color(ChatColor.GREEN).create());
              } catch (IOException e) {
                sender.sendMessage(
                    new ComponentBuilder("Failed to Stop Instance: ")
                        .color(ChatColor.GREEN)
                        .append(e.getMessage())
                        .color(ChatColor.YELLOW)
                        .create());
              }
            });
  }

  /** Get Info on an Instance */
  private void instanceInfo(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Instance Info Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Get information about an instance")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance info")
              .color(ChatColor.RED)
              .append(" <instance_name> [<page>]")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance info")
              .color(ChatColor.RED)
              .append(" World1")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance info")
              .color(ChatColor.RED)
              .append(" World1 2")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    // Arguments
    final String instanceName = arguments.args.get(0);
    Instance instance = plugin.getInstanceManager().getInstance(instanceName);

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
      return;
    }

    sender.sendMessage(new ComponentBuilder("--- General Info ---").color(ChatColor.AQUA).create());
    sender.sendMessage(
        new ComponentBuilder("Name: ")
            .color(ChatColor.DARK_AQUA)
            .append(instance.getName())
            .color(ChatColor.GREEN)
            .create());

    sender.sendMessage(
        new ComponentBuilder("State: ")
            .color(ChatColor.DARK_AQUA)
            .append("[")
            .color(ChatColor.DARK_GRAY)
            .append(instanceStateToMessage(instance.getState()))
            .append("]")
            .color(ChatColor.DARK_GRAY)
            .create());

    sender.sendMessage(
        new ComponentBuilder("Auto: ")
            .color(ChatColor.DARK_AQUA)
            .append(instance.getAuto() ? "ENABLED" : "DISABLED")
            .color(instance.getAuto() ? ChatColor.YELLOW : ChatColor.DARK_GRAY)
            .create());

    sender.sendMessage(new ComponentBuilder("Start:").color(ChatColor.DARK_AQUA).create());
    sender.sendMessage(
        new ComponentBuilder("  Mode: ")
            .color(ChatColor.DARK_AQUA)
            .append(instance.getStartMode().toString())
            .color(ChatColor.GREEN)
            .create());
    sender.sendMessage(
        new ComponentBuilder("  Delay: ")
            .color(ChatColor.DARK_AQUA)
            .append(String.valueOf(instance.getStartDelay()))
            .color(ChatColor.GREEN)
            .create());

    sender.sendMessage(new ComponentBuilder("Stop:").color(ChatColor.DARK_AQUA).create());
    sender.sendMessage(
        new ComponentBuilder("  Mode: ")
            .color(ChatColor.DARK_AQUA)
            .append(instance.getStopMode().toString())
            .color(ChatColor.GREEN)
            .create());
    sender.sendMessage(
        new ComponentBuilder("  Delay: ")
            .color(ChatColor.DARK_AQUA)
            .append(String.valueOf(instance.getStopDelay()))
            .color(ChatColor.GREEN)
            .create());

    sender.sendMessage(new ComponentBuilder("").create());

    sender.sendMessage(
        new ComponentBuilder("--- Required Tags ---").color(ChatColor.AQUA).create());
    Map<String, String> tags = instance.getTags();
    List<String> requiredTags = instance.getRequiredTags();

    requiredTags.stream()
        .sorted()
        .forEach(
            s -> {
              ComponentBuilder m = new ComponentBuilder(s + ": ").color(ChatColor.DARK_AQUA);
              if (tags.containsKey(s)) {
                m.append(tags.get(s)).color(ChatColor.GREEN);
              } else {
                m.append("MISSING!").color(ChatColor.RED);
              }
              sender.sendMessage(m.create());
            });
  }

  /** Set Tags on an instance */
  private void instanceTagSet(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Set Instance Tag Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Set a tag on an Instance. Leave value blank to clear.")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag set")
              .color(ChatColor.RED)
              .append(" <instance_name> <tag_name> [<value>]")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag set")
              .color(ChatColor.RED)
              .append(" World1 EULA true")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag set")
              .color(ChatColor.RED)
              .append(" World1 EULA")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    // Arguments
    final String instanceName = arguments.args.get(0);
    Instance instance = plugin.getInstanceManager().getInstance(instanceName);

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
      return;
    }

    String tag = arguments.args.get(1);
    if (arguments.args.size() > 2) {
      instance.setTag(tag, String.join(" ", arguments.args.subList(2, arguments.args.size())));
      sender.sendMessage(
          new ComponentBuilder("Set ")
              .color(ChatColor.GREEN)
              .append(tag.toUpperCase())
              .color(ChatColor.YELLOW)
              .append(" = ")
              .color(ChatColor.GREEN)
              .append(arguments.args.get(2))
              .color(ChatColor.YELLOW)
              .create());
    } else {
      instance.clearTag(tag);
      sender.sendMessage(
          new ComponentBuilder("Cleared ")
              .color(ChatColor.GREEN)
              .append(tag.toUpperCase())
              .color(ChatColor.YELLOW)
              .create());
    }
  }

  /** Get Tag on an instance */
  private void instanceTagGet(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() < 2 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Get Instance Tag Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Get a tag on an Instance.").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag get")
              .color(ChatColor.RED)
              .append(" <instance_name>")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag get")
              .color(ChatColor.RED)
              .append(" World1 EULA")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    // Arguments
    final String instanceName = arguments.args.get(0);
    Instance instance = plugin.getInstanceManager().getInstance(instanceName);

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
      return;
    }

    String tag = arguments.args.get(1);

    sender.sendMessage(
        new ComponentBuilder("Instance: ")
            .color(ChatColor.GREEN)
            .append(tag.toUpperCase())
            .color(ChatColor.YELLOW)
            .append(" = ")
            .color(ChatColor.GREEN)
            .append(instance.getLocalTag(tag))
            .color(ChatColor.YELLOW)
            .create());
  }

  /** List Instance Tags */
  private void instanceTagList(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() < 1 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ List Instance Tags Help ] ---")
              .color(ChatColor.AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("List instance tags").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag list")
              .color(ChatColor.RED)
              .append(" <instance_name> [<page>]")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag list")
              .color(ChatColor.RED)
              .append(" World1")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb instance tag list")
              .color(ChatColor.RED)
              .append(" World1 2")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    final String instanceName = arguments.args.get(0);
    Instance instance = plugin.getInstanceManager().getInstance(instanceName);

    if (instance == null) {
      sender.sendMessage(
          new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
      return;
    }

    int page = 1;
    if (arguments.args.size() > 1) {
      try {
        page = Math.max(1, Integer.parseInt(arguments.args.get(1)));
      } catch (NumberFormatException e) {
        sender.sendMessage(
            new ComponentBuilder("Invalid Page Number").color(ChatColor.RED).create());
        return;
      }
    }

    sender.sendMessage(
        new ComponentBuilder("--- Instance Tags ---").color(ChatColor.AQUA).create());

    Set<String> localTags = instance.getLocalTags().keySet();

    instance.getTags().entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .skip(10 * (page - 1))
        .limit(10)
        .forEach(
            s -> {
              ComponentBuilder msg =
                  new ComponentBuilder(s.getKey() + ": ")
                      .color(ChatColor.DARK_AQUA)
                      .append(s.getValue())
                      .color(ChatColor.GREEN);

              if (!localTags.contains(s.getKey())) {
                msg.append(" (inherited)").color(ChatColor.LIGHT_PURPLE);
              }

              sender.sendMessage(msg.create());
            });
  }

  /** Set Global Tags */
  private void globalTagSet(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Set Global Tag Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Set a tag globally. Leave value blank to clear.")
              .color(ChatColor.DARK_AQUA)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb global tag set")
              .color(ChatColor.RED)
              .append(" <tag_name> [<value>]")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb global tag set")
              .color(ChatColor.RED)
              .append(" EULA true")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(
          new ComponentBuilder("/mb global tag set")
              .color(ChatColor.RED)
              .append(" EULA")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    // Arguments
    String tag = arguments.args.get(0);
    if (arguments.args.size() > 1) {
      plugin
          .getGlobalManager()
          .setTag(tag, String.join(" ", arguments.args.subList(1, arguments.args.size())));
      sender.sendMessage(
          new ComponentBuilder("Set ")
              .color(ChatColor.GREEN)
              .append(tag.toUpperCase())
              .color(ChatColor.YELLOW)
              .append(" = ")
              .color(ChatColor.GREEN)
              .append(arguments.args.get(1))
              .color(ChatColor.YELLOW)
              .create());
    } else {
      plugin.getGlobalManager().clearTag(tag);
      sender.sendMessage(
          new ComponentBuilder("Cleared ")
              .color(ChatColor.GREEN)
              .append(tag.toUpperCase())
              .color(ChatColor.YELLOW)
              .create());
    }
  }

  /** Get Global Tags */
  private void globalTagGet(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ Get Global Tag Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("Get a global tag").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb global tag get")
              .color(ChatColor.RED)
              .append(" <tag_name>")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(
          new ComponentBuilder("/mb global tag get")
              .color(ChatColor.RED)
              .append(" EULA")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    // Arguments
    String tag = arguments.args.get(0);
    sender.sendMessage(
        new ComponentBuilder("Global: ")
            .color(ChatColor.GREEN)
            .append(tag.toUpperCase())
            .color(ChatColor.YELLOW)
            .append(" = ")
            .color(ChatColor.GREEN)
            .append(plugin.getGlobalManager().getTag(tag))
            .color(ChatColor.YELLOW)
            .create());
  }

  /** List Global Tags */
  private void globalTagList(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() > 0 && arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
          new ComponentBuilder("--- [ List Global Tags Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("List global tags").color(ChatColor.DARK_AQUA).create());
      sender.sendMessage(
          new ComponentBuilder("/mb global tag list")
              .color(ChatColor.RED)
              .append(" [<page>]")
              .color(ChatColor.YELLOW)
              .create());
      sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
      sender.sendMessage(new ComponentBuilder("/mb global tag list").color(ChatColor.RED).create());
      sender.sendMessage(
          new ComponentBuilder("/mb global tag list")
              .color(ChatColor.RED)
              .append(" 2")
              .color(ChatColor.YELLOW)
              .create());
      return;
    }

    int page = 1;
    if (arguments.args.size() > 0) {
      try {
        page = Math.max(1, Integer.parseInt(arguments.args.get(0)));
      } catch (NumberFormatException e) {
        sender.sendMessage(
            new ComponentBuilder("Invalid Page Number").color(ChatColor.RED).create());
        return;
      }
    }

    sender.sendMessage(new ComponentBuilder("--- Global Tags ---").color(ChatColor.AQUA).create());

    plugin.getGlobalManager().getTags().entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .skip((page - 1) * 10)
            .limit(10)
            .forEach(
                    s ->
                            sender.sendMessage(
                                    new ComponentBuilder(s.getKey() + ": ")
                                            .color(ChatColor.DARK_AQUA)
                                            .append(s.getValue())
                                            .color(ChatColor.GREEN)
                                            .create()));
  }

  /**
   * Reload Config
   */
  private void reload(CommandSender sender, Arguments arguments) {
    if (arguments.args.size() > 0 && arguments.args.get(0).equalsIgnoreCase("help")) {
      sender.sendMessage(
              new ComponentBuilder("--- [ Reload Help ] ---").color(ChatColor.AQUA).create());
      sender.sendMessage(
              new ComponentBuilder("Reload configuration").color(ChatColor.DARK_AQUA).create());
      return;
    }

    plugin.getInstanceManager().reloadConfig();

    sender.sendMessage(new ComponentBuilder("Reloaded Config").color(ChatColor.YELLOW).create());
  }
}
