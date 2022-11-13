package com.github.kbinani.holosportsfestival2022;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//TODO: 金リンゴをコレクションできないように対策する

public class DarumaEventListener implements Listener, Announcer {
    private final JavaPlugin owner;
    private boolean initialized = false;
    private Status _status = Status.IDLE;
    private @Nullable Race race;

    enum Status {
        IDLE,
        COUNTDOWN,
        // 手動で「だるまさんが...」ボタン, 「ころんだ!!!」ボタンをそれぞれ押すモードで競技が進行中
        RUN_MANUAL,
        // プラグイン側が自動で進行させるモードで競技が進行中
        RUN_AUTOMATIC,
    }

    enum TeamColor {
        RED,
        YELLOW,
        WHITE,
    }

    static class Team {
        private final Map<UUID, Player> players = new HashMap<>();

        boolean contains(Player player) {
            return players.containsKey(player.getUniqueId());
        }

        void add(Player player) {
            players.put(player.getUniqueId(), player);
        }

        void remove(Player player) {
            players.remove(player.getUniqueId());
        }

        int getPlayerCount() {
            return players.size();
        }

        void eachPlayer(Consumer<Player> callback) {
            players.values().forEach(callback);
        }
    }

    static interface Foo {
    }

    static class Race {
        static class Goal {
            final Player player;
            final double tick;

            Goal(Player player, double tick) {
                this.player = player;
                this.tick = tick;
            }
        }

        private final List<Goal> order = new LinkedList<>();
        private final Set<UUID> finished = new HashSet<>();
        private final Set<UUID> running = new HashSet<>();
        // ゴール判定の結果順位が変わる可能性がある.
        // なので本人に通知した後に順位が変動した場合再アナウンスできるよう, アナウンスした順位を覚えておく.
        private final Map<UUID, Integer> announcedOrder = new HashMap<>();

        void goal(Player player, double tick) {
            UUID uuid = player.getUniqueId();
            if (finished.contains(uuid)) {
                return;
            }
            order.add(new Goal(player, tick));
            finished.add(uuid);
            running.remove(uuid);
        }

        void participate(Player player) {
            running.add(player.getUniqueId());
        }

        void withdraw(Player player) {
            running.remove(player.getUniqueId());
        }

        int getRunningPlayerCount() {
            return running.size();
        }

        boolean isRunning(Player player) {
            return running.contains(player.getUniqueId());
        }

        void announceOrder(Announcer announcer, Player player) {
            order.sort(Comparator.comparingDouble(a -> a.tick));
            enumerateInOrder((order, p) -> {
                if (p.getUniqueId().equals(player.getUniqueId())) {
                    announcer.broadcast("%sが %d位 でクリア！", player.getName(), order);
                    announcedOrder.put(player.getUniqueId(), order);
                } else {
                    Integer prev = announcedOrder.get(p.getUniqueId());
                    if (prev != null && order != prev) {
                        announcer.broadcastUnofficial("判定の結果 %s の順位が %d位 から %d位 に変わりました", p.getName(), prev, order);
                        announcedOrder.put(p.getUniqueId(), order);
                    }
                }
            });
        }

        void announceOrders(Announcer announcer) {
            if (running.size() == 0) {
                announcer.broadcast("");
                announcer.broadcast("-----------------------");
                announcer.broadcast("[試合終了]");
                enumerateInOrder((order, player) -> {
                    announcer.broadcast("%d位 : %s", order, player.getName());
                });
                announcer.broadcast("-----------------------");
                announcer.broadcast("");
            }
        }

        private void enumerateInOrder(BiConsumer<Integer, Player> action) {
            double prevTick = -1;
            int currentOrder = 0;
            for (int i = 0; i < order.size(); i++) {
                Goal goal = order.get(i);
                if (prevTick < goal.tick) {
                    currentOrder++;
                }
                prevTick = goal.tick;
                action.accept(currentOrder, goal.player);
            }
        }
    }

    private final Map<TeamColor, Team> teams = new HashMap<>();

    DarumaEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!initialized) {
            initialized = true;
            resetField();
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        onClickLeave(player);
        if (getPlayerCount() < 1) {
            // 参加者の最後の一人がログアウトした. 会場をリセットする
            setStatus(Status.IDLE);
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
        if (location.equals(offset(kButtonYellowJoin))) {
            onClickJoin(player, TeamColor.YELLOW);
        } else if (location.equals(offset(kButtonRedJoin))) {
            onClickJoin(player, TeamColor.RED);
        } else if (location.equals(offset(kButtonWhiteJoin))) {
            onClickJoin(player, TeamColor.WHITE);
        } else if (location.equals(offset(kButtonLeave))) {
            onClickLeave(player);
        } else if (location.equals(offset(kButtonStartFinal))) {
            // 決勝. 各チームの人数が一緒になっているか検証してからスタートするモードなのかも
            onClickStart(player);
        } else if (location.equals(offset(kButtonStartPreliminary))) {
            // 予選
            onClickStart(player);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerMove(PlayerMoveEvent e) {
        if (race == null) {
            return;
        }
        if (_status != Status.RUN_AUTOMATIC && _status != Status.RUN_MANUAL) {
            return;
        }
        Player player = e.getPlayer();
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        if (!race.isRunning(player)) {
            return;
        }
        BoundingBox box = offset(kGoalDetectionBox);
        if (box.contains(player.getLocation().toVector())) {
            // ゴールラインを超えた時刻を計算する.
            double z = box.getMaxZ();
            double fromZ = e.getFrom().getZ();
            double toZ = player.getLocation().getZ();
            double tick = world.getFullTime();
            if (fromZ != toZ) {
                tick = (z - fromZ) / (toZ - fromZ) + world.getFullTime() - 1;
            }
            race.goal(player, tick);

            // 同一 tick で box に侵入したという判定になったとしても,
            // 駆け込んだ時の速度によってはゴールラインを超えた時刻は他の人の方が早いかもしれない.
            // 1 tick 待ってから順位を発表する.
            BukkitScheduler scheduler = owner.getServer().getScheduler();
            scheduler.runTask(owner, () -> {
                if (race == null) {
                    return;
                }
                race.announceOrder(this, player);
                if (race.getRunningPlayerCount() < 1) {
                    race.announceOrders(this);
                    setStatus(Status.IDLE);
                }
            });
        }
    }

    private void setStatus(Status status) {
        if (_status == status) {
            return;
        }
        _status = status;
        switch (_status) {
            case IDLE:
                setEntranceOpened(true);
                setStartGateOpened(false);
                clearItem("@a");
                race = null;
                break;
            case COUNTDOWN:
                setEntranceOpened(false);
                setStartGateOpened(false);
                break;
            case RUN_MANUAL:
            case RUN_AUTOMATIC:
                setEntranceOpened(false);
                setStartGateOpened(true);
                break;
        }
    }

    private void onClickJoin(Player player, TeamColor color) {
        if (_status != Status.IDLE) {
            return;
        }
        TeamColor current = getCurrentColor(player);
        if (current == null) {
            Team team = ensureTeam(color);
            team.add(player);
            giveItem(player);
            broadcast("[だるまさんがころんだ] %sが%sにエントリーしました", player.getName(), ToColoredString(color));
        } else {
            broadcast("[だるまさんがころんだ] %sは%sにエントリー済みです", player.getName(), ToColoredString(current));
        }
    }

    //TODO: 予選と決勝で処理を分ける
    private void onClickStart(Player player) {
        if (!player.isOp()) {
            return;
        }
        if (_status != Status.IDLE) {
            return;
        }
        int total = getPlayerCount();
        if (total < 1) {
            broadcastUnofficial("[だるまさんがころんだ] 参加者が見つかりません");
            return;
        }
        broadcast("");
        broadcast("-----------------------");
        for (Map.Entry<TeamColor, Team> it : teams.entrySet()) {
            Team team = it.getValue();
            TeamColor color = it.getKey();
            int count = team.getPlayerCount();
            if (count < 1) {
                broadcast("%sの参加者が見つかりません", ToString(color));
            } else {
                broadcast("%s が競技に参加します（参加者%d人）", ToColoredString(color), count);
            }
        }
        broadcast("-----------------------");
        broadcast("");
        setStatus(Status.COUNTDOWN);
        Countdown.Then(getAnnounceBounds(), owner, (count) -> _status == Status.COUNTDOWN, () -> {
            if (_status != Status.COUNTDOWN) {
                return false;
            }
            final Race race = new Race();
            for (Team team : teams.values()) {
                team.eachPlayer(race::participate);
            }
            this.race = race;
            setStatus(Status.RUN_MANUAL);
            return true;
        });
    }

    private int getPlayerCount() {
        int total = 0;
        for (Team team : teams.values()) {
            total += team.getPlayerCount();
        }
        return total;
    }

    private BoundingBox getAnnounceBounds() {
        return offset(kAnnounceBounds);
    }

    static String ToString(TeamColor color) {
        switch (color) {
            case RED:
                return "TEAM RED";
            case WHITE:
                return "TEAM WHITE";
            case YELLOW:
                return "TEAM YELLOW";
        }
        return "";
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

    @Override
    public void broadcast(String format, Object... args) {
        String msg = String.format(format, args);
        execute("tellraw @a[%s] \"%s\"", TargetSelector.Of(getAnnounceBounds()), msg);
    }

    @Override
    public void broadcastUnofficial(String msg, Object... args) {
        broadcast(msg, args);
    }

    private @Nonnull Team ensureTeam(TeamColor color) {
        Team team = teams.get(color);
        if (team == null) {
            team = new Team();
            teams.put(color, team);
        }
        return team;
    }

    @Nullable
    private TeamColor getCurrentColor(Player player) {
        for (Map.Entry<TeamColor, Team> it : teams.entrySet()) {
            if (it.getValue().contains(player)) {
                return it.getKey();
            }
        }
        return null;
    }

    private void onClickLeave(Player player) {
        TeamColor current = getCurrentColor(player);
        if (current == null) {
            return;
        }
        Team team = ensureTeam(current);
        team.remove(player);
        clearItem(String.format("@p[name=\"%s\"]", player.getName()));
        if (race != null) {
            race.withdraw(player);
        }
        broadcast("[だるまさんがころんだ] %sがエントリー解除しました", player.getName());
    }

    private void clearItem(String selector) {
        execute("clear %s golden_apple{tag:{%s:1b}}", selector, kItemTag);
    }

    private void giveItem(Player player) {
        execute("give @p[name=\"%s\"] golden_apple{tag:{%s:1b}}", player.getName(), kItemTag);
    }

    private void resetField() {
        WallSign.Place(offset(kButtonLeave), BlockFace.SOUTH, "エントリー解除");
        WallSign.Place(offset(kButtonRedJoin), BlockFace.SOUTH, "赤組", "エントリー");
        WallSign.Place(offset(kButtonWhiteJoin), BlockFace.SOUTH, "白組", "エントリー");
        WallSign.Place(offset(kButtonYellowJoin), BlockFace.SOUTH, "黃組", "エントリー");
        setStartGateOpened(false);
        setEntranceOpened(true);
    }

    private void setStartGateOpened(boolean open) {
        stroke(new Point3i(104, -59, -123), new Point3i(144, -59, -123), open ? "air" : "iron_bars");
    }

    private void setEntranceOpened(boolean open) {
        String block = open ? "air" : "iron_bars";
        stroke(new Point3i(103, -58, -118), new Point3i(145, -58, -118), block);
        stroke(new Point3i(144, -59, -118), new Point3i(104, -59, -118), block);
    }

    private void stroke(Point3i from, Point3i to, String block) {
        Point3i f = offset(from);
        Point3i t = offset(to);
        execute("fill %s %s %s %s %s %s %s", f.x, f.y, f.z, t.x, t.y, t.z, block);
    }

    public void execute(String format, Object... args) {
        Server server = owner.getServer();
        server.dispatchCommand(server.getConsoleSender(), String.format(format, args));
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

    private static final Point3i kButtonLeave = new Point3i(105, -60, -121);
    private static final Point3i kButtonRedJoin = new Point3i(107, -60, -121);
    private static final Point3i kButtonWhiteJoin = new Point3i(109, -60, -121);
    private static final Point3i kButtonYellowJoin = new Point3i(111, -60, -121);
    // デバッグ用. 本家ではボタン押す形式だけど, ボタンだと誰でも押せてしまう. 誰でもスタートできるとマズいので看板右クリックの形式にする.
    private static final Point3i kButtonStartPreliminary = new Point3i(128, -53, -229);
    // デバッグ用. 本家ではボタン押す形式だけど, ボタンだと誰でも押せてしまう. 誰でもスタートできるとマズいので看板右クリックの形式にする.
    private static final Point3i kButtonStartFinal = new Point3i(126, -53, -229);

    private static final BoundingBox kAnnounceBounds = new BoundingBox(96, -60, -240, 152, -30, -106);
    private static final BoundingBox kGoalDetectionBox = new BoundingBox(104, -56, -228, 145, -53, -223);

    private static final String kItemTag = "hololive_sports_festival_2022_daruma";
}