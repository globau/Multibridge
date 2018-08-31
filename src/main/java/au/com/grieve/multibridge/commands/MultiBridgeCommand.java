package au.com.grieve.multibridge.commands;

import au.com.grieve.multibridge.MultiBridge;
import au.com.grieve.multibridge.instance.Instance;
import au.com.grieve.multibridge.template.TemplateManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.IOException;
import java.util.*;

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
            sender.sendMessage(new ComponentBuilder("/mb").color(ChatColor.RED).append(" global").color(ChatColor.YELLOW).create());
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
            case "global":
                subcommandGlobal(sender, arguments.shift(1));
                break;
            default:
                sender.sendMessage(new ComponentBuilder("Unknown Command").color(ChatColor.DARK_RED).create());
                break;
        }
    }

    private void subcommandGlobal(CommandSender sender, Arguments arguments) {
        if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ Global Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb global").color(ChatColor.RED).append(" set").color(ChatColor.YELLOW).create());
            return;
        }

        switch(arguments.args.get(0).toLowerCase()) {
            case "set":
                setGlobal(sender, arguments.shift(1));
                return;
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
            sender.sendMessage(new ComponentBuilder("/mb instance").color(ChatColor.RED).append(" info").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("/mb instance").color(ChatColor.RED).append(" set").color(ChatColor.YELLOW).create());
//            sender.sendMessage(new ComponentBuilder("/mb instance").color(ChatColor.RED).append(" send").color(ChatColor.YELLOW).create());
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
            case "info":
                infoInstance(sender, arguments.shift(1));
                break;
            case "set":
                setInstance(sender, arguments.shift(1));
                break;
//            case "send":
//                sendInstance(sender, arguments.shift(1));
//                break;
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
                                case ERROR:
                                    msg.append("ERROR").color(ChatColor.RED);
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
            sender.sendMessage(new ComponentBuilder("/mb instance create").color(ChatColor.RED).append(" <instance_name> <template_name>").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
            sender.sendMessage(new ComponentBuilder("/mb instance create").color(ChatColor.RED).append(" World1 vanilla.1.12").color(ChatColor.YELLOW).create());
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
            try {
                instance.start();
            } catch (RuntimeException e) {
                sender.sendMessage(new ComponentBuilder("Unable to start Instance: ").color(ChatColor.RED)
                        .append(e.getMessage()).color(ChatColor.YELLOW).create());
                return;
            }

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

    /**
     * Get Info on an Instance
     *
     */
    private void infoInstance(CommandSender sender, Arguments arguments) {
        if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ Instance Info Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("Get information about an instance").color(ChatColor.DARK_AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb instance info").color(ChatColor.RED).append(" <instance_name> [<page>]").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
            sender.sendMessage(new ComponentBuilder("/mb instance info").color(ChatColor.RED).append(" World1").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("/mb instance info").color(ChatColor.RED).append(" World1 2").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        final String instanceName = arguments.args.get(0);
        Instance instance = plugin.getInstanceManager().getInstance(instanceName);

        if (instance == null) {
            sender.sendMessage(new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
            return;
        }

        sender.sendMessage(new ComponentBuilder("--- General Info ---").color(ChatColor.AQUA).create());
        sender.sendMessage(new ComponentBuilder("Name: ").color(ChatColor.DARK_AQUA)
                .append(instance.getName()).color(ChatColor.GREEN).create());

        ComponentBuilder msg = new ComponentBuilder("State: ").color(ChatColor.DARK_AQUA)
                .append("[").color(ChatColor.DARK_GRAY);

        switch(instance.getState()) {
            case STARTED:
                msg.append("ACTIVE").color(ChatColor.GREEN);
                break;
            case STOPPED:
                msg.append("STOPPED").color(ChatColor.GRAY);
                break;
            case WAITING:
                msg.append("WAITING").color(ChatColor.YELLOW);
                break;
            case ERROR:
                msg.append("ERROR").color(ChatColor.RED);
                break;
            default:
                msg.append("UNKNOWN").color(ChatColor.RED);
        }
        msg.append("]").color(ChatColor.DARK_GRAY);
        sender.sendMessage(msg.create());

        sender.sendMessage("");

        sender.sendMessage(new ComponentBuilder("--- Required Tags ---").color(ChatColor.AQUA).create());
        Map<String, String> tags = instance.getTags();
        List<String> requiredTags = instance.getRequiredTags();

        requiredTags.stream()
                .sorted()
                .forEach( s -> {
                    ComponentBuilder m = new ComponentBuilder(s + ": ").color(ChatColor.DARK_AQUA);
                    if (tags.containsKey(s)) {
                        m.append(tags.get(s)).color(ChatColor.GREEN);
                    } else {
                        m.append("MISSING!").color(ChatColor.RED);
                    }
                    sender.sendMessage(m.create());
                });

        sender.sendMessage("");

        sender.sendMessage(new ComponentBuilder("--- Tags ---").color(ChatColor.AQUA).create());

        tags.keySet().stream()
                .sorted()
                .forEach( s-> {
                    if (requiredTags.contains(s)) {
                        return;
                    }

                    sender.sendMessage(new ComponentBuilder(s + ": ").color(ChatColor.DARK_AQUA)
                            .append(tags.get(s)).color(ChatColor.GREEN).create());

                });
    }

    /**
     * Set Tags on an instance
     *
     */
    private void setInstance(CommandSender sender, Arguments arguments) {
        if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ Set Instance Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("Set a tag on an Instance. Leave value blank to clear.").color(ChatColor.DARK_AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb instance set").color(ChatColor.RED).append(" <instance_name> <tag_name> [<value>]").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
            sender.sendMessage(new ComponentBuilder("/mb instance set").color(ChatColor.RED).append(" World1 EULA true").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("/mb instance set").color(ChatColor.RED).append(" World1 EULA").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        final String instanceName = arguments.args.get(0);
        Instance instance = plugin.getInstanceManager().getInstance(instanceName);

        if (instance == null) {
            sender.sendMessage(new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
            return;
        }

        String tag = arguments.args.get(1);
        if (arguments.args.size() > 2) {
            instance.setTag(tag, String.join(" ", arguments.args.subList(2,arguments.args.size())));
            sender.sendMessage(new ComponentBuilder("Set ").color(ChatColor.GREEN)
                    .append(tag.toUpperCase()).color(ChatColor.YELLOW)
                    .append(" = ").color(ChatColor.GREEN)
                    .append(arguments.args.get(2)).color(ChatColor.YELLOW).create());
        } else {
            instance.clearTag(tag);
            sender.sendMessage(new ComponentBuilder("Cleared ").color(ChatColor.GREEN)
                    .append(tag.toUpperCase()).color(ChatColor.YELLOW).create());
        }
    }

    /**
     * Set Global Tags
     *
     */
    private void setGlobal(CommandSender sender, Arguments arguments) {
        if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
            sender.sendMessage(new ComponentBuilder("--- [ Set Global Help ] ---").color(ChatColor.AQUA).create());
            sender.sendMessage(new ComponentBuilder("Set a tag gloally. Leave value blank to clear.").color(ChatColor.DARK_AQUA).create());
            sender.sendMessage(new ComponentBuilder("/mb global set").color(ChatColor.RED).append(" <tag_name> [<value>]").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
            sender.sendMessage(new ComponentBuilder("/mb global set").color(ChatColor.RED).append(" EULA true").color(ChatColor.YELLOW).create());
            sender.sendMessage(new ComponentBuilder("/mb global set").color(ChatColor.RED).append(" EULA").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        String tag = arguments.args.get(0);
        if (arguments.args.size() > 1) {
            plugin.getGlobalManager().setTag(tag, String.join(" ", arguments.args.subList(2,arguments.args.size())));
            sender.sendMessage(new ComponentBuilder("Set ").color(ChatColor.GREEN)
                    .append(tag.toUpperCase()).color(ChatColor.YELLOW)
                    .append(" = ").color(ChatColor.GREEN)
                    .append(arguments.args.get(1)).color(ChatColor.YELLOW).create());
        } else {
            plugin.getGlobalManager().clearTag(tag);
            sender.sendMessage(new ComponentBuilder("Cleared ").color(ChatColor.GREEN)
                    .append(tag.toUpperCase()).color(ChatColor.YELLOW).create());
        }
    }

//    /**
//     * Send user to an instance, starting it if necessary
//     *
//     */
//    private void sendInstance(CommandSender sender, Arguments arguments) {
//        if (arguments.args.size() == 0 || arguments.args.get(0).equalsIgnoreCase("help")) {
//            sender.sendMessage(new ComponentBuilder("--- [ Send to Instance Help ] ---").color(ChatColor.AQUA).create());
//            sender.sendMessage(new ComponentBuilder("Send a player to an Instance, starting it if necessary.").color(ChatColor.DARK_AQUA).create());
//            sender.sendMessage(new ComponentBuilder("/mb instance send").color(ChatColor.RED).append(" <instance_name> [<player>]").color(ChatColor.YELLOW).create());
//            sender.sendMessage(new ComponentBuilder("Examples:").color(ChatColor.LIGHT_PURPLE).create());
//            sender.sendMessage(new ComponentBuilder("/mb instance send").color(ChatColor.RED).append(" World1").color(ChatColor.YELLOW).create());
//            sender.sendMessage(new ComponentBuilder("/mb instance send").color(ChatColor.RED).append(" World1 notch").color(ChatColor.YELLOW).create());
//            return;
//        }
//
//        // Arguments
//        final String instanceName = arguments.args.get(0);
//        Instance instance = plugin.getInstanceManager().getInstance(instanceName);
//
//        if (instance == null) {
//            sender.sendMessage(new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
//            return;
//        }
//
//        ProxiedPlayer player;
//        if (arguments.args.size() > 1) {
//            player = plugin.getProxy().getPlayer(arguments.args.get(1));
//            if (player == null) {
//                sender.sendMessage(new ComponentBuilder("Cannot find Player: ").color(ChatColor.RED)
//                        .append(arguments.args.get(1)).color(ChatColor.YELLOW).create());
//                return;
//            }
//        } else {
//            if (sender instanceof ProxiedPlayer) {
//                player = (ProxiedPlayer) sender;
//            } else {
//                sender.sendMessage(new ComponentBuilder("Can't send the console there.").color(ChatColor.RED).create());
//                return;
//            }
//        }
//
//        if (!instance.isRunning()) {
//            instance.start();
//        }
//
//        Date date = new Date();
//        long time = date.getTime();
//
//        // Wait for a ping to come back
//        plugin.getProxy().getScheduler().schedule(plugin, () -> {
//            plugin.getProxy().getServers().get(instance.getName()).ping(new Callback<ServerPing>() {
//
//                @Override
//                public void done(ServerPing result, Throwable error) {
//                    if(error!=null){
//                        // Failed
//                        if (date.getTime() - time > 20 * 1000) {
//                            sender.sendMessage(new ComponentBuilder("Failed to send player.").color(ChatColor.RED).create());
//                        }
//                        return;
//                    }
//                    ServerInfo target = plugin.getProxy().getServerInfo(instance.getName());
//
//                    // Success
//                    sender.sendMessage(new ComponentBuilder("Sending ").color(ChatColor.GREEN)
//                            .append(player.getName()).color(ChatColor.YELLOW)
//                            .append(" to ").color(ChatColor.GREEN)
//                            .append(getName()).color(ChatColor.YELLOW).create());
//
//                    player.connect(target);
//                }
//            });
//        }, 1, 1, TimeUnit.SECONDS);
//
//
//
//    }

}
