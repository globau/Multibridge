package au.com.grieve.multibridge.commands;

import au.com.grieve.multibridge.MultiBridge;
import au.com.grieve.multibridge.instance.Instance;
import au.com.grieve.multibridge.instance.InstanceManager;
import au.com.grieve.multibridge.template.TemplateManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.omg.PortableServer.POAManagerPackage.State;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class MultiBridgeCommand extends Command implements TabExecutor {
    final private MultiBridge plugin;

    public class Arguments {
        List<String> before;
        List<String> args;

        /**
         * Initialise Arguments
         */
        Arguments(String[] args) {
            this.args = new ArrayList<>(Arrays.asList(args));
        }

        Arguments(List<String> before, List<String> args) {
            this.args = args;
            this.before = before;
        }

        Arguments shift(int num) {
            List<String> newBefore = new ArrayList<>();
            if (before != null) {
                newBefore.addAll(before);
            }
            newBefore.addAll(args.subList(0, num));
            return new Arguments(
                    newBefore,
                    args.subList(num, args.size())
            );
        }
    }


    public MultiBridgeCommand(MultiBridge plugin) {
        super("multibridge", "multibridge.mb", "mb");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("=== [ MultiBridge Help ] ===").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb").color(ChatColor.RED).append(" template").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("/mb").color(ChatColor.RED).append(" instance").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Add").color(ChatColor.DARK_AQUA)
                    .append(" help").color(ChatColor.YELLOW)
                    .append(" to the end of any command to get more help.").color(ChatColor.DARK_AQUA)
                    .create());
            return;
        }

        Arguments arguments = new Arguments(args);

        switch(arguments.args.get(0).toLowerCase()) {
            case "template":
                subcommandTemplate(sender, arguments.shift(1));
                break;
            case "instance":
                subcommandInstance(sender, arguments.shift(1));
                break;
            default:
                sender.sendMessage(new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
                break;
        }
    }

    private void subcommandTemplate(CommandSender sender, Arguments arguments) {
        if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ Template Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb template").color(ChatColor.RED).append(" list").color(ChatColor.YELLOW).create());
            return;
        }

        switch(arguments.args.get(0).toLowerCase()) {
            case "list":
                listTemplates(sender, arguments.shift(1));
                return;
            default:
                sender.sendMessage(new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
                break;
        }

    }

    private void subcommandInstance(CommandSender sender, Arguments arguments) {
        if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ Instance Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb instance").color(ChatColor.RED).append(" create").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("/mb instance").color(ChatColor.RED).append(" start").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("/mb instance").color(ChatColor.RED).append(" stop").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("/mb instance").color(ChatColor.RED).append(" remove").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("/mb instance").color(ChatColor.RED).append(" list").color(ChatColor.YELLOW).create());
            return;
        }

        switch(arguments.args.get(0).toLowerCase()) {
            case "create":
                createInstance(sender, arguments.shift(1));
                break;
            case "start":
                startInstance(sender, arguments.shift(1));
                break;
            case "stop":
                stopInstance(sender, arguments.shift(1));
                break;
            case "remove":
                removeInstance(sender, arguments.shift(1));
                break;
            case "list":
                listInstances(sender, arguments.shift(1));
                break;
            default:
                sender.sendMessage(new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
                break;
        }

    }


    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    /**
     * List all Templates available
     *
     */
    private void listTemplates(CommandSender sender, Arguments arguments) {
        if (arguments.args.size() > 0 && arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ List Templates Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("List installed templates").color(ChatColor.DARK_AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb template list").color(ChatColor.RED).append(" [<page>]").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
            sender.sendMessage(new ComponentBuilder("/mb template list").color(ChatColor.RED).create());
            sender.sendMessage(new ComponentBuilder("/mb template list").color(ChatColor.RED).append(" 2").color(ChatColor.YELLOW).create());
            return;
        }

        int page = 1;
        if (arguments.args.size() > 0) {
            try {
                page = Math.max(1,Integer.parseInt(arguments.args.get(0)));
            } catch (NumberFormatException e) {
                sender.sendMessage(new ComponentBuilder("Invalid Page Number").color(ChatColor.RED).create());
                return;
            }
        }

        sender.sendMessage(new ComponentBuilder("--- Templates ---").color(ChatColor.AQUA).create());

        Map<String, TemplateManager.Template> templates = plugin.getTemplateManager().getTemplates();
        if (templates.size() <= (page-1)*20) {
            sender.sendMessage(new ComponentBuilder("No templates found").color(ChatColor.GREEN).create());
            return;
        }

        templates.keySet().stream()
                .skip(20*(page-1))
                .forEach(s -> sender.sendMessage(new ComponentBuilder(s).color(ChatColor.DARK_AQUA).create()));
    }

    /**
     * List all Instances available
     *
     */
    private void listInstances(CommandSender sender,Arguments arguments) {
        if (arguments.args.size() > 0 && arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ List Instances Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("List available instances").color(ChatColor.DARK_AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb instance list").color(ChatColor.RED).append(" [<page>]").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
            sender.sendMessage(new ComponentBuilder("/mb instance list").color(ChatColor.RED).create());
            sender.sendMessage(new ComponentBuilder("/mb instance list").color(ChatColor.RED).append(" 2").color(ChatColor.YELLOW).create());
            return;
        }

        int page = 1;
        if (arguments.args.size() > 0) {
            try {
                page = Math.max(1,Integer.parseInt(arguments.args.get(0)));
            } catch (NumberFormatException e) {
                sender.sendMessage(new ComponentBuilder("Invalid Page Number").color(ChatColor.RED).create());
                return;
            }
        }

        sender.sendMessage(new ComponentBuilder("--- Instances ---").color(ChatColor.AQUA).create());
        Map<String, Instance> instances = plugin.getInstanceManager().getInstances();
        if (instances.size() <= (page-1)*20) {
            sender.sendMessage(new ComponentBuilder("No instances found").color(ChatColor.RED).create());
            return;
        }

        instances.values().stream()
                .skip(20*(page-1))
                .forEach(s -> {
                            ComponentBuilder msg = new ComponentBuilder(" - [").color(ChatColor.DARK_GRAY);

                            Instance.State state = s.getState();
                            switch(state) {
                                case STARTED:
                                    msg.append("ACTIVE").color(ChatColor.GREEN);
                                    break;
                                case STOPPED:
                                    msg.append("STOPPED").color(ChatColor.GRAY);
                                    break;
                                case WAITING:
                                    msg.append("WAITING").color(ChatColor.YELLOW);
                                    break;
                                default:
                                    msg.append("UNKNOWN").color(ChatColor.RED);
                            }

                            msg.append("]").color(ChatColor.DARK_GRAY);
                            msg.append(" " + s.getName()).color(ChatColor.DARK_AQUA);

                            sender.sendMessage(msg.create());
                });

    }

    /**
     * Create a new Server Instance
     *
     */
    private void createInstance(CommandSender sender, Arguments arguments) {
        // Help
        if (arguments.args.size() < 2 || arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ Create Instance Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("Create a new Instance from a Template").color(ChatColor.DARK_AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb instance create").color(ChatColor.RED).append(" <instance_name> <template_name> [<tag>:<value> ...]").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
            sender.sendMessage(new ComponentBuilder("/mb instance create").color(ChatColor.RED).append(" World1 vanilla.1.12 EULA:true").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("/mb instance create").color(ChatColor.RED).append(" World2 papermc.1.13 MC_GAMEMODE:0 EULA:true").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        final String instanceName = arguments.args.get(0);
        final String templateName = arguments.args.get(1);


        // Async It
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            // Create a new Instance
            Instance instance = plugin.getInstanceManager().create(templateName, instanceName);

            // @TODO: Make use of exceptions rather than returning null
            if (instance == null) {
                sender.sendMessage(new ComponentBuilder("Unable to create new Instance").color(ChatColor.RED).create());
                return;
            }

            // Success
            sender.sendMessage(new ComponentBuilder("New Instance Created: ").color(ChatColor.GREEN).append(instanceName).color(ChatColor.YELLOW).create());
        });
    }

    /**
     * Remove an Instance
     *
     */
    private void removeInstance(CommandSender sender, Arguments arguments) {
        if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ Remove Instance Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("Remove an existing instance. This will delete the physical files.").color(ChatColor.DARK_AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb instance remove").color(ChatColor.RED).append(" <instance_name>").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
            sender.sendMessage(new ComponentBuilder("/mb instance remove").color(ChatColor.RED).append(" World1").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        final String instanceName = arguments.args.get(0);

        // Schedule the task
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            Instance instance = plugin.getInstanceManager().getInstance(instanceName);

            if (instance == null) {
                sender.sendMessage(new ComponentBuilder("Instance does not exist").color(ChatColor.RED).create());
                return;
            }

            if (instance.isRunning()) {
                sender.sendMessage(new ComponentBuilder("Instance is currently running").color(ChatColor.RED).create());
                return;
            }

            // Remove it
            try {
                plugin.getInstanceManager().remove(instance);
            } catch (IOException e) {
                sender.sendMessage(new ComponentBuilder("Unable to remove Instance: " + e.getMessage()).color(ChatColor.RED).create());
                return;
            }

            // Success
            sender.sendMessage(new ComponentBuilder("Instance Removed").color(ChatColor.GREEN).create());
        });
    }

    /**
     * Start existing Server Instance
     *
     */
    private void startInstance(CommandSender sender, Arguments arguments) {
        if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ Start Instance Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("Manually start an Instance").color(ChatColor.DARK_AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb instance start").color(ChatColor.RED).append(" <instance_name>").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
            sender.sendMessage(new ComponentBuilder("/mb instance start").color(ChatColor.RED).append(" World1").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        final String instanceName = arguments.args.get(0);
        Instance instance = plugin.getInstanceManager().getInstance(instanceName);

        if (instance == null) {
            sender.sendMessage(new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
            return;
        }

        if (instance.isRunning()) {
            sender.sendMessage(new ComponentBuilder("Instance is already running").color(ChatColor.RED).create());
            return;
        }

        // Schedule the task
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            // Start Instance
            instance.start();

            // Success
            sender.sendMessage(new ComponentBuilder("Instance Starting").color(ChatColor.GREEN).create());
        });

    }

    /**
     * Stop existing Server Instance
     *
     */
    private void stopInstance(CommandSender sender, Arguments arguments) {
        if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ Stop Instance Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("Manually stop an Instance").color(ChatColor.DARK_AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb instance stop").color(ChatColor.RED).append(" <instance_name>").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
            sender.sendMessage(new ComponentBuilder("/mb instance stop").color(ChatColor.RED).append(" World1").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        final String instanceName = arguments.args.get(0);
        Instance instance = plugin.getInstanceManager().getInstance(instanceName);

        if (instance == null) {
            sender.sendMessage(new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
            return;
        }

        if (!instance.isRunning()) {
            sender.sendMessage(new ComponentBuilder("Instance not running").color(ChatColor.RED).create());
            return;
        }

        // Schedule the task
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            // Start Instance
            instance.stop();

            // Success
            sender.sendMessage(new ComponentBuilder("Instance Stopping").color(ChatColor.GREEN).create());
        });

    }

}
