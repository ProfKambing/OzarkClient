package me.trambled.ozark.ozarkclient.command;

import me.trambled.ozark.ozarkclient.command.commands.*;
import me.trambled.turok.values.TurokString;
import net.minecraft.util.text.Style;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class Commands {
	public static ArrayList<Command> command_list = new ArrayList<>();
	static HashMap<java.lang.String, Command> list_command  = new HashMap<>();

	public static final TurokString prefix = new TurokString("Prefix", "Prefix", ".");

	public final Style style;

	public Commands(Style style_) {
		style = style_;

		add_command(new BindCommand());
		add_command(new PrefixCommand());
		add_command(new SettingsCommand());
		add_command(new ToggleCommand());
		add_command(new AlertCommand());
		add_command(new HelpCommand());
		add_command(new FriendCommand());
		add_command(new DrawnCommand());
		add_command(new EzMessageCommand());
		add_command(new EnemyCommand());
		add_command(new ConfigCommand());
		add_command(new ServerCommand());
		add_command(new AutoKitCommand());
		add_command(new AutoGearCommand());
		add_command(new XrayCommand());
		add_command(new RenameCommand());
		add_command(new NotificationTestCommand());
		add_command(new ClearRamCommand());
		add_command(new NamemcCommand());
		add_command(new OpenFolderCommand());

		command_list.sort(Comparator.comparing(Command::get_name));
	}

	public static void add_command(Command command) {
		command_list.add(command);

		list_command.put(command.get_name().toLowerCase(), command);
	}

	public java.lang.String[] get_message(java.lang.String message) {
		java.lang.String[] arguments = {};

		if (has_prefix(message)) {
			arguments = message.replaceFirst(prefix.get_value(), "").split(" ");
		}

		return arguments;
	}

	public boolean has_prefix(java.lang.String message) {
		return message.startsWith(prefix.get_value());
	}

	public void set_prefix(java.lang.String new_prefix) {
		prefix.set_value(new_prefix);
	}

	public java.lang.String get_prefix() {
		return prefix.get_value();
	}

	public static ArrayList<Command> get_pure_command_list() {
		return command_list;
	}

	public static Command get_command_with_name(java.lang.String name) {
		return list_command.get(name.toLowerCase());
	}
}