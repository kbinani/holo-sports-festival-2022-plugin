package com.github.kbinani.holosportsfestival2022;

import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
  private MobFightEventListener mobFightEventListener;
  private FencingEventListener fencingEventListener;

  public Main() {
  }

  @Override
  public void onEnable() {
    this.mobFightEventListener = new MobFightEventListener(this);
    this.fencingEventListener = new FencingEventListener(this);
    PluginManager pluginManager = getServer().getPluginManager();
    pluginManager.registerEvents(this.mobFightEventListener, this);
    pluginManager.registerEvents(this.fencingEventListener, this);
  }
}