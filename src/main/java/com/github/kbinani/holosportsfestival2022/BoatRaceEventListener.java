package com.github.kbinani.holosportsfestival2022;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class BoatRaceEventListener implements Listener {
    private final JavaPlugin owner;

    enum Team {
        RED,
        WHITE,
        YELLOW,
    }

    enum Role {
        DRIVER,
        SHOOTER,
    }

    static String ToString(Role role) {
        if (role == Role.DRIVER) {
            return "（操縦担当）";
        } else {
            return "（妨害担当）";
        }
    }

    static String ToColoredString(Team team) {
        if (team == Team.RED) {
            return ChatColor.RED + "TEAM RED" + ChatColor.RESET;
        } else if (team == Team.YELLOW) {
            return ChatColor.YELLOW + "TEAM YELLOW" + ChatColor.RESET;
        } else {
            return ChatColor.GRAY + "TEAM WHITE" + ChatColor.RESET;
        }
    }

    class Participation {
        final Team team;
        final Role role;

        Participation(Team team, Role role) {
            this.team = team;
            this.role = role;
        }
    }

    class Participant {
        private @Nullable Player shooter;
        private @Nullable Player driver;

        void setPlayer(Role role, @Nullable Player player) {
            if (role == Role.DRIVER) {
                this.driver = player;
            } else {
                this.shooter = player;
            }
        }

        @Nullable
        Player getPlayer(Role role) {
            if (role == Role.DRIVER) {
                return driver;
            } else {
                return shooter;
            }
        }

        @Nullable
        Role getCurrentRole(@Nonnull Player player) {
            if (driver != null && driver.getUniqueId().equals(player.getUniqueId())) {
                return Role.DRIVER;
            }
            if (shooter != null && shooter.getUniqueId().equals(player.getUniqueId())) {
                return Role.SHOOTER;
            }
            return null;
        }
    }

    private final Map<Team, Participant> teams = new HashMap<>();

    private @Nonnull Participant ensureTeam(Team team) {
        Participant t = teams.get(team);
        if (t == null) {
            t = new Participant();
            teams.put(team, t);
        }
        return t;
    }

    private static final Point3i kYellowEntryShooter = new Point3i(-56, -59, -198);
    private static final Point3i kYellowEntryDriver = new Point3i(-56, -59, -200);
    private static final Point3i kWhiteEntryShooter = new Point3i(-56, -59, -202);
    private static final Point3i kWhiteEntryDriver = new Point3i(-56, -59, -204);
    private static final Point3i kRedEntryShooter = new Point3i(-56, -59, -206);
    private static final Point3i kRedEntryDriver = new Point3i(-56, -59, -208);
    private static final Point3i kLeaveButton = new Point3i(-56, -59, -210);

    BoatRaceEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {

    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Point3i location = new Point3i(block.getLocation());
        if (location.equals(offset(kYellowEntryShooter))) {
            onClickJoin(player, Team.YELLOW, Role.SHOOTER);
        } else if (location.equals(offset(kYellowEntryDriver))) {
            onClickJoin(player, Team.YELLOW, Role.DRIVER);
        } else if (location.equals(offset(kWhiteEntryShooter))) {
            onClickJoin(player, Team.WHITE, Role.SHOOTER);
        } else if (location.equals(offset(kWhiteEntryDriver))) {
            onClickJoin(player, Team.WHITE, Role.DRIVER);
        } else if (location.equals(offset(kRedEntryShooter))) {
            onClickJoin(player, Team.RED, Role.SHOOTER);
        } else if (location.equals(offset(kRedEntryDriver))) {
            onClickJoin(player, Team.RED, Role.DRIVER);
        } else if (location.equals(offset(kLeaveButton))) {
            onClickLeave(player);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onServerLoad(ServerLoadEvent e) {
        if (e.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }
        WallSign.Place(offset(kYellowEntryShooter), BlockFace.WEST, "黄組", "エントリー", ToString(Role.SHOOTER));
        WallSign.Place(offset(kYellowEntryDriver), BlockFace.WEST, "黄組", "エントリー", ToString(Role.DRIVER));
        WallSign.Place(offset(kWhiteEntryShooter), BlockFace.WEST, "白組", "エントリー", ToString(Role.SHOOTER));
        WallSign.Place(offset(kWhiteEntryDriver), BlockFace.WEST, "白組", "エントリー", ToString(Role.DRIVER));
        WallSign.Place(offset(kRedEntryShooter), BlockFace.WEST, "赤組", "エントリー", ToString(Role.SHOOTER));
        WallSign.Place(offset(kRedEntryDriver), BlockFace.WEST, "赤組", "エントリー", ToString(Role.DRIVER));
        WallSign.Place(offset(kLeaveButton), BlockFace.WEST, "エントリー解除");
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        @Nullable Participation current = getCurrentParticipation(player);
        if (current == null) {
            return;
        }
        onClickLeave(player);
    }

    private void broadcast(String msg) {
        owner.getServer().broadcastMessage(msg);
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String msg) {
        broadcast(msg);
    }

    private void onClickJoin(Player player, Team team, Role role) {
        @Nullable Participation current = getCurrentParticipation(player);
        if (current == null) {
            Participant p = ensureTeam(team);
            p.setPlayer(role, player);
            broadcast(String.format("[水上レース] %sが%s%sにエントリーしました", player.getName(), ToColoredString(team), ToString(role)));
        } else {
            broadcastUnofficial(ChatColor.RED + String.format("[水上レース] %sは%s%sで既にエントリー済みです", ChatColor.RESET + player.getName(), ToColoredString(team), ChatColor.RED + ToString(role)));
        }
    }

    private void onClickLeave(Player player) {
        @Nullable Participation current = getCurrentParticipation(player);
        if (current == null) {
            return;
        }
        Participant team = ensureTeam(current.team);
        team.setPlayer(current.role, null);
        broadcastUnofficial(String.format("[水上レース] %sが%s%sのエントリー解除しました", player.getName(), ToColoredString(current.team), ToString(current.role)));
    }

    private @Nullable Participation getCurrentParticipation(@Nonnull Player player) {
        for (Team team : teams.keySet()) {
            Participant participant = teams.get(team);
            if (participant == null) {
                continue;
            }
            @Nullable Role role = participant.getCurrentRole(player);
            if (role != null) {
                return new Participation(team, role);
            }
        }
        return null;
    }

    private Point3i offset(Point3i p) {
        // 座標が間違っていたらここでオフセットする
        return new Point3i(p.x, p.y, p.z);
    }

    private int x(int x) {
        // 座標が間違っていたらここでオフセットする
        return x;
    }

    private int y(int y) {
        // 座標が間違っていたらここでオフセットする
        return y;
    }

    private int z(int z) {
        // 座標が間違っていたらここでオフセットする
        return z;
    }
}