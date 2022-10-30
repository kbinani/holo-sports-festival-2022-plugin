package com.github.kbinani.holosportsfestival2022;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MobFightEventListener implements Listener {
    private final JavaPlugin owner;

    MobFightEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {

    }
/*
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        this.lastMovedTime.remove(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        this.lastMovedTime.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damageeEntity = e.getEntity();
        if (!(damageeEntity instanceof Player)) {
            return;
        }
        Player damagee = (Player) damageeEntity;

        Player attacker;
        Entity attackerEntity = e.getDamager();
        if (attackerEntity instanceof Arrow) {
            Arrow arrow = (Arrow) attackerEntity;
            Object shooter = arrow.getShooter();
            if (shooter instanceof Player) {
                attacker = (Player) shooter;
            } else {
                return;
            }
        } else if (attackerEntity instanceof Player) {
            attacker = (Player) attackerEntity;
        } else {
            return;
        }

        if (!IsInDarumaField(damagee.getLocation())) {
            return;
        }

        GameMode damageeGameMode = damagee.getGameMode();
        if (damageeGameMode != GameMode.ADVENTURE && damageeGameMode != GameMode.SURVIVAL) {
            return;
        }
        e.setCancelled(true);
        L10n.send(attacker, L10n.Message.INFO_INTERFERING_OTHERS);
        kill(attacker);
    }

    private static boolean IsInDarumaField(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        if (x < 589 || 589 + 40 < x) {
            return false;
        }
        if (y < 66 || 67 + 3 < y) {
            return false;
        }
        if (z < -673 || -673 + 99 < z) {
            return false;
        }
        return true;
    }

    public void killMovedPlayers(Server server) {
        server.getOnlinePlayers().stream().filter(this::shouldKillPlayer).forEach(this::kill);
    }

    private void kill(Player player) {
        Server server = player.getServer();
        server.dispatchCommand(server.getConsoleSender(), "kill " + player.getName());
    }

    private boolean shouldKillPlayer(Player player) {
        if (player.isDead()) {
            return false;
        }
        if (!IsInDarumaField(player.getLocation())) {
            return false;
        }
        GameMode mode = player.getGameMode();
        if (mode != GameMode.ADVENTURE && mode != GameMode.SURVIVAL) {
            return false;
        }
        if (player.getVehicle() != null) {
            return true;
        }
        UUID id = player.getUniqueId();
        if (!lastMovedTime.containsKey(id)) {
            return false;
        }
        long lastMoved = lastMovedTime.get(id);
        double elapsed = (System.currentTimeMillis() - lastMoved) / 1000.0;
        if (elapsed >= 0.1) {
            return false;
        }
        return true;
    }

    private void info(String message) {
        owner.getLogger().info(message);
    }
 */
}
