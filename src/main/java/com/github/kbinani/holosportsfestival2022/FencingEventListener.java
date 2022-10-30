package com.github.kbinani.holosportsfestival2022;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class FencingEventListener implements Listener {
    private final JavaPlugin owner;
    private @Nullable UUID playerLeft;
    private @Nullable UUID playerRight;
    private int hitpointLeft = 3;
    private int hitpointRight = 3;
    static final String kBossbarLeft = "sports_festival_2022_bossbar_left";
    static final String kBossbarRight = "sports_festival_2022_bossbar_right";
    static final int kFieldX = 104;
    static final int kFieldY = -18;
    static final int kFieldZ = -268;
    static final int kFieldDx = 61;
    static final int kFieldDz = 4;
    static final int kWeaponKnockbackLevel = 10;

    FencingEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    enum Status {
        IDLE,
        RUN,
        AWAIT_DEATH,
    }

    enum Team {
        LEFT,
        RIGHT,
    }

    private FencingEventListener.Status _status = FencingEventListener.Status.IDLE;

    private void setStatus(Status status) {
        if (status == _status) {
            return;
        }
        _status = status;
        switch (status) {
            case RUN:
                // 範囲内に居るプレイヤーを観客席側に排除する
                execute("tp @p[x=" + kFieldX + ",y=" + kFieldY + ",z=" + kFieldZ + ",dx=" + kFieldDx + ",dy=5,dz=" + kFieldDz + "] 134 -17 -276");

                // 左ゲート構築
                execute("fill 165 -16 -268 165 -18 -264 white_concrete");
                execute("fill 165 -17 -267 165 -18 -265 glass");
                execute("setblock 164 -17 -266 iron_door[facing=west,half=upper,hinge=left]");
                execute("setblock 164 -18 -266 iron_door[facing=west,half=lower,hinge=left]");
                execute("setblock 165 -17 -266 air");
                execute("setblock 165 -18 -266 heavy_weighted_pressure_plate");

                // 右ゲート構築
                execute("fill 103 -16 -264 103 -18 -268 white_concrete");
                execute("fill 103 -17 -265 103 -18 -267 glass");
                execute("setblock 104 -17 -266 iron_door[facing=east,half=upper,hinge=left]");
                execute("setblock 104 -18 -266 iron_door[facing=east,half=lower,hinge=left]");
                execute("setblock 103 -17 -266 air");
                execute("setblock 103 -18 -266 heavy_weighted_pressure_plate");

                // バリアブロックの柵
                execute("fill 165 -17 -269 103 -17 -269 barrier");
                execute("fill 104 -17 -263 165 -17 -263 barrier");

                // bossbar 追加
                execute("bossbar remove " + kBossbarLeft);
                execute("bossbar add " + kBossbarLeft + " \"<<< " + TeamName(Team.LEFT) + " <<<\"");
                execute("bossbar set " + kBossbarLeft + " max 3");
                execute("bossbar set " + kBossbarLeft + " value 3");
                execute("bossbar set " + kBossbarLeft + " color green");

                execute("bossbar remove " + kBossbarRight);
                execute("bossbar add " + kBossbarRight + " \">>> " + TeamName(Team.RIGHT) + " >>>\"");
                execute("bossbar set " + kBossbarRight + " max 3");
                execute("bossbar set " + kBossbarRight + " value 3");
                execute("bossbar set " + kBossbarRight + " color green");

                execute("bossbar set " + kBossbarLeft + " players @a");
                execute("bossbar set " + kBossbarRight + " players @a");

                hitpointRight = 3;
                hitpointLeft = 3;

                broadcast("");
                broadcast("[フェンシング] 競技を開始します！");
                broadcast("");
                break;
            case IDLE:
                clearField();
                break;
            case AWAIT_DEATH:
                clearField();
                break;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (_status != Status.RUN) {
            return;
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (_status != Status.RUN) {
            return;
        }
        if (playerRight == null || playerLeft == null) {
            return;
        }
        Entity entity = e.getEntity();
        Entity damager = e.getDamager();
        if (entity.getType() != EntityType.PLAYER || damager.getType() != EntityType.PLAYER) {
            return;
        }
        Player defence = (Player) entity;
        Player offence = (Player) damager;

        Team defenceTeam = null;
        if (defence.getUniqueId().equals(playerLeft) && offence.getUniqueId().equals(playerRight)) {
            defenceTeam = Team.LEFT;
        } else if (defence.getUniqueId().equals(playerRight) && offence.getUniqueId().equals(playerLeft)) {
            defenceTeam = Team.RIGHT;
        } else {
            return;
        }
        Team offenceTeam = TeamHostile(defenceTeam);

        Location location = offence.getLocation();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        //NOTE: 攻撃側がフィールド内に居るかどうかだけ判定する.
        if (x < kFieldX || kFieldX + kFieldDx < x || y < kFieldY || z < kFieldZ || kFieldZ + kFieldDz < z) {
            return;
        }
        EntityEquipment equipment = offence.getEquipment();
        if (equipment == null) {
            return;
        }
        ItemStack mainHand = equipment.getItemInMainHand();
        if (mainHand.getType() != Material.IRON_SWORD) {
            e.setCancelled(true);
            return;
        }
        int knockback = mainHand.getEnchantments().get(Enchantment.KNOCKBACK);
        if (knockback != kWeaponKnockbackLevel) {
            e.setCancelled(true);
            return;
        }
        Player loser = damageToKill(defenceTeam);

        execute("bossbar set " + kBossbarLeft + " value " + hitpointLeft);
        execute("bossbar set " + kBossbarRight + " value " + hitpointRight);

        if (loser != null) {
            //TODO: この処理は敗北者を kill してから行う
            Player winner = getPlayer(getPlayerUid(offenceTeam));
            broadcast("");
            broadcast("-----------------------");
            broadcast("[試合終了]");
            if (winner == null) {
                broadcastUnofficial(TeamName(offenceTeam) + "が勝利！");
            } else {
                broadcast(winner.getName() + "が勝利！");
            }
            broadcast("-----------------------");
            broadcast("");
        }
    }

    private void clearField() {
        execute("fill 102 -16 -269 165 -18 -264 air");
        execute("bossbar remove " + kBossbarLeft);
        execute("bossbar remove " + kBossbarRight);
    }

    private @Nullable Player damageToKill(Team team) {
        if (_status != Status.RUN) {
            return null;
        }
        if (hitpointLeft < 1 || hitpointRight < 1) {
            // 既に決着がついている
            return null;
        }
        if (team == Team.LEFT) {
            hitpointLeft -= 1;
        } else if (team == Team.RIGHT) {
            hitpointRight -= 1;
        }
        UUID loserUid;
        if (hitpointLeft < 1) {
            loserUid = playerLeft;
        } else if (hitpointRight < 1) {
            loserUid = playerRight;
        } else {
            return null;
        }
        if (loserUid == null) {
            // playerLeft か playerRight なぜか null. ノーコンテストにする
            setStatus(Status.IDLE);
            return null;
        }
        setStatus(Status.AWAIT_DEATH);
        return getPlayer(loserUid);
    }

    @EventHandler
    public void onBlockRedstoneEvent(BlockRedstoneEvent e) {
        if (e.getOldCurrent() != 0 || e.getNewCurrent() <= 0) {
            return;
        }

        Location location = e.getBlock().getLocation();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if (x == 101 && y == -18 && z == -264) {
            onClickJoin(location, Team.RIGHT);
        } else if (x == 167 && y == -18 && z == -264) {
            onClickJoin(location, Team.LEFT);
        } else if (x == 169 && y == -18 && z == -264) {
            onClickLeave(location, Team.LEFT);
        } else if (x == 99 && y == -18 && z == -264) {
            onClickLeave(location, Team.RIGHT);
        } else if (x == 134 && y == -18 && z == -272) {
            onClickStart();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (playerLeft != null && player.getUniqueId().equals(playerLeft)) {
            clearPlayer(Team.LEFT);
        }
        if (playerRight != null && player.getUniqueId().equals(playerRight)) {
            clearPlayer(Team.RIGHT);
        }
    }

    private @Nullable UUID getPlayerUid(Team team) {
        if (team == Team.RIGHT) {
            return playerRight;
        } else if (team == Team.LEFT) {
            return playerLeft;
        } else {
            return null;
        }
    }

    private void joinPlayer(@Nonnull Player player, Team team) {
        if (team == Team.RIGHT) {
            if (playerLeft != null && playerLeft.equals(player.getUniqueId())) {
                broadcastUnofficial(ChatColor.RED + "[フェンシング] " + player.getName() + "は" + TeamName(Team.LEFT) + "としてエントリー済みです");
            } else {
                playerRight = player.getUniqueId();
                execute("give @p[name=\"" + player.getName() + "\"] iron_sword{Enchantments:[{id:knockback,lvl:10}]}");
                broadcast("[フェンシング] " + player.getName() + "がエントリーしました（" + TeamName(team) + "）");
            }
        } else if (team == Team.LEFT) {
            if (playerRight != null && playerRight.equals(player.getUniqueId())) {
                broadcastUnofficial(ChatColor.RED + "[フェンシング] " + player.getName() + "は" + TeamName(Team.RIGHT) + "としてエントリー済みです");
            } else {
                playerLeft = player.getUniqueId();
                execute("give @p[name=\"" + player.getName() + "\"] iron_sword{Enchantments:[{id:knockback,lvl:10}]}");
                broadcast("[フェンシング] " + player.getName() + "がエントリーしました（" + TeamName(team) + "）");
            }
        }
    }

    private void clearPlayer(Team team) {
        if (team == Team.RIGHT) {
            playerRight = null;
        } else if (team == Team.LEFT) {
            playerLeft = null;
        }
        setStatus(Status.IDLE);
    }

    private void broadcast(String message) {
        owner.getServer().broadcastMessage(message);
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String message) {
        broadcast(message);
    }

    private void onClickJoin(Location location, Team team) {
        if (_status != Status.IDLE) {
            return;
        }
        UUID uid = getPlayerUid(team);
        if (uid != null) {
            //TODO: 既に join 済みの時のメッセージ
            return;
        }
        Player player = findNearest(location);
        if (player == null) {
            return;
        }
        joinPlayer(player, team);
    }

    private static String TeamName(Team team) {
        switch (team) {
            case LEFT:
                return "LEFT SIDE";
            case RIGHT:
                return "RIGHT SIDE";
            default:
                return "";
        }
    }

    private static Team TeamHostile(Team team) {
        if (team == Team.LEFT) {
            return Team.RIGHT;
        } else {
            return Team.LEFT;
        }
    }

    private void onClickLeave(Location location, Team team) {
        // status をリセットするため _status == Status.IDLE チェックは入れずに強制的にエントリー解除処理する
        UUID uid = getPlayerUid(team);
        if (uid == null) {
            return;
        }
        Player player = findNearest(location);
        if (player == null) {
            return;
        }
        if (!uid.equals(player.getUniqueId())) {
            return;
        }
        clearPlayer(team);
        broadcastUnofficial("[フェンシング] " + player.getName() + "がエントリー解除しました（" + TeamName(team) + "）");
    }

    private void onClickStart() {
        if (_status != Status.IDLE) {
            return;
        }
        Player left = null;
        Player right = null;
        int numLeft = 0;
        int numRight = 0;
        if (playerLeft != null) {
            left = getPlayer(playerLeft);
            if (left == null) {
                clearPlayer(Team.LEFT);
            } else {
                numLeft += 1;
            }
        }
        if (playerRight != null) {
            right = getPlayer(playerRight);
            if (right == null) {
                clearPlayer(Team.RIGHT);
            } else {
                numRight += 1;
            }
        }
        if (numLeft != numRight || numLeft < 1 || numRight < 1) {
            broadcast("参加人数が正しくありません（" + TeamName(Team.LEFT) + " : " + numLeft + "人、" + TeamName(Team.RIGHT) + " : " + numRight + "人）");
            return;
        }
        setStatus(Status.RUN);
    }

    private void execute(String command) {
        Server server = owner.getServer();
        CommandSender sender = server.getConsoleSender();
        server.dispatchCommand(sender, command);
    }

    private @Nullable Player getPlayer(UUID uid) {
        Server server = owner.getServer();
        Optional<World> maybeWorld = server.getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
        if (maybeWorld.isEmpty()) {
            return null;
        }
        World world = maybeWorld.get();
        Optional<Player> maybePlayer = world.getPlayers().stream().filter(it -> it.getUniqueId().equals(uid)).findFirst();
        return maybePlayer.orElse(null);
    }

    private Player findNearest(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        Optional<Entity> entity = world.getNearbyEntities(location, 2.5, 2.5, 2.5, (it) -> it.getType() == EntityType.PLAYER).stream().findFirst();
        if (entity.isEmpty()) {
            return null;
        }
        Entity e = entity.get();
        if (e instanceof Player) {
            return (Player) e;
        } else {
            return null;
        }
    }
}