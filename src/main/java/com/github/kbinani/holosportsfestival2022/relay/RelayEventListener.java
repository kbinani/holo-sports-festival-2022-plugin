package com.github.kbinani.holosportsfestival2022.relay;

import com.github.kbinani.holosportsfestival2022.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RelayEventListener implements Listener, Competition {
    private final long loadDelay;
    private final MainDelegate delegate;

    private final Map<TeamColor, Team> teams = new HashMap<>();

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
                Bukkit.getServer().getOnlinePlayers().forEach(this::clearBatons);
                race = null;
                break;
            case AWAIT_START:
                setEnableStartGate(true);
                setEnableCornerFence(true);
                Bukkit.getServer().getOnlinePlayers().forEach(this::clearBatons);
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
            Editor.Fill(offset(from), offset(to), block);
        }
    }

    public RelayEventListener(MainDelegate delegate, long loadDelay) {
        this.loadDelay = loadDelay;
        this.delegate = delegate;
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
        if (_status != Status.RUN && _status != Status.COUNTDOWN && _status != Status.AWAIT_START) {
            return;
        }
        Player player = e.getPlayer();
        TeamColor color = getCurrentTeam(player);
        if (color == null) {
            return;
        }
        onClickLeave(player);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        TeamColor color = getCurrentTeam(player);
        if (color == null) {
            return;
        }
        Vector location = player.getLocation().toVector();
        if (!getAnnounceBounds().contains(location)) {
            player.sendMessage(ChatColor.RED + "[リレー] 場外に出たためエントリー解除となります");
            onClickLeave(player);
            return;
        }
        if (_status != Status.RUN || race == null) {
            return;
        }
        if (!race.isActive(color)) {
            return;
        }
        if (race.isAlreadyFinished(color)) {
            return;
        }
        Team team = ensureTeam(color);
        Player runner = team.getCurrentRunner();
        if (runner == null) {
            return;
        }
        if (!runner.getUniqueId().equals(player.getUniqueId())) {
            return;
        }
        BoundingBox outer = offset(kFieldOuterArea);
        BoundingBox inner = offset(kFieldInnerArea);
        BoundingBox eastWhiteLine = offset(kFieldInnerPoolArea);
        BoundingBox westWhiteLine = offset(kFieldOuterPoolArea);
        BoundingBox northWhiteLine = offset(kFieldOuterJumpArea);
        BoundingBox southWhiteLine = offset(kFieldInnerJumpArea);
        if (!outer.contains(location) || inner.contains(location) || eastWhiteLine.contains(location) || westWhiteLine.contains(location) || northWhiteLine.contains(location) || southWhiteLine.contains(location)) {
            broadcastUnofficial(ChatColor.RED + "%sの%sがコースから逸脱しました。失格とします", ToColoredString(color), player.getName());
            abstain(player);
            return;
        }

        BoundingBox checkPoint = offset(kPreGoalCheckPointArea);
        BoundingBox goal = offset(kGoalArea);

        if (!team.isRunnerPassedCheckPoint(player) && checkPoint.contains(location)) {
            team.pushPassedCheckPoint(player);
        }

        if (team.getOrderLength() >= race.numberOfLaps && team.isRunnerPassedCheckPoint(player) && goal.contains(location)) {
            // チェックポイント通過済みの最終走者がゴールを通過した
            race.pushOrder(color);
            broadcast("%s GOAL !!", ToColoredString(color));
            launchFireworkRocket(color);

            // 全てのチームがゴールしたら結果を表示して終了
            AtomicBoolean allTeamsFinished = new AtomicBoolean(true);
            teams.forEach((tc, c) -> {
                if (!race.isActive(tc)) {
                    return;
                }
                if (!race.isAlreadyFinished(tc)) {
                    allTeamsFinished.set(false);
                }
            });
            if (allTeamsFinished.get()) {
                broadcast("");
                broadcast("-----------------------");
                broadcast("[結果発表]");
                for (int i = 0; i < race.order.size(); i++) {
                    TeamColor c = race.order.get(i);
                    broadcast("%d位 : %s", i + 1, ToColoredString(c));
                }
                broadcast("-----------------------");
                broadcast("");
                race = null;
                teams.forEach((tc, t) -> {
                    t.clearParticipants();
                    t.clearOrder();
                    t.clearPassedCheckPoint();
                });
                setStatus(Status.IDLE);
            }
        }
    }

    private void launchFireworkRocket(TeamColor color) {
        int c = 0;
        switch (color) {
            case WHITE:
                c = FireworkRocket.Color.LIGHT_BLUE;
                break;
            case YELLOW:
                c = FireworkRocket.Color.YELLOW;
                break;
            case RED:
                c = FireworkRocket.Color.PINK;
                break;
            default:
                return;
        }
        FireworkRocket.Launch(x(41) + 0.5, y(-50) + 0.5, z(-171) + 0.5, new int[]{c}, new int[]{c}, 20, 1, false, false);
        FireworkRocket.Launch(x(41) + 0.5, y(-48) + 0.5, z(-173) + 0.5, new int[]{c}, new int[]{c}, 20, 1, false, false);
        FireworkRocket.Launch(x(41) + 0.5, y(-50) + 0.5, z(-175) + 0.5, new int[]{c}, new int[]{c}, 20, 1, false, false);
        FireworkRocket.Launch(x(41) + 0.5, y(-48) + 0.5, z(-177) + 0.5, new int[]{c}, new int[]{c}, 20, 1, false, false);
        FireworkRocket.Launch(x(41) + 0.5, y(-50) + 0.5, z(-179) + 0.5, new int[]{c}, new int[]{c}, 20, 1, false, false);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        if (_status != Status.RUN) {
            return;
        }
        Player player = e.getPlayer();
        GameMode mode = e.getNewGameMode();
        if (mode != GameMode.ADVENTURE && mode != GameMode.SURVIVAL) {
            abstainIfRunner(player);
        }
    }

    private void abstain(Player player) {
        TeamColor color = getCurrentTeam(player);
        if (color == null) {
            return;
        }
        Team team = ensureTeam(color);
        clearBatons(player);
        team.clearParticipants();
        if (this.race != null) {
            race.remove(color);

            if (race.getTeamCount() == 0) {
                if (getPlayerCount() == 0) {
                    setStatus(Status.IDLE);
                } else {
                    // 唯一のチームが失格になった. 結果も表示できないので AWAIT_START に戻す
                    setStatus(Status.AWAIT_START);
                }
            }
        }
    }

    private void abstainIfRunner(Player player) {
        if (_status != Status.RUN) {
            return;
        }
        Race race = this.race;
        if (race == null) {
            return;
        }
        TeamColor color = getCurrentTeam(player);
        if (color == null) {
            return;
        }
        Team team = ensureTeam(color);
        Player runner = team.getCurrentRunner();
        if (runner == null) {
            return;
        }
        if (!runner.getUniqueId().equals(player.getUniqueId())) {
            return;
        }
        abstain(player);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerDeath(PlayerDeathEvent e) {
        abstainIfRunner(e.getEntity());
    }

    private void resetField() {
        Editor.WallSign(offset(kButtonEntryRed), BlockFace.NORTH, "赤組", "エントリー");
        Editor.WallSign(offset(kButtonEntryWhite), BlockFace.NORTH, "白組", "エントリー");
        Editor.WallSign(offset(kButtonEntryYellow), BlockFace.NORTH, "黃組", "エントリー");
        Editor.WallSign(offset(kButtonLeave), BlockFace.NORTH, "エントリー解除");

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

        // 第1コーナー内側
        stroke(block, kCorner1stInner);
        if (enable) {
            setBlock(new Point3i(62, -60, -179), "birch_fence[north=true,west=true]");
            setBlock(new Point3i(63, -60, -180), "birch_fence[north=true,west=true]");
            setBlock(new Point3i(64, -60, -181), "birch_fence[north=true,west=true]");
        }

        // 第1コーナー外側
        stroke(block, kCorner1stOuter);
        if (enable) {
            setBlock(new Point3i(70, -60, -171), "birch_fence[north=true,west=true]");
            setBlock(new Point3i(72, -60, -173), "birch_fence[north=true,west=true]");
            setBlock(new Point3i(71, -60, -172), "birch_fence[north=true,west=true]");
        }

        // 第2コーナー内側
        stroke(block, kCorner2ndInner);
        if (enable) {
            setBlock(new Point3i(62, -60, -226), "birch_fence[south=true,west=true]");
            setBlock(new Point3i(64, -60, -224), "birch_fence[south=true,west=true]");
        }

        // 第2コーナー外側
        stroke(block, kCorner2ndOuter);
        if (enable) {
            setBlock(new Point3i(72, -60, -232), "birch_fence[south=true,west=true]");
            setBlock(new Point3i(70, -60, -234), "birch_fence[south=true,west=true]");
        }

        // 第3コーナー内側
        stroke(block, kCorner3rdInner);
        if (enable) {
            setBlock(new Point3i(11, -60, -225), "birch_fence[north=true,west=true]");
            setBlock(new Point3i(11, -60, -226), "birch_fence[east=true,south=true]");
            setBlock(new Point3i(10, -60, -224), "birch_fence[north=true,west=true]");
        }

        // 第3コーナー外側
        stroke(block, kCorner3rdOuter);
        if (enable) {
            setBlock(new Point3i(3, -60, -234), "birch_fence[east=true,south=true]");
            setBlock(new Point3i(2, -60, -232), "birch_fence[north=true,west=true]");
        }

        // 第4コーナー内側
        stroke(block, kCorner4thInner);
        if (enable) {
            setBlock(new Point3i(10, -60, -180), "birch_fence[east=true,north=true]");
            setBlock(new Point3i(9, -60, -181), "birch_fence[east=true,north=true]");
        }

        // 第4コーナー外側
        stroke(block, kCorner4thOuter);
        if (enable) {
            setBlock(new Point3i(2, -60, -173), "birch_fence[south=true,west=true]");
            setBlock(new Point3i(1, -60, -173), "birch_fence[east=true,north=true]");
        }
    }

    private void setEnableStartGate(boolean enable) {
        if (enable) {
            // 第1レーン
            fill(new Point3i(37, -60, -178), new Point3i(38, -60, -176), "birch_fence");
            setBlock(new Point3i(37, -61, -177), "command_block[facing=north]");
            setBlock(new Point3i(37, -60, -177), "birch_button[face=floor,facing=east]");

            // 第2レーン
            fill(new Point3i(38, -60, -176), new Point3i(39, -60, -174), "birch_fence");
            setBlock(new Point3i(38, -61, -175), "command_block[facing=north]");
            setBlock(new Point3i(38, -60, -175), "birch_button[face=floor,facing=east]");

            // 第3レーン
            fill(new Point3i(39, -60, -174), new Point3i(40, -60, -172), "birch_fence");
            setBlock(new Point3i(39, -61, -173), "command_block[facing=north]");
            setBlock(new Point3i(39, -60, -173), "birch_button[face=floor,facing=east]");

            setBlock(new Point3i(38, -60, -176), "birch_fence[east=true,north=true,west=true]");
            setBlock(new Point3i(39, -60, -174), "birch_fence[east=true,north=true,west=true]");
        } else {
            fill(new Point3i(37, -60, -178), new Point3i(40, -60, -172), "air");
            fill(new Point3i(40, -61, -172), new Point3i(37, -61, -178), "dirt_path");
        }
    }

    private void fill(Point3i from, Point3i to, String block) {
        Editor.Fill(offset(from), offset(to), block);
    }

    private void setBlock(Point3i p, String block) {
        Editor.SetBlock(offset(p), block);
    }

    private void onClickJoin(Player player, TeamColor teamColor) {
        if (_status != Status.IDLE && _status != Status.AWAIT_START) {
            return;
        }
        CompetitionType type = delegate.mainGetCurrentCompetition(player);
        if (type != null && type != CompetitionType.RELAY) {
            broadcastUnofficial("[リレー] %sは既に%sにエントリー済みです", player.getName(), CompetitionTypeHelper.ToString(type));
            return;
        }
        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) {
            player.sendMessage("[リレー] ゲームモードはサバイバルかアドベンチャーの場合のみ参加可能です");
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
        delegate.mainClearCompetitionItems(player);
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
        clearBatons(player);

        // チーム競技なので人数が減ったら強制的にノーコンテストにする
        setStatus(Status.IDLE);
    }

    private void clearBatons(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (container.has(NamespacedKey.minecraft(kItemTag), PersistentDataType.BYTE)) {
                inventory.clear(i);
            }
        }
    }

    private void giveBaton(Player player) {
        ItemStack baton = ItemBuilder.For(Material.BLAZE_ROD)
                .amount(1)
                .customByteTag(kItemTag, (byte) 1)
                .displayName("バトン")
                .build();
        player.getInventory().addItem(baton);
    }

    private void onClickStart() {
        if (_status != Status.AWAIT_START) {
            return;
        }
        AtomicBoolean canStart = new AtomicBoolean(false);
        teams.forEach((color, team) -> {
            int count = team.getPlayerCount();
            if (count < 1) {
                // 不参加
            } else if (count == 1) {
                broadcastUnofficial(ChatColor.RED + "[リレー] %sの参加者が足りません", ToColoredString(color));
            } else {
                canStart.set(true);
            }
        });
        if (!canStart.get()) {
            return;
        }

        // 第一走者を検出する
        Map<TeamColor, Player> firstRunners = new HashMap<>();
        World world = delegate.mainGetWorld();
        if (world == null) {
            return;
        }
        Player[] lanes = new Player[]{null, null, null};
        BoundingBox[] laneBoundingBox = new BoundingBox[]{offset(kStartGateFirstLane), offset(kStartGateSecondLane), offset(kStartGateThirdLane)};
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
            // 最も参加人数が多いチームの人数を周回数とする. 人数が足りないチームは複数回出走する
            numberOfLaps.set(Math.max(c, numberOfLaps.get()));
            broadcast("%s が競技に参加します（参加者%d人）", ToColoredString(color), c);
        });
        broadcast("-----------------------");
        broadcast("");
        broadcast("[リレー] 競技を開始します！");
        broadcast("");
        setStatus(Status.COUNTDOWN);
        delegate.mainCountdownThen(new BoundingBox[]{getAnnounceBounds()}, c -> _status == Status.COUNTDOWN, () -> {
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
                giveBaton(runner);
                broadcast("%s 第一走者 : %sがスタート！", ToColoredString(teamColor), runner.getName());
                Team team = ensureTeam(teamColor);
                team.pushRunner(runner);
                race.add(teamColor);
            });
            setStatus(Status.RUN);
            return true;
        }, 20);
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
        if (_status != Status.RUN || race == null) {
            return;
        }
        Entity damagerEntity = e.getDamager();
        Entity entity = e.getEntity();
        if (!(damagerEntity instanceof Player) || !(entity instanceof Player)) {
            return;
        }

        //TODO: バトンで殴ったかどうか確認するのが良さそう

        // 両者がバトンパス領域に入っているかどうか確かめる
        BoundingBox box = offset(kBatonPassingArea);
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

        if (team.getOrderLength() >= race.numberOfLaps) {
            // 最終走者はバトンパスしない
            return;
        }

        // バトンパスする
        team.pushRunner(to);
        clearBatons(from);
        giveBaton(to);
        broadcast("%s バトンタッチ！%sがスタート！", ToColoredString(teamColor), to.getName());
    }

    private boolean initialized = false;

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (initialized) {
            return;
        }
        initialized = true;
        delegate.mainRunTaskLater(this::resetField, loadDelay);
    }

    private BoundingBox getAnnounceBounds() {
        return offset(kAnnounceBounds);
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

    private void sendMessage(String message, String selectorArgFormat, Object... args) {
        String selector = String.format(selectorArgFormat, args);
        execute("execute if entity @a[%s] run tellraw @a[%s] \"%s\"", selector, selector, message);
    }

    private void broadcast(String format, Object... args) {
        String msg = String.format(format, args);
        sendMessage(msg, TargetSelector.Of(getAnnounceBounds()));
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String msg, Object... args) {
        broadcast(msg, args);
    }

    private BoundingBox offset(BoundingBox box) {
        return new BoundingBox(xd(box.getMinX()), yd(box.getMinY()), zd(box.getMinZ()), xd(box.getMaxX()), yd(box.getMaxY()), zd(box.getMaxZ()));
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
        delegate.mainExecute(format, args);
    }

    @Override
    public boolean competitionIsJoined(Player player) {
        return getCurrentTeam(player) != null;
    }

    @NotNull
    @Override
    public CompetitionType competitionGetType() {
        return CompetitionType.RELAY;
    }

    @Override
    public void competitionClearItems(Player player) {
        clearBatons(player);
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

    // アナウンス etc. を行う範囲
    private static final BoundingBox kAnnounceBounds = new BoundingBox(-2, -61, -241, 82, 384, -115);
    // 広めに. 進行方向手前 8 ブロック, 進行方向に 16 ブロック
    private static final BoundingBox kBatonPassingArea = new BoundingBox(36, -61, -179, 56, -58, -170);
    // 競技場の外側の白線
    private static final BoundingBox kFieldOuterArea = new BoundingBox(1, -62, -234, 73, -54, -170);
    // 水槽の外側の白線(西). 侵入不可にする
    private static final BoundingBox kFieldOuterPoolArea = new BoundingBox(1, -60, -201, 2, -58, -189);
    // 競技場の内側の白線
    private static final BoundingBox kFieldInnerArea = new BoundingBox(10.5, -62, -224.5, 63.5, -54, -179.5);
    // 水槽の内側の白線(東). 侵入不可にする
    private static final BoundingBox kFieldInnerPoolArea = new BoundingBox(9, -60, -201, 10, -58, -189);
    // 水槽の外側の白線(北). 進入不可にする
    private static final BoundingBox kFieldOuterJumpArea = new BoundingBox(12, -60, -234, 30, -54, -233);
    // 水槽の外側の白線(南). 進入不可にする
    private static final BoundingBox kFieldInnerJumpArea = new BoundingBox(12, -60, -226, 30, -54, -225);
    // 赤組MOB討伐ステージの入り口前付近
    private static final BoundingBox kPreGoalCheckPointArea = new BoundingBox(30, -61, -234, 43, -54, -225);
    // ゴール判定を行う範囲. ゴールラインから進行方向に 8 ブロック
    private static final BoundingBox kGoalArea = new BoundingBox(41, -61, -179, 49, -58, -170);
    private static final BoundingBox kStartGateFirstLane = new BoundingBox(37.5, -60, -177.5, 38.5, -58, -175.5);
    private static final BoundingBox kStartGateSecondLane = new BoundingBox(38.5, -60, -175.5, 39.5, -58, -173.5);
    private static final BoundingBox kStartGateThirdLane = new BoundingBox(39.5, -60, -173.5, 40.5, -58, -171.5);
}