package com.github.kbinani.holosportsfestival2022;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class RelayEventListener implements Listener {
    private final JavaPlugin owner;

    enum TeamColor {
        RED,
        WHITE,
        YELLOW,
    }

    static class Team {
        private final Set<Player> participants = new HashSet<>();

        int getPlayerCount() {
            return (int) participants.stream().filter(Player::isOnline).count();
        }

        void add(Player player) {
            participants.add(player);
        }

        void remove(Player player) {
            participants.removeIf(it -> it.getUniqueId().equals(player.getUniqueId()));
        }

        boolean contains(Player player) {
            return participants.stream().anyMatch(it -> it.getUniqueId().equals(player.getUniqueId()));
        }
    }

    private final Map<TeamColor, Team> teams = new HashMap<>();

    enum Status {
        IDLE,
        AWAIT_START,
        RUN,
    }

    private Status _status = Status.IDLE;

    private void setStatus(Status s) {
        if (s == _status) {
            return;
        }
        _status = s;
        switch (_status) {
            case IDLE:
                resetField();
                break;
            case AWAIT_START:
                setEnableStartGate(true);
                stroke("birch_fence", kCorner1stInner);
                stroke("birch_fence", kCorner1stOuter);
                stroke("birch_fence", kCorner2ndInner);
                stroke("birch_fence", kCorner2ndOuter);
                stroke("birch_fence", kCorner3rdInner);
                stroke("birch_fence", kCorner3rdOuter);
                stroke("birch_fence", kCorner4thInner);
                stroke("birch_fence", kCorner4thOuter);
                break;
            case RUN:
                break;
        }
    }

    private void stroke(String block, Point3i... points) {
        for (int i = 0; i < points.length - 1; i++) {
            Point3i from = points[i];
            Point3i to = points[i + 1];
            execute("fill %s %s %s", xyz(from), xyz(to), block);
        }
    }

    RelayEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onServerLoad(ServerLoadEvent e) {
        if (e.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }
        resetField();
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onBlockRedstoneEvent(BlockRedstoneEvent e) {
        if (e.getOldCurrent() != 0 || e.getNewCurrent() <= 0) {
            return;
        }

        Location location = e.getBlock().getLocation();
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();

        if (bx == x(41) && by == y(-59) && bz == z(-183)) {
            // インフィールド
            onClickStart();
        } else if (bx == x(37) && by == y(-60) && bz == z(-177)) {
            // 第1レーン
            onClickStart();
        } else if (bx == x(38) && by == y(-60) && bz == z(-175)) {
            // 第2レーン
            onClickStart();
        } else if (bx == x(39) && by == y(-60) && bz == z(-173)) {
            // 第3レーン
            onClickStart();
        }
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
        if (location.equals(offset(kButtonEntryRed))) {
            onClickJoin(player, TeamColor.RED);
        } else if (location.equals(offset(kButtonEntryWhite))) {
            onClickJoin(player, TeamColor.WHITE);
        } else if (location.equals(offset(kButtonEntryYellow))) {
            onClickJoin(player, TeamColor.YELLOW);
        } else if (location.equals(offset(kButtonLeave))) {
            onClickLeave(player);
        }
    }

    private void resetField() {
        WallSign.Place(offset(kButtonEntryRed), BlockFace.NORTH, "赤組", "エントリー");
        WallSign.Place(offset(kButtonEntryWhite), BlockFace.NORTH, "白組", "エントリー");
        WallSign.Place(offset(kButtonEntryYellow), BlockFace.NORTH, "黃組", "エントリー");
        WallSign.Place(offset(kButtonLeave), BlockFace.NORTH, "エントリー解除");

        setEnableStartGate(false);

        stroke("air", kCorner1stInner);
        stroke("air", kCorner1stOuter);
        stroke("air", kCorner2ndInner);
        stroke("air", kCorner2ndOuter);
        stroke("air", kCorner3rdInner);
        stroke("air", kCorner3rdOuter);
        stroke("air", kCorner4thInner);
        stroke("air", kCorner4thOuter);
    }

    private void setEnableStartGate(boolean enable) {
        if (enable) {
            // 第1レーン
            execute("fill %s %s birch_fence", xyz(37, -60, -178), xyz(38, -60, -176));
            execute("setblock %s command_block[facing=north]", xyz(37, -61, -177));
            execute("setblock %s birch_button[face=floor,facing=east]", xyz(37, -60, -177));

            // 第2レーン
            execute("fill %s %s birch_fence", xyz(38, -60, -176), xyz(39, -60, -174));
            execute("setblock %s command_block[facing=north]", xyz(38, -61, -175));
            execute("setblock %s birch_button[face=floor,facing=east]", xyz(38, -60, -175));

            // 第3レーン
            execute("fill %s %s birch_fence", xyz(39, -60, -174), xyz(40, -60, -172));
            execute("setblock %s command_block[facing=north]", xyz(39, -61, -173));
            execute("setblock %s birch_button[face=floor,facing=east]", xyz(39, -60, -173));
        } else {
            execute("fill %s %s air", xyz(37, -60, -178), xyz(40, -60, -172));
            execute("fill %s %s dirt_path", xyz(40, -61, -172), xyz(37, -61, -178));
        }
    }

    private void onClickJoin(Player player, TeamColor teamColor) {
        if (_status != Status.IDLE && _status != Status.AWAIT_START) {
            return;
        }
        AtomicBoolean ok = new AtomicBoolean(true);
        useTeams((team, color) -> {
            if (team.contains(player)) {
                broadcastUnofficial(ChatColor.RED + "%sは既に%sにエントリー済みです", player.getName(), ToString(color));
                ok.set(false);
            }
            return null;
        });
        if (!ok.get()) {
            return;
        }
        Team team = ensureTeam(teamColor);
        team.add(player);
        broadcast("[リレー] %sが%sにエントリーしました", player.getName(), ToColoredString(teamColor));
        setStatus(Status.AWAIT_START);
    }

    private Team ensureTeam(TeamColor teamColor) {
        Team team;
        if (teams.containsKey(teamColor)) {
            team = teams.get(teamColor);
        } else {
            team = new Team();
            teams.put(teamColor, team);
        }
        return team;
    }

    private int getPlayerCount() {
        AtomicInteger count = new AtomicInteger(0);
        useTeams((team, color) -> {
            count.addAndGet(team.getPlayerCount());
            return null;
        });
        return count.get();
    }

    private void onClickLeave(Player player) {
        useTeams((team, color) -> {
            team.remove(player);
            return null;
        });
        if (getPlayerCount() < 1) {
            setStatus(Status.IDLE);
        }
    }

    private void onClickStart() {

    }

    private void useTeams(BiFunction<Team, TeamColor, Void> callback) {
        TeamColors(color -> {
            Team team = ensureTeam(color);
            callback.apply(team, color);
        });
    }

    static String ToColoredString(TeamColor color) {
        switch (color) {
            case WHITE:
                return ChatColor.GRAY + "TEAM WHITE" + ChatColor.RESET;
            case RED:
                return ChatColor.RED + "TEAM RED" + ChatColor.RESET;
            case YELLOW:
                return ChatColor.YELLOW + "TEAM YELLOW" + ChatColor.RESET;
            default:
                return "";
        }
    }

    static String ToString(TeamColor color) {
        switch (color) {
            case WHITE:
                return "白組";
            case RED:
                return "赤組";
            case YELLOW:
                return "黄組";
            default:
                return "";
        }
    }

    static void TeamColors(Consumer<TeamColor> callback) {
        callback.accept(TeamColor.RED);
        callback.accept(TeamColor.WHITE);
        callback.accept(TeamColor.YELLOW);
    }

    private void broadcast(String msg, Object... args) {
        owner.getServer().broadcastMessage(String.format(msg, args));
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String msg, Object... args) {
        broadcast(msg, args);
    }

    private String xyz(Point3i p) {
        // 座標が間違っていてもここはオフセットしなくていい
        Point3i o = offset(p);
        return String.format("%d %d %d", o.x, o.y, o.z);
    }

    private String xyz(int x, int y, int z) {
        // 座標が間違っていたらここでオフセットする
        return String.format("%d %d %d", x, y, z);
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

    private void execute(String format, Object... args) {
        Server server = owner.getServer();
        CommandSender sender = server.getConsoleSender();
        server.dispatchCommand(sender, String.format(format, args));
    }

    private static final Point3i[] kCorner1stInner = new Point3i[]{
            new Point3i(58, -60, -179),
            new Point3i(62, -60, -179),
            new Point3i(62, -60, -180),
            new Point3i(63, -60, -180),
            new Point3i(63, -60, -181),
            new Point3i(64, -60, -181),
            new Point3i(64, -60, -185),
    };
    private static final Point3i[] kCorner1stOuter = new Point3i[]{
            new Point3i(58, -60, -171),
            new Point3i(70, -60, -171),
            new Point3i(70, -60, -172),
            new Point3i(71, -60, -172),
            new Point3i(71, -60, -173),
            new Point3i(72, -60, -173),
            new Point3i(72, -60, -185),
    };
    private static final Point3i[] kCorner2ndInner = new Point3i[]{
            new Point3i(64, -60, -220),
            new Point3i(64, -60, -224),
            new Point3i(63, -60, -224),
            new Point3i(63, -60, -225),
            new Point3i(62, -60, -225),
            new Point3i(62, -60, -226),
            new Point3i(58, -60, -226),
    };
    private static final Point3i[] kCorner2ndOuter = new Point3i[]{
            new Point3i(72, -60, -220),
            new Point3i(72, -60, -232),
            new Point3i(71, -60, -232),
            new Point3i(71, -60, -233),
            new Point3i(70, -60, -233),
            new Point3i(70, -60, -234),
            new Point3i(58, -60, -234),
    };
    private static final Point3i[] kCorner3rdInner = new Point3i[]{
            new Point3i(15, -60, -226),
            new Point3i(11, -60, -226),
            new Point3i(11, -60, -225),
            new Point3i(10, -60, -225),
            new Point3i(10, -60, -224),
            new Point3i(9, -60, -224),
            new Point3i(9, -60, -220),
    };
    private static final Point3i[] kCorner3rdOuter = new Point3i[]{
            new Point3i(15, -60, -234),
            new Point3i(3, -60, -234),
            new Point3i(3, -60, -233),
            new Point3i(2, -60, -233),
            new Point3i(2, -60, -232),
            new Point3i(1, -60, -232),
            new Point3i(1, -60, -220),
    };
    private static final Point3i[] kCorner4thInner = new Point3i[]{
            new Point3i(9, -60, -185),
            new Point3i(9, -60, -181),
            new Point3i(10, -60, -181),
            new Point3i(10, -60, -180),
            new Point3i(11, -60, -180),
            new Point3i(11, -60, -179),
            new Point3i(15, -60, -179),
    };
    private static final Point3i[] kCorner4thOuter = new Point3i[]{
            new Point3i(1, -60, -185),
            new Point3i(1, -60, -173),
            new Point3i(2, -60, -173),
            new Point3i(2, -60, -172),
            new Point3i(3, -60, -172),
            new Point3i(3, -60, -171),
            new Point3i(15, -60, -171),
    };

    private static final Point3i kButtonEntryRed = new Point3i(39, -60, -184);
    private static final Point3i kButtonEntryWhite = new Point3i(37, -60, -184);
    private static final Point3i kButtonEntryYellow = new Point3i(35, -60, -184);
    private static final Point3i kButtonLeave = new Point3i(33, -60, -184);
}