package com.github.kbinani.holosportsfestival2022;

import com.github.kbinani.holosportsfestival2022.boatrace.BoatRaceEventListener;
import com.github.kbinani.holosportsfestival2022.daruma.DarumaEventListener;
import com.github.kbinani.holosportsfestival2022.fencing.FencingEventListener;
import com.github.kbinani.holosportsfestival2022.mob.MobFightEventListener;
import com.github.kbinani.holosportsfestival2022.relay.RelayEventListener;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin implements Listener, MainDelegate {
    private final List<Competition> competitions = new ArrayList<>();
    private World world;

    public Main() {
    }

    @Override
    public @Nullable CompetitionType mainGetCurrentCompetition(Player player) {
        for (Competition competition : competitions) {
            if (competition.competitionIsJoined(player)) {
                return competition.competitionGetType();
            }
        }
        return null;
    }

    @Override
    public void mainRunTask(Runnable task) {
        getServer().getScheduler().runTask(this, task);
    }

    @Override
    public void mainRunTaskLater(Runnable task, long delay) {
        getServer().getScheduler().runTaskLater(this, task, delay);
    }

    @Override
    public BukkitTask mainRunTaskTimer(Runnable task, long delay, long period) {
        return getServer().getScheduler().runTaskTimer(this, task, delay, period);
    }

    @Override
    @Nonnull
    public World mainGetWorld() {
        return world;
    }

    @Override
    public Logger mainGetLogger() {
        return getLogger();
    }

    @Override
    public void mainCountdownThen(BoundingBox[] boxes, Predicate<Integer> countdown, Supplier<Boolean> task, long delay) {
        Countdown.Then(world, boxes, this, countdown, task, delay);
    }

    @Override
    public void mainClearCompetitionItems(Player player) {
        for (Competition competition : competitions) {
            competition.competitionClearItems(player);
        }
    }

    @Override
    public void mainUsingChunk(BoundingBox box, Consumer<World> callback) {
        Loader.UsingChunk(world, box, this, callback);
    }

    @Override
    public void onEnable() {
        Optional<World> overworld = getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
        if (overworld.isEmpty()) {
            getLogger().log(Level.SEVERE, "server should have at least one overworld dimension");
            setEnabled(false);
            return;
        }
        world = overworld.get();

        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Boolean mobGriefing = world.getGameRuleValue(GameRule.MOB_GRIEFING);
        Boolean keepInventory = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
        Boolean showDeathMessages = world.getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
        Boolean announceAdvancements = world.getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS);
        if (mobGriefing != null && mobGriefing) {
            reasons.add("mobGriefing gamerule is set to true");
        }
        if (keepInventory != null && !keepInventory) {
            reasons.add("keepInventory gamerule is set to false");
        }
        if (showDeathMessages != null && showDeathMessages) {
            warnings.add("showDeathMessages gamerule is set to true");
        }
        if (announceAdvancements != null && announceAdvancements) {
            warnings.add("announceAdvancements gamerule is set to true");
        }
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            reasons.add("the \"mob\" mini game is not playable as the difficulty is set to peaceful");
        }
        if (!world.getPVP()) {
            reasons.add("pvp is set to false");
        }
        if (!reasons.isEmpty()) {
            getLogger().log(Level.SEVERE, "Disabling the plugin because:");
            for (String reason : reasons) {
                getLogger().log(Level.SEVERE, "  " + reason);
            }
            setEnabled(false);
            return;
        }
        if (!warnings.isEmpty()) {
            for (String warning : warnings) {
                getLogger().warning(warning);
            }
        }

        competitions.add(new MobFightEventListener(this, 20));
        competitions.add(new FencingEventListener(this, 40));
        competitions.add(new BoatRaceEventListener(this, 60));
        competitions.add(new RelayEventListener(this, 80));
        competitions.add(new DarumaEventListener(this, 100));
        PluginManager pluginManager = getServer().getPluginManager();
        for (Listener competition : competitions) {
            pluginManager.registerEvents(competition, this);
        }
        pluginManager.registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        competitions.forEach(Competition::competitionReset);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getEntity().getWorld() != world) {
            return;
        }
        switch (e.getSpawnReason()) {
            case NATURAL:
            case VILLAGE_INVASION:
            case BUILD_WITHER:
            case BUILD_IRONGOLEM:
            case BUILD_SNOWMAN:
            case SPAWNER_EGG:
            case SPAWNER:
                e.setCancelled(true);
                break;
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }
}