package com.github.kbinani.holosportsfestival2022;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class FencingEventListener implements Listener {
    private final JavaPlugin owner;
    private @Nullable UUID playerLeft;
    private @Nullable UUID playerRight;
    static final String kBossbarLeft = "sports_festival_2022_bossbar_left";
    static final String kBossbarRight = "sports_festival_2022_bossbar_right";

    FencingEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    enum State {
        IDLE,
        RUN,
        AWAIT_DEATH,
    }

    enum Team {
        LEFT,
        RIGHT,
    }

    private FencingEventListener.State state = FencingEventListener.State.IDLE;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {

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

    private @Nullable UUID getPlayerUid(Team team) {
        if (team == Team.RIGHT) {
            return playerRight;
        } else if (team == Team.LEFT) {
            return playerLeft;
        } else {
            return null;
        }
    }

    private void setPlayer(Player player, Team team) {
        if (team == Team.RIGHT) {
            if (player == null) {
                playerRight = null;
            } else if (playerLeft != null && playerLeft.equals(player.getUniqueId())) {
                broadcastUnofficial(ChatColor.RED + "[フェンシング] " + player.getName() + "は" + TeamName(Team.LEFT) + "としてエントリー済みです");
            } else {
                playerRight = player.getUniqueId();
                execute("give @p[name=\"" + player.getName() + "\"] iron_sword{Enchantments:[{id:knockback,lvl:10}]}");
                broadcast("[フェンシング] " + player.getName() + "がエントリーしました（" + TeamName(team) + "）");
            }
        } else if (team == Team.LEFT) {
            if (player == null) {
                playerLeft = null;
            } else if (playerRight != null && playerRight.equals(player.getUniqueId())) {
                broadcastUnofficial(ChatColor.RED + "[フェンシング] " + player.getName() + "は" + TeamName(Team.RIGHT) + "としてエントリー済みです");
            } else {
                playerLeft = player.getUniqueId();
                execute("give @p[name=\"" + player.getName() + "\"] iron_sword{Enchantments:[{id:knockback,lvl:10}]}");
                broadcast("[フェンシング] " + player.getName() + "がエントリーしました（" + TeamName(team) + "）");
            }
        }
    }

    private void broadcast(String message) {
        owner.getServer().broadcastMessage(message);
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String message) {
        broadcast(message);
    }

    private void onClickJoin(Location location, Team team) {
        UUID uid = getPlayerUid(team);
        if (uid != null) {
            //TODO: 既に join 済みの時のメッセージ
            return;
        }
        Player player = findNearest(location);
        if (player == null) {
            return;
        }
        setPlayer(player, team);
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

    private void onClickLeave(Location location, Team team) {
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
        setPlayer(null, team);
        broadcastUnofficial("[フェンシング] " + player.getName() + "がエントリー解除しました（" + TeamName(team) + "）");
    }

    private void onClickStart() {
        if (state != State.IDLE) {
            return;
        }
        Player left = null;
        Player right = null;
        int numLeft = 0;
        int numRight = 0;
        if (playerLeft != null) {
            left = getPlayer(playerLeft);
            if (left == null) {
                playerLeft = null;
            } else {
                numLeft += 1;
            }
        }
        if (playerRight != null) {
            right = getPlayer(playerRight);
            if (right == null) {
                playerRight = null;
            } else {
                numRight += 1;
            }
        }
        if (numLeft != numRight || numLeft < 1 || numRight < 1) {
            broadcast("参加人数が正しくありません（" + TeamName(Team.LEFT) + " : " + numLeft + "人、" + TeamName(Team.RIGHT) + " : " + numRight + "人）");
            return;
        }
        state = State.RUN;

        // 範囲内に居るプレイヤーを観客席側に排除する
        execute("tp @p[x=104,y=-18,z=-268,dx=61,dy=5,dz=4] 134 -17 -276");

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
        execute("bossbar add " + kBossbarLeft + " \"<<< LEFT SIDE <<<\"");
        execute("bossbar set " + kBossbarLeft + " max 3");
        execute("bossbar set " + kBossbarLeft + " value 3");
        execute("bossbar set " + kBossbarLeft + " color green");

        execute("bossbar remove " + kBossbarRight);
        execute("bossbar add " + kBossbarRight + " \">>> RIGHT SIDE >>>\"");
        execute("bossbar set " + kBossbarRight + " max 3");
        execute("bossbar set " + kBossbarRight + " value 3");
        execute("bossbar set " + kBossbarRight + " color green");

        execute("bossbar set " + kBossbarLeft + " players @a");
        execute("bossbar set " + kBossbarRight + " players @a");

        broadcast("");
        broadcast("[フェンシング] 競技を開始します！");
        broadcast("");
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