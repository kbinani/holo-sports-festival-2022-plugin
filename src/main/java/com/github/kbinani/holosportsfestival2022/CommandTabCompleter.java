package com.github.kbinani.holosportsfestival2022;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

class CommandTabCompleter implements TabCompleter {
    static final String kCommandLabel = "holosportsfestival2022";
    static final String[] kMiniGameNames = new String[]{"boatrace", "daruma", "fencing", "mob", "relay"};

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!label.equals(kCommandLabel)) {
            return null;
        }
        if (args.length == 0) {
            return null;
        } else if (args.length == 1) {
            String arg = args[0];
            List<String> candidate = Arrays.asList("help", "reset");
            if (arg.isEmpty()) {
                return candidate;
            } else {
                return candidate.stream().filter(it -> it.startsWith(arg)).toList();
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            String arg = args[1];
            List<String> candidate = Arrays.asList(kMiniGameNames);
            if (arg.isEmpty()) {
                return candidate;
            } else {
                return candidate.stream().filter(it -> it.startsWith(arg)).toList();
            }
        }
        return null;
    }
}