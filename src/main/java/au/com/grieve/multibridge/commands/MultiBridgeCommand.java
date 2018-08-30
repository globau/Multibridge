package au.com.grieve.multibridge.commands;

import au.com.grieve.multibridge.MultiBridge;
import au.com.grieve.multibridge.util.InstanceManager;
import au.com.grieve.multibridge.util.TemplateManager;
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
        public List<String> before;
        public List<String> args;

        /**
         * Initialise Arguments
         */
        public Arguments(String[] args) {
            this.args = new ArrayList<>(Arrays.asList(args));
        }

        public Arguments(List<String> before, List<String> args) {
            this.args = args;
            this.before = before;
        }

        public Arguments shift(int num) {
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
        if (args.length == 0) {
            sender.sendMessage(new ComponentBuilder("TODO Help2").color(ChatColor.DARK_PURPLE).create());
            return;
        }

        Arguments arguments = new Arguments(args);


        switch(arguments.args.get(0).toLowerCase()) {
            case "send":
                break;
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
        if (arguments.args.size() == 0) {
            sender.sendMessage(new ComponentBuilder("TODO Template Help").color(ChatColor.DARK_PURPLE).create());
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
        if (arguments.args.size() == 0) {
            sender.sendMessage(new ComponentBuilder("TODO Instance Help").color(ChatColor.DARK_PURPLE).create());
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
        sender.sendMessage(new ComponentBuilder("Multibridge Templates Available").color(ChatColor.GREEN).create());
        sender.sendMessage(new ComponentBuilder("-------------------------------").color(ChatColor.GREEN).create());
        Map<String, TemplateManager.Template> templates = plugin.getTemplateManager().getTemplates();
        if (templates.size() == 0) {
            sender.sendMessage(new ComponentBuilder("No templates found").color(ChatColor.GREEN).create());
            return;
        }

        for (String template: templates.keySet()) {
            sender.sendMessage(new ComponentBuilder(template).color(ChatColor.GREEN).create());
        }

    }

    /**
     * List all Instances available
     *
     */
    private void listInstances(CommandSender sender,Arguments arguments) {
        sender.sendMessage(new ComponentBuilder("Multibridge Instances").color(ChatColor.GREEN).create());
        Map<String, InstanceManager.Instance> instances = plugin.getInstanceManager().getInstances();
        if (instances.size() == 0) {
            sender.sendMessage(new ComponentBuilder("No instances found").color(ChatColor.RED).create());
            return;
        }

        for (InstanceManager.Instance instance: instances.values()) {
            ComponentBuilder msg = new ComponentBuilder(" - [").color(ChatColor.DARK_GRAY);

            if (instance.isRunning()) {
                msg.append("ACTIVE").color(ChatColor.GREEN);
            } else {
                msg.append("INACTIVE").color(ChatColor.GRAY);
            }

            msg.append("] ").color(ChatColor.DARK_GRAY);

            msg.append(instance.getName()).color(ChatColor.RESET);

            sender.sendMessage(msg.create());
        }

    }

    /**
     * Create a new Server Instance
     *
     */
    private void createInstance(CommandSender sender, Arguments arguments) {
        // Help
        if (arguments.args.size() < 2) {
            sender.sendMessage(new ComponentBuilder("/" + String.join(" ", arguments.before) + " ").color(ChatColor.RED).append("<template_name> <instance_name>").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        final String templateName = arguments.args.get(0);
        final String instanceName = arguments.args.get(1);

        // Schedule the task
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                // Create a new Instance
                InstanceManager.Instance instance = plugin.getInstanceManager().create(templateName, instanceName);

                // @TODO: Make use of exceptions rather than returning null
                if (instance == null) {
                    sender.sendMessage(new ComponentBuilder("Unable to create new Instance").color(ChatColor.RED).create());
                    return;
                }

                // Success
                sender.sendMessage(new ComponentBuilder("New Instance Created: ").color(ChatColor.GREEN).append(instanceName).color(ChatColor.YELLOW).create());
            }
        });
    }

    /**
     * Remove an Instance
     *
     */
    private void removeInstance(CommandSender sender, Arguments arguments) {
        // Help
        if (arguments.args.size() < 1) {
            sender.sendMessage(new ComponentBuilder("/" + String.join(" ", arguments.before) + " ").color(ChatColor.RED).append("<instance_name>").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        final String instanceName = arguments.args.get(0);

        // Schedule the task
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                InstanceManager.Instance instance = plugin.getInstanceManager().getInstance(instanceName);

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
            }
        });
    }

    /**
     * Start existing Server Instance
     *
     */
    private void startInstance(CommandSender sender, Arguments arguments) {
        // Help
        if (arguments.args.size() < 1) {
            sender.sendMessage(new ComponentBuilder("/" + String.join(" ", arguments.before) + " ").color(ChatColor.RED).append("<instance_name>").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        final String instanceName = arguments.args.get(0);
        InstanceManager.Instance instance = plugin.getInstanceManager().getInstance(instanceName);

        if (instance == null) {
            sender.sendMessage(new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
            return;
        }

        if (instance.isRunning()) {
            sender.sendMessage(new ComponentBuilder("Instance is already running").color(ChatColor.RED).create());
            return;
        }

        // Schedule the task
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                // Start Instance
                instance.start();

                // Success
                sender.sendMessage(new ComponentBuilder("Instance Starting").color(ChatColor.GREEN).create());
            }
        });

    }

    /**
     * Start existing Server Instance
     *
     */
    private void stopInstance(CommandSender sender, Arguments arguments) {
        // Help
        if (arguments.args.size() < 1) {
            sender.sendMessage(new ComponentBuilder("/" + String.join(" ", arguments.before) + " ").color(ChatColor.RED).append("<instance_name>").color(ChatColor.YELLOW).create());
            return;
        }

        // Arguments
        final String instanceName = arguments.args.get(0);
        InstanceManager.Instance instance = plugin.getInstanceManager().getInstance(instanceName);

        if (instance == null) {
            sender.sendMessage(new ComponentBuilder("Cannot find Instance").color(ChatColor.RED).create());
            return;
        }

        if (!instance.isRunning()) {
            sender.sendMessage(new ComponentBuilder("Instance not running").color(ChatColor.RED).create());
            return;
        }

        // Schedule the task
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                // Start Instance
                instance.stop();

                // Success
                sender.sendMessage(new ComponentBuilder("Instance Stopping").color(ChatColor.GREEN).create());
            }
        });

    }

}
