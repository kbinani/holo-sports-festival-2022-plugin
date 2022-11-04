package com.github.kbinani.holosportsfestival2022;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
  private MobFightEventListener mobFightEventListener;
  private FencingEventListener fencingEventListener;
  private BoatRaceEventListener boatRaceEventListener;
  private RelayEventListener relayEventListener;

  public Main() {
  }

  @Override
  public void onEnable() {
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
}