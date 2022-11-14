package com.github.kbinani.holosportsfestival2022;

import com.github.kbinani.holosportsfestival2022.boatrace.BoatRaceEventListener;
import com.github.kbinani.holosportsfestival2022.daruma.DarumaEventListener;
import com.github.kbinani.holosportsfestival2022.fencing.FencingEventListener;
import com.github.kbinani.holosportsfestival2022.mob.MobFightEventListener;
import com.github.kbinani.holosportsfestival2022.relay.RelayEventListener;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener {
    private MobFightEventListener mobFightEventListener;
    private FencingEventListener fencingEventListener;
    private BoatRaceEventListener boatRaceEventListener;
    private RelayEventListener relayEventListener;
    private DarumaEventListener darumaEventListener;

    public Main() {
    }

    @Override
    public void onEnable() {
        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        overworld().ifPresent(world -> {
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
        });
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

        this.mobFightEventListener = new MobFightEventListener(this, 20);
        this.fencingEventListener = new FencingEventListener(this, 40);
        this.boatRaceEventListener = new BoatRaceEventListener(this, 60);
        this.relayEventListener = new RelayEventListener(this, 80);
        this.darumaEventListener = new DarumaEventListener(this, 100);
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(this.mobFightEventListener, this);
        pluginManager.registerEvents(this.fencingEventListener, this);
        pluginManager.registerEvents(this.boatRaceEventListener, this);
        pluginManager.registerEvents(this.relayEventListener, this);
        pluginManager.registerEvents(this.darumaEventListener, this);
        pluginManager.registerEvents(this, this);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.isCancelled()) {
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

    private Optional<World> overworld() {
        return getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
    }
}