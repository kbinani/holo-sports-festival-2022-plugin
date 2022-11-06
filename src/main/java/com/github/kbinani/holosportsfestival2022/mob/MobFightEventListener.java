package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Loader;
import com.github.kbinani.holosportsfestival2022.Point3i;
import com.github.kbinani.holosportsfestival2022.WallSign;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MobFightEventListener implements Listener, StageDelegate {
    private final JavaPlugin owner;
    private boolean initialized = false;
    private final Map<TeamColor, Level> levels = new HashMap<>();
    private Map<TeamColor, Team> teams = new HashMap<>();
    private Status _status = Status.IDLE;

    public MobFightEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    void setStatus(Status s) {
        if (_status == s) {
            return;
        }
        _status = s;
        switch (_status) {
            case IDLE:
                resetField();
                break;
            case COUNTDOWN:
                break;
            case RUN:
                break;
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (initialized) {
            return;
        }
        initialized = true;
        resetField();
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onEntityDeathEvent(EntityDeathEvent e) {

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
        if (location.equals(offset(kButtonYellowJoinArrow))) {
            onClickJoin(player, TeamColor.YELLOW, Role.ARROW);
        } else if (location.equals(offset(kButtonYellowJoinSword))) {
            onClickJoin(player, TeamColor.YELLOW, Role.SWORD);
        } else if (location.equals(offset(kButtonRedJoinArrow))) {
            onClickJoin(player, TeamColor.RED, Role.ARROW);
        } else if (location.equals(offset(kButtonRedJoinSword))) {
            onClickJoin(player, TeamColor.RED, Role.SWORD);
        } else if (location.equals(offset(kButtonWhiteJoinArrow))) {
            onClickJoin(player, TeamColor.WHITE, Role.ARROW);
        } else if (location.equals(offset(kButtonWhiteJoinSword))) {
            onClickJoin(player, TeamColor.WHITE, Role.SWORD);
        } else if (location.equals(offset(kButtonYellowLeave)) || location.equals(offset(kButtonRedLeave)) || location.equals(offset(kButtonWhiteLeave))) {
            onClickLeave(player);
        }
    }

    void onClickJoin(Player player, TeamColor color, Role role) {
        if (_status != Status.IDLE) {
            return;
        }
        Participation current = getCurrentParticipation(player);
        if (current != null) {
            broadcast("[MOB討伐レース] %sは%sにエントリー済みです", player.getName(), ToColoredString(current.color));
            return;
        }
        Team team = ensureTeam(color);
        team.add(player, role);
        broadcast("[MOB討伐レース] %sが%s%sにエントリーしました", player.getName(), ToColoredString(color), ToString(role));
    }

    void onClickLeave(Player player) {
        Participation current = getCurrentParticipation(player);
        if (current == null) {
            return;
        }
        Team team = ensureTeam(current.color);
        team.remove(player);
        broadcast("[MOB討伐レース] %sがエントリー解除しました", player.getName());
        setStatus(Status.IDLE);
    }

    @Nonnull
    Level ensureLevel(TeamColor color) {
        Level level = levels.get(color);
        if (level == null) {
            level = newLevel(color);
            levels.put(color, level);
        }
        return level;
    }

    @Nonnull
    Team ensureTeam(TeamColor color) {
        Team team = teams.get(color);
        if (team == null) {
            team = new Team();
            teams.put(color, team);
        }
        return team;
    }

    @Nullable
    Participation getCurrentParticipation(Player player) {
        for (Map.Entry<TeamColor, Team> it : teams.entrySet()) {
            Role role = it.getValue().getCurrentRole(player);
            if (role == null) {
                continue;
            }
            return new Participation(it.getKey(), role);
        }
        return null;
    }

    private void resetField() {
        overworld().ifPresent(world -> {
            Loader.LoadChunk(world, offset(kBounds));
        });

        WallSign.Place(offset(kButtonWhiteLeave), BlockFace.SOUTH, "エントリー解除");
        WallSign.Place(offset(kButtonWhiteJoinArrow), BlockFace.SOUTH, "白組", "エントリー", "（弓）");
        WallSign.Place(offset(kButtonWhiteJoinSword), BlockFace.SOUTH, "白組", "エントリー", "（剣）");

        WallSign.Place(offset(kButtonRedLeave), BlockFace.SOUTH, "エントリー解除");
        WallSign.Place(offset(kButtonRedJoinArrow), BlockFace.SOUTH, "赤組", "エントリー", "（弓）");
        WallSign.Place(offset(kButtonRedJoinSword), BlockFace.SOUTH, "赤組", "エントリー", "（剣）");

        WallSign.Place(offset(kButtonYellowLeave), BlockFace.SOUTH, "エントリー解除");
        WallSign.Place(offset(kButtonYellowJoinArrow), BlockFace.SOUTH, "黃組", "エントリー", "（弓）");
        WallSign.Place(offset(kButtonYellowJoinSword), BlockFace.SOUTH, "黃組", "エントリー", "（剣）");

        for (TeamColor tc : kColors) {
            ensureLevel(tc);
        }
    }

    private Optional<World> overworld() {
        return owner.getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
    }

    private Point3i offset(Point3i p) {
        // 座標が間違っていたらここでオフセットする
        return new Point3i(p.x, p.y, p.z);
    }

    private double xd(double x) {
        // 座標が間違っていたらここでオフセットする
        return x;
    }

    private double yd(double y) {
        // 座標が間違っていたらここでオフセットする
        return y;
    }

    private double zd(double z) {
        // 座標が間違っていたらここでオフセットする
        return z;
    }

    private BoundingBox offset(BoundingBox box) {
        return new BoundingBox(xd(box.getMinX()), yd(box.getMinY()), zd(box.getMinZ()), xd(box.getMaxX()), yd(box.getMaxY()), zd(box.getMaxZ()));
    }

    private Level newLevel(TeamColor color) {
        switch (color) {
            case YELLOW:
                return new Level(offset(new Point3i(-9, -59, -254)), this);
            case RED:
                return new Level(offset(new Point3i(22, -59, -254)), this);
            case WHITE:
                return new Level(offset(new Point3i(53, -59, -254)), this);
        }
        return null;
    }

    @Override
    public void execute(String format, Object... args) {
        Server server = owner.getServer();
        server.dispatchCommand(server.getConsoleSender(), String.format(format, args));
    }

    private void broadcast(String msg, Object... args) {
        owner.getServer().broadcastMessage(String.format(msg, args));
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String msg, Object... args) {
        broadcast(msg, args);
    }

    static class Participation {
        final TeamColor color;
        final Role role;

        Participation(TeamColor color, Role role) {
            this.color = color;
            this.role = role;
        }
    }

    static String ToColoredString(TeamColor color) {
        switch (color) {
            case RED:
                return ChatColor.RED + "TEAM RED" + ChatColor.RESET;
            case WHITE:
                return ChatColor.GRAY + "TEAM WHITE" + ChatColor.RESET;
            case YELLOW:
                return ChatColor.YELLOW + "TEAM YELLOW" + ChatColor.RESET;
        }
        return "";
    }

    static String ToString(Role role) {
        switch (role) {
            case ARROW:
                return "（弓）";
            case SWORD:
                return "（剣）";
        }
        return "";
    }

    private static final BoundingBox kBounds = new BoundingBox(-26, -61, -424, 79, -19, -244);

    private static final TeamColor[] kColors = new TeamColor[]{TeamColor.RED, TeamColor.YELLOW, TeamColor.WHITE};

    private static final Point3i kButtonWhiteLeave = new Point3i(53, -59, -253);
    private static final Point3i kButtonWhiteJoinArrow = new Point3i(55, -59, -253);
    private static final Point3i kButtonWhiteJoinSword = new Point3i(57, -59, -253);

    private static final Point3i kButtonRedLeave = new Point3i(22, -59, -253);
    private static final Point3i kButtonRedJoinArrow = new Point3i(24, -59, -253);
    private static final Point3i kButtonRedJoinSword = new Point3i(26, -59, -253);

    private static final Point3i kButtonYellowLeave = new Point3i(-9, -59, -253);
    private static final Point3i kButtonYellowJoinArrow = new Point3i(-7, -59, -253);
    private static final Point3i kButtonYellowJoinSword = new Point3i(-5, -59, -253);
}