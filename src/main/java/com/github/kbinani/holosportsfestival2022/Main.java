package com.github.kbinani.holosportsfestival2022;

import com.github.kbinani.holosportsfestival2022.mob.MobFightEventListener;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Main extends JavaPlugin {
  private MobFightEventListener mobFightEventListener;
  private FencingEventListener fencingEventListener;
  private BoatRaceEventListener boatRaceEventListener;
  private RelayEventListener relayEventListener;

  public Main() {
  }

  @Override
  public void onEnable() {
    List<String> reasons = new ArrayList<>();
    overworld().ifPresent(world -> {
      Boolean mobGriefing = world.getGameRuleValue(GameRule.MOB_GRIEFING);
      if (mobGriefing != null && mobGriefing) {
        reasons.add( "mobGriefing gamerule is set to true");
      }
    });
    if (!reasons.isEmpty()) {
      getLogger().warning("Disabling the plugin because:");
      for (String reason : reasons) {
        getLogger().warning("  " + reason);
      }
      setEnabled(false);
      return;
    }

    this.mobFightEventListener = new MobFightEventListener(this);
    this.fencingEventListener = new FencingEventListener(this);
    this.boatRaceEventListener = new BoatRaceEventListener(this);
    this.relayEventListener = new RelayEventListener(this);
    PluginManager pluginManager = getServer().getPluginManager();
    pluginManager.registerEvents(this.mobFightEventListener, this);
    pluginManager.registerEvents(this.fencingEventListener, this);
    pluginManager.registerEvents(this.boatRaceEventListener, this);
    pluginManager.registerEvents(this.relayEventListener, this);
  }

  private Optional<World> overworld() {
    return getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
  }
}