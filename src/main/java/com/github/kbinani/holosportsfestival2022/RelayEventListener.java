package com.github.kbinani.holosportsfestival2022;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
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
        private final List<Player> order = new LinkedList<>();

        int getPlayerCount() {
            return (int) participants.stream().filter(Player::isOnline).count();
        }

        void add(@Nonnull Player player) {
            participants.add(player);
        }

        void remove(@Nonnull Player player) {
            participants.removeIf(it -> it.getUniqueId().equals(player.getUniqueId()));
        }

        boolean contains(@Nonnull Player player) {
            return participants.stream().anyMatch(it -> it.getUniqueId().equals(player.getUniqueId()));
        }

        void pushRunner(@Nonnull Player player) {
            if (!contains(player)) {
                return;
            }
            order.add(player);
        }

        @Nullable
        Player getCurrentRunner() {
            if (order.isEmpty()) {
                return null;
            }
            return order.stream().skip(order.size() - 1).findFirst().orElse(null);
        }

        void clearOrder() {
            order.clear();
        }
    }

    private final Map<TeamColor, Team> teams = new HashMap<>();

    static class Race {
        final int numberOfLaps;

        Race(int numberOfLaps) {
            this.numberOfLaps = numberOfLaps;
        }
    }

    enum Status {
        IDLE,
        AWAIT_START,
        COUNTDOWN,
        RUN,
    }

    private Status _status = Status.IDLE;
    private @Nullable Race race;

    private void setStatus(Status s) {
        if (s == _status) {
            return;
        }
        _status = s;
        switch (_status) {
            case IDLE:
                resetField();
                clearBatons("@a");
                race = null;
                break;
            case AWAIT_START:
                setEnableStartGate(true);
                setEnableCornerFence(true);
                clearBatons("@a");
                break;
            case COUNTDOWN:
                break;
            case RUN:
                setEnableStartGate(false);
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

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (_status != Status.RUN && _status != Status.COUNTDOWN) {
            return;
        }
        Player player = e.getPlayer();
        TeamColor color = getCurrentTeam(player);
        if (color == null) {
            return;
        }
        onClickLeave(player);
        if (getPlayerCount() > 0) {
            setStatus(Status.AWAIT_START);
        } else {
            setStatus(Status.IDLE);
        }
    }

    private void resetField() {
        WallSign.Place(offset(kButtonEntryRed), BlockFace.NORTH, "赤組", "エントリー");
        WallSign.Place(offset(kButtonEntryWhite), BlockFace.NORTH, "白組", "エントリー");
        WallSign.Place(offset(kButtonEntryYellow), BlockFace.NORTH, "黃組", "エントリー");
        WallSign.Place(offset(kButtonLeave), BlockFace.NORTH, "エントリー解除");

        setEnableStartGate(false);
        setEnableCornerFence(false);
    }

    private void setEnableCornerFence(boolean enable) {
        String block;
        if (enable) {
            block = "birch_fence";
        } else {
            block = "air";
        }
        stroke(block, kCorner1stInner);
        stroke(block, kCorner1stOuter);
        stroke(block, kCorner2ndInner);
        stroke(block, kCorner2ndOuter);
        stroke(block, kCorner3rdInner);
        stroke(block, kCorner3rdOuter);
        stroke(block, kCorner4thInner);
        stroke(block, kCorner4thOuter);
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
        teams.forEach((color, team) -> {
            if (team.contains(player)) {
                broadcast("%sは%sにエントリー済みです", player.getName(), ToColoredString(color));
                ok.set(false);
            }
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
        teams.forEach((color, team) -> {
            count.addAndGet(team.getPlayerCount());
        });
        return count.get();
    }

    private void onClickLeave(Player player) {
        TeamColor color = getCurrentTeam(player);
        if (color == null) {
            return;
        }
        Team team = ensureTeam(color);
        team.remove(player);
        broadcast("[リレー] %sがエントリー解除しました", player.getName());
        clearBatons(player.getName());

        // チーム競技なので人数が減ったら強制的にノーコンテストにする
        setStatus(Status.IDLE);
    }

    private void clearBatons(String selector) {
        execute("clear %s blaze_rod{tag:{%s:1b}}", selector, kItemTag);
    }

    private void giveBaton(Player player) {
        execute("give %s blaze_rod{tag:{%s:1b},display:{Name:'[{\"text\":\"バトン\"}]'}}", player.getName(), kItemTag);
    }

    private void onClickStart() {
        if (_status != Status.AWAIT_START) {
            return;
        }
        AtomicBoolean canStart = new AtomicBoolean(false);
        teams.forEach((color, team) -> {
            int count = getPlayerCount();
            if (count < 1) {
                return;
            } else if (count == 1) {
                broadcastUnofficial(ChatColor.RED + "[リレー] 参加者が足りません");
            } else {
                canStart.set(true);
            }
        });
        if (!canStart.get()) {
            return;
        }

        // 第一走者を検出する
        Map<TeamColor, Player> firstRunners = new HashMap<>();
        World world = overworld().orElse(null);
        if (world == null) {
            return;
        }
        Player[] lanes = new Player[]{null, null, null};
        BoundingBox firstLane = new BoundingBox(xd(37.5), yd(-60), zd(-177.5), xd(38.5), yd(-58), zd(-175.5));
        BoundingBox secondLane = new BoundingBox(xd(38.5), yd(-60), zd(-175.5), xd(39.5), yd(-58), zd(-173.5));
        BoundingBox thirdLane = new BoundingBox(xd(39.5), yd(-60), zd(-173.5), xd(40.5), yd(-58), zd(-171.5));
        BoundingBox[] laneBoundingBox = new BoundingBox[]{firstLane, secondLane, thirdLane};
        for (int i = 0; i < 3; i++) {
            BoundingBox box = laneBoundingBox[i];
            Collection<Entity> entities = world.getNearbyEntities(box, it -> it.getType() == EntityType.PLAYER);
            if (entities.isEmpty()) {
                continue;
            }
            if (entities.size() > 1) {
                broadcastUnofficial(ChatColor.RED + "[リレー] 一つのゲートに複数人入っています");
                return;
            }
            Player player = (Player) entities.stream().findFirst().get();
            TeamColor tc = getCurrentTeam(player);
            if (tc == null) {
                broadcastUnofficial(ChatColor.RED + "[リレー] ゲートに競技者でないプレイヤーが入っています");
                return;
            }
            if (firstRunners.containsKey(tc)) {
                broadcastUnofficial(ChatColor.RED + "[リレー] 同じチームの人が複数人ゲートに入っています");
                return;
            }
            firstRunners.put(tc, player);
            lanes[i] = player;
        }
        AtomicBoolean isReady = new AtomicBoolean(true);
        teams.forEach((color, team) -> {
            if (team.getPlayerCount() < 2) {
                // リレーなので 1 回はバトンパスが必要, ということにする
                return;
            }
            if (!firstRunners.containsKey(color)) {
                broadcast("%sの第一走者はスタート位置についてください！", ToColoredString(color));
                isReady.set(false);
            }
        });
        if (!isReady.get()) {
            return;
        }

        broadcast("");
        broadcast("-----------------------");
        AtomicInteger numberOfLaps = new AtomicInteger(0);
        teams.forEach((color, team) -> {
            int c = team.getPlayerCount();
            if (c < 2) {
                // リレーなので 1 回はバトンパスが必要, ということにする
                return;
            }
            // 最も参加人数が多いチームの人数を周回数とする. 人数が足りないチームは
            numberOfLaps.set(Math.max(c, numberOfLaps.get()));
            broadcast("%s が競技に参加します（参加者%d人）", ToColoredString(color), c);
        });
        broadcast("-----------------------");
        broadcast("");
        broadcast("[リレー] 競技を開始します！");
        broadcast("");
        setStatus(Status.COUNTDOWN);
        Countdown.Then(getBounds(), owner, c -> _status == Status.COUNTDOWN, () -> {
            if (_status != Status.COUNTDOWN) {
                return false;
            }
            boolean ok = true;
            for (int i = 0; i < lanes.length; i++) {
                Player runner = lanes[i];
                if (runner == null) {
                    continue;
                }
                BoundingBox box = laneBoundingBox[i];
                Location location = runner.getLocation();
                if (!box.contains(location.getX(), location.getY(), location.getZ())) {
                    ok = false;
                    broadcastUnofficial("[リレー] %sがゲート内に入っていません", runner.getName());
                }
            }
            if (!ok) {
                setStatus(Status.AWAIT_START);
                return false;
            }

            race = new Race(numberOfLaps.get());
            teams.forEach((color, team) -> {
                team.clearOrder();
            });
            firstRunners.forEach((teamColor, runner) -> {
                execute("give %s blaze_rod{tag:{%s:1b},display:{Name:'[{\"text\":\"バトン\"}]'}}", runner.getName(), kItemTag);
                broadcast("%s 第一走者 : %sがスタート！", ToColoredString(teamColor), runner.getName());
                Team team = ensureTeam(teamColor);
                team.pushRunner(runner);
            });
            setStatus(Status.RUN);
            return true;
        });
    }

    private TeamColor getCurrentTeam(@Nonnull Player player) {
        AtomicReference<TeamColor> color = new AtomicReference<>(null);
        teams.forEach((teamColor, team) -> {
            if (team.contains(player)) {
                color.set(teamColor);
            }
        });
        return color.get();
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (_status != Status.RUN) {
            return;
        }
        Entity damagerEntity = e.getDamager();
        Entity entity = e.getEntity();
        if (!(damagerEntity instanceof Player) || !(entity instanceof Player)) {
            return;
        }

        //TODO: バトンで殴ったかどうか確認するのが良さそう

        //TODO: 最終走者だったらこの処理要らない

        // 両者がバトンパス領域に入っているかどうか確かめる
        BoundingBox box = getBatonPassingArea();
        Player from = (Player) damagerEntity;
        Player to = (Player) entity;
        if (!box.contains(from.getLocation().toVector()) || !box.contains(to.getLocation().toVector())) {
            return;
        }
        TeamColor teamColor = getCurrentTeam(from);
        if (teamColor == null) {
            return;
        }
        {
            TeamColor tc = getCurrentTeam(to);
            if (tc == null || tc != teamColor) {
                return;
            }
        }

        // 殴った人がチームの現在の走者かどうかを確かめる
        Team team = ensureTeam(teamColor);
        Player currentRunner = team.getCurrentRunner();
        if (currentRunner == null) {
            return;
        }
        if (!currentRunner.getUniqueId().equals(from.getUniqueId())) {
            return;
        }

        // バトンパスする
        team.pushRunner(to);
        clearBatons(from.getName());
        giveBaton(to);
    }

    private boolean isInField(Player player) {
        Location location = player.getLocation();
        if (!player.isOnline()) {
            return false;
        }
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            return false;
        }
        BoundingBox box = getBounds();
        return box.contains(location.getX(), location.getY(), location.getZ());
    }

    private BoundingBox getBounds() {
        return new BoundingBox(x(-2), y(-61), z(-241), x(82), y(384), z(-115));
    }

    private BoundingBox getBatonPassingArea() {
        // 広めに. 進行方向手前 8 ブロック, 進行方向に 16 ブロック
        return new BoundingBox(xd(36), yd(-61), zd(-179), xd(56), yd(-58), zd(-170));
    }

    private void useTeams(BiConsumer<TeamColor, Team> callback) {
        TeamColors(color -> {
            Team team = ensureTeam(color);
            callback.accept(color, team);
        });
    }

    private Optional<World> overworld() {
        return owner.getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
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

    private static final String kItemTag = "hololive_sports_festival_2022_relay";
}