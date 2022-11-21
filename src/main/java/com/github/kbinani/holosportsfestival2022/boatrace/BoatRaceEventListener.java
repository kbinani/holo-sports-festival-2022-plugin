package com.github.kbinani.holosportsfestival2022.boatrace;

import com.github.kbinani.holosportsfestival2022.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BoatRaceEventListener implements Competition {
    private final long loadDelay;
    private final MainDelegate delegate;
    private static final String kPrimaryShootItemDisplayName = "[水上レース専用] 暗闇（弱）";
    private static final String kSecondaryShootItemDisplayName = "[水上レース専用] 暗闇（強）";

    static void AllTeamColors(Consumer<TeamColor> callback) {
        callback.accept(TeamColor.RED);
        callback.accept(TeamColor.WHITE);
        callback.accept(TeamColor.YELLOW);
    }

    private Status _status = Status.IDLE;
    private final Map<TeamColor, Long> finishedServerTime = new HashMap<>();

    private void setStatus(Status status) {
        if (status == _status) {
            return;
        }
        _status = status;
        switch (status) {
            case IDLE:
                resetField();
                Bukkit.getServer().getOnlinePlayers().forEach(this::clearItems);
                break;
            case AWAIT_START:
                // ゴールラインに柵を設置
                setGoalGateOpened(false);

                // スタートラインに柵を設置
                setStartGateOpened(false);
                break;
            case COUNTDOWN:
                break;
            case RUN:
                // ゴールラインの柵を撤去
                setGoalGateOpened(true);

                // スタートラインの柵を撤去
                setStartGateOpened(true);

                // 妨害装置を起動
                //TODO: 同時起動だと妨害装置が同期するので適当にずらす
                for (Point3i p : kJammingBlockStarterBlocks) {
                    setLeverPowered(offset(p), true);
                }
                break;
        }
    }

    private void setStartGateOpened(boolean opened) {
        String block = opened ? "air" : "bedrock";
        fill(new Point3i(-26, -58, -186), new Point3i(-36, -58, -186), block);
        fill(new Point3i(-37, -58, -187), new Point3i(-44, -58, -187), block);
        fill(new Point3i(-45, -58, -188), new Point3i(-52, -58, -188), block);
    }

    private void setGoalGateOpened(boolean opened) {
        fill(new Point3i(-52, -58, -196), new Point3i(-25, -58, -196), opened ? "air" : "bedrock");
    }

    private void fill(Point3i from, Point3i to, String block) {
        Editor.Fill(offset(from), offset(to), block);
    }

    private void resetField() {
        // 操作用の看板を設置
        Editor.WallSign(offset(kYellowEntryShooter), BlockFace.WEST, "黄組", "エントリー", ToString(Role.SHOOTER));
        Editor.WallSign(offset(kYellowEntryDriver), BlockFace.WEST, "黄組", "エントリー", ToString(Role.DRIVER));
        Editor.WallSign(offset(kWhiteEntryShooter), BlockFace.WEST, "白組", "エントリー", ToString(Role.SHOOTER));
        Editor.WallSign(offset(kWhiteEntryDriver), BlockFace.WEST, "白組", "エントリー", ToString(Role.DRIVER));
        Editor.WallSign(offset(kRedEntryShooter), BlockFace.WEST, "赤組", "エントリー", ToString(Role.SHOOTER));
        Editor.WallSign(offset(kRedEntryDriver), BlockFace.WEST, "赤組", "エントリー", ToString(Role.DRIVER));
        Editor.WallSign(offset(kLeaveButton), BlockFace.WEST, "エントリー解除");

        // ゴールラインの柵を撤去
        setGoalGateOpened(true);

        // スタートラインの柵を撤去
        setStartGateOpened(true);

        // 妨害装置を停止
        for (Point3i p : kJammingBlockStarterBlocks) {
            setLeverPowered(offset(p), false);
        }

        // 競技用のエンティティを削除する. 競技場内に居るアイテム化したボート.
        execute("kill @e[tag=%s,%s]", kItemTag, TargetSelector.Of(getFieldBounds()));
    }

    private void setLeverPowered(Point3i pos, boolean powered) {
        World world = delegate.mainGetWorld();
        if (world == null) {
            return;
        }
        Block block = world.getBlockAt(pos.x, pos.y, pos.z);
        BlockData data = block.getBlockData();
        if (data.getMaterial() != Material.LEVER) {
            return;
        }
        if (!(data instanceof Powerable)) {
            return;
        }
        Powerable lever = (Powerable) data;
        lever.setPowered(powered);
        world.setBlockData(pos.x, pos.y, pos.z, data);
    }

    private static int FireworkRocketColor(TeamColor color) {
        switch (color) {
            case RED:
                return FireworkRocket.Color.PINK;
            case WHITE:
                return FireworkRocket.Color.LIGHT_BLUE;
            case YELLOW:
                return FireworkRocket.Color.YELLOW;
        }
        return 0;
    }

    private void launchFireworkRockets(TeamColor color) {
        int c = FireworkRocketColor(color);
        for (int i = 0; i < 5; i++) {
            Point3i pos = offset(new Point3i(-51 + i * 6, -50, -196));
            FireworkRocket.Launch(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, new int[]{c}, new int[]{c}, 10, 1, false, false);
        }
    }

    static String ToString(Role role) {
        if (role == Role.DRIVER) {
            return "（操縦担当）";
        } else {
            return "（妨害担当）";
        }
    }

    static String ToString(TeamColor color) {
        if (color == TeamColor.RED) {
            return "TEAM RED";
        } else if (color == TeamColor.YELLOW) {
            return "TEAM YELLOW";
        } else {
            return "TEAM WHITE";
        }
    }

    static String ToColoredString(TeamColor color) {
        if (color == TeamColor.RED) {
            return ChatColor.RED + "TEAM RED" + ChatColor.RESET;
        } else if (color == TeamColor.YELLOW) {
            return ChatColor.YELLOW + "TEAM YELLOW" + ChatColor.RESET;
        } else {
            return ChatColor.GRAY + "TEAM WHITE" + ChatColor.RESET;
        }
    }

    private final Map<TeamColor, Team> teams = new HashMap<>();

    private @Nonnull Team ensureTeam(TeamColor color) {
        Team t = teams.get(color);
        if (t == null) {
            t = new Team();
            teams.put(color, t);
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
    private static final Point3i[] kJammingBlockStarterBlocks = new Point3i[]{
            new Point3i(-49, -62, -249),
            new Point3i(-39, -62, -247),
            new Point3i(-47, -62, -240),
            new Point3i(-37, -62, -238),
            new Point3i(-29, -62, -218),
            new Point3i(-41, -62, -224),
            new Point3i(-50, -62, -232),

            new Point3i(-105, -56, -189), // 西側のフェンスゲート
    };
    private static final String kItemTag = "hololive_sports_festival_2022_boat_race";
    private static final BoundingBox kFieldBounds = new BoundingBox(-106, -60, -294, -24, -30, -127);
    private static final BoundingBox kAnnounceBoundsNorth = new BoundingBox(-106, -60, -294, -11, -30, -243);
    private static final BoundingBox kAnnounceBoundsSouth = new BoundingBox(-106, -60, -243, -2, -30, -126);

    public BoatRaceEventListener(MainDelegate delegate, long loadDelay) {
        this.loadDelay = loadDelay;
        this.delegate = delegate;
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        Participation participation = getCurrentParticipation(player);
        if (participation == null) {
            return;
        }
        Location location = player.getLocation();
        if (!getFieldBounds().contains(location.toVector())) {
            player.sendMessage(ChatColor.RED + "[水上レース] 場外に出たためエントリー解除となります");
            onClickLeave(player);
            return;
        }

        if (_status != Status.RUN) {
            return;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        Team team = ensureTeam(participation.color);
        PlayerStatus playerStatus = team.getPlayerStatus(player);
        if (playerStatus == null) {
            return;
        }
        switch (playerStatus) {
            case IDLE:
                break;
            case STARTED:
                if (x(-65) <= x && x <= x(-59) && y(-47) <= y && y <= y(-44) && z(-293) <= z && z <= z(-253)) {
                    // 滝の頂上を通過
                    team.updatePlayerStatus(participation.role, PlayerStatus.CLEARED_CHECKPOINT1);
                    delegate.mainInfo("[水上レース] %s %s%sが1周目のチェックポイントを通過", player.getName(), ToString(participation.color), ToString(participation.role));
                }
                break;
            case CLEARED_CHECKPOINT1:
                if (x(-52) <= x && x <= x(-25) && y(-59) <= y && y <= y(-57) && z(-196) <= z && z <= z(-188)) {
                    // ゴールラインを通過
                    team.updatePlayerStatus(participation.role, PlayerStatus.CLEARED_START_LINE1);
                    delegate.mainInfo("[水上レース] %s %s%sが1周目のゴールラインを通過", player.getName(), ToString(participation.color), ToString(participation.role));
                    if (team.getRemainingRound() == 1) {
                        broadcast("%s あと1周！", ToColoredString(participation.color));
                    }
                }
                break;
            case CLEARED_START_LINE1:
                if (x(-65) <= x && x <= x(-59) && y(-47) <= y && y <= y(-44) && z(-293) <= z && z <= z(-253)) {
                    // 滝の頂上を通過
                    team.updatePlayerStatus(participation.role, PlayerStatus.CLEARED_CHECKPOINT2);
                    delegate.mainInfo("[水上レース] %s %s%sが2周目のチェックポイントを通過", player.getName(), ToString(participation.color), ToString(participation.role));
                }
                break;
            case CLEARED_CHECKPOINT2:
                if (x(-52) <= x && x <= x(-25) && y(-59) <= y && y <= y(-57) && z(-196) <= z && z <= z(-188)) {
                    // ゴールラインを通過
                    team.updatePlayerStatus(participation.role, PlayerStatus.FINISHED);
                    delegate.mainInfo("[水上レース] %s %s%sが2周目のゴールラインを通過", player.getName(), ToString(participation.color), ToString(participation.role));
                    if (team.getRemainingRound() == 0) {
                        broadcast("%s GOAL !!", ToColoredString(participation.color));
                        launchFireworkRockets(participation.color);
                        finishedServerTime.put(participation.color, player.getWorld().getGameTime());
                        boolean cleared = true;
                        for (Long it : finishedServerTime.values()) {
                            if (it < 0) {
                                cleared = false;
                                break;
                            }
                        }
                        if (cleared) {
                            broadcast("");
                            broadcast("-----------------------");
                            broadcast("[結果発表]");
                            List<TeamColor> ordered = finishedServerTime.keySet().stream().sorted((a, b) -> {
                                long timeA = finishedServerTime.get(a);
                                long timeB = finishedServerTime.get(b);
                                return (int) (timeA - timeB);
                            }).collect(Collectors.toList());
                            for (int i = 0; i < ordered.size(); i++) {
                                broadcast("%d位: %s", i + 1, ToColoredString(ordered.get(i)));
                            }
                            broadcast("-----------------------");
                            broadcast("");
                            AllTeamColors(c -> {
                                Team p = ensureTeam(c);
                                p.setPlayer(Role.DRIVER, null);
                                p.setPlayer(Role.SHOOTER, null);
                            });
                            finishedServerTime.clear();
                            setStatus(Status.IDLE);
                        }
                    }
                }
                break;
            case FINISHED:
                break;
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block block = e.getClickedBlock();
        Action action = e.getAction();
        ItemStack item = e.getItem();
        if (block != null && action == Action.RIGHT_CLICK_BLOCK) {
            Point3i location = new Point3i(block.getLocation());
            if (location.equals(offset(kYellowEntryShooter))) {
                onClickJoin(player, TeamColor.YELLOW, Role.SHOOTER);
                return;
            } else if (location.equals(offset(kYellowEntryDriver))) {
                onClickJoin(player, TeamColor.YELLOW, Role.DRIVER);
                return;
            } else if (location.equals(offset(kWhiteEntryShooter))) {
                onClickJoin(player, TeamColor.WHITE, Role.SHOOTER);
                return;
            } else if (location.equals(offset(kWhiteEntryDriver))) {
                onClickJoin(player, TeamColor.WHITE, Role.DRIVER);
                return;
            } else if (location.equals(offset(kRedEntryShooter))) {
                onClickJoin(player, TeamColor.RED, Role.SHOOTER);
                return;
            } else if (location.equals(offset(kRedEntryDriver))) {
                onClickJoin(player, TeamColor.RED, Role.DRIVER);
                return;
            } else if (location.equals(offset(kLeaveButton))) {
                onClickLeave(player);
                return;
            }
        }
        if (_status == Status.RUN && item != null && (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)) {
            ItemMeta meta = item.getItemMeta();
            Participation participation = getCurrentParticipation(player);
            if (participation != null && meta != null && participation.role == Role.SHOOTER) {
                PotionEffect candidate = null;
                if (kPrimaryShootItemDisplayName.equals(meta.getDisplayName())) {
                    item.setAmount(0);
                    broadcast("%s が暗闇（弱）を発動！", ToColoredString(participation.color));
                    candidate = new PotionEffect(PotionEffectType.DARKNESS, 200, 1);
                } else if (kSecondaryShootItemDisplayName.equals(meta.getDisplayName())) {
                    item.setAmount(0);
                    broadcast("%s が暗闇（強）を発動！", ToColoredString(participation.color));
                    candidate = new PotionEffect(PotionEffectType.BLINDNESS, 100, 1);
                }
                PotionEffect effect = candidate;
                if (effect != null) {
                    for (Map.Entry<TeamColor, Team> it : teams.entrySet()) {
                        TeamColor color = it.getKey();
                        Team team = it.getValue();
                        if (color == participation.color) {
                            continue;
                        }
                        team.eachPlayer((p, role, status) -> {
                            if (status == PlayerStatus.FINISHED || status == PlayerStatus.IDLE) {
                                return;
                            }
                            effect.apply(p);
                        });
                    }
                }
            }
        }
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

    private boolean initialized = false;

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (initialized) {
            return;
        }
        initialized = true;
        delegate.mainRunTaskLater(() -> {
            delegate.mainUsingChunk(getFieldBounds(), world -> {
                resetField();
            });
        }, loadDelay);
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

        if (bx == x(-55) && by == y(-58) && bz == z(-196)) {
            onClickStart();
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onEntityPlace(EntityPlaceEvent e) {
        if (e.getEntityType() != EntityType.BOAT) {
            return;
        }
        Boat boat = (Boat) e.getEntity();
        Point3i pos = new Point3i(boat.getLocation());
        Material material = boat.getBoatType().getMaterial();
        if (material != BoatType(TeamColor.RED) && material != BoatType(TeamColor.WHITE) && material != BoatType(TeamColor.YELLOW)) {
            return;
        }

        BoundingBox bounds = getFieldBounds();
        if (!bounds.contains(pos.x, pos.y, pos.z)) {
            return;
        }

        boat.addScoreboardTag(kItemTag);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        Player player = e.getPlayer();
        GameMode mode = e.getNewGameMode();
        if (mode != GameMode.ADVENTURE && mode != GameMode.SURVIVAL) {
            onClickLeave(player);
        }
    }

    private BoundingBox getFieldBounds() {
        return offset(kFieldBounds);
    }

    private void broadcast(String format, Object... args) {
        String msg = String.format(format, args);
        execute("tellraw @a[%s] \"%s\"", TargetSelector.Of(offset(kAnnounceBoundsNorth)), msg);
        execute("tellraw @a[%s] \"%s\"", TargetSelector.Of(offset(kAnnounceBoundsSouth)), msg);
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String msg, Object... args) {
        broadcast(msg, args);
    }

    private static Material Boat(TeamColor color) {
        if (color == TeamColor.WHITE) {
            return Material.BIRCH_BOAT;
        } else if (color == TeamColor.YELLOW) {
            return Material.JUNGLE_BOAT;
        } else {
            return Material.MANGROVE_BOAT;
        }
    }

    private static Material BoatType(TeamColor color) {
        if (color == TeamColor.WHITE) {
            return Material.BIRCH_PLANKS;
        } else if (color == TeamColor.YELLOW) {
            return Material.JUNGLE_PLANKS;
        } else {
            return Material.MANGROVE_PLANKS;
        }
    }

    private void onClickJoin(Player player, TeamColor color, Role role) {
        if (_status != Status.IDLE && _status != Status.AWAIT_START) {
            return;
        }
        CompetitionType type = delegate.mainGetCurrentCompetition(player);
        if (type != null && type != CompetitionType.BOAT_RACE) {
            broadcastUnofficial("[水上レース] %sは既に%sにエントリー済みです", player.getName(), CompetitionTypeHelper.ToString(type));
            return;
        }
        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) {
            player.sendMessage("[水上レース] ゲームモードはサバイバルかアドベンチャーの場合のみ参加可能です");
            return;
        }
        @Nullable Participation current = getCurrentParticipation(player);
        if (current == null) {
            Team p = ensureTeam(color);
            p.setPlayer(role, player);
            delegate.mainClearCompetitionItems(player);
            if (role == Role.DRIVER) {
                execute("give @p[name=\"%s\"] %s{tag:{%s:1b}}", player.getName(), Boat(color).name().toLowerCase(), kItemTag);
            } else {
                execute("give @p[name=\"%s\"] snowball{tag:{%s:1b},display:{Name:'[{\"text\":\"%s\"}]'}}", player.getName(), kItemTag, kPrimaryShootItemDisplayName);
                execute("give @p[name=\"%s\"] crossbow{tag:{%s:1b}}", player.getName(), kItemTag);
                execute("give @p[name=\"%s\"] splash_potion{Potion:darkness,tag:{%s:1b},display:{Name:'[{\"text\":\"%s\"}]'}}", player.getName(), kItemTag, kSecondaryShootItemDisplayName);
                int c = FireworkRocketColor(color);
                execute("give @p[name=\"%s\"] firework_rocket{tag:{%s:1b},Fireworks:{Explosions:[{Type:0,Flicker:0b,Trail:0b,Colors:[I;%d],FadeColors:[I;%d],Flight:3}]}} 3", player.getName(), kItemTag, c, c);
            }
            broadcast("[水上レース] %sが%s%sにエントリーしました", player.getName(), ToColoredString(color), ToString(role));
            setStatus(Status.AWAIT_START);
        } else {
            broadcast("[水上レース] %sは%sにエントリー済みです", player.getName(), ToColoredString(color));
        }
    }

    private void onClickLeave(Player player) {
        // 競技中でもエントリー解除したらノーコンテストにする. ので status のチェックはここではやらない

        @Nullable Participation current = getCurrentParticipation(player);
        if (current == null) {
            return;
        }
        Team team = ensureTeam(current.color);
        team.setPlayer(current.role, null);
        clearItems(player);
        broadcast("[水上レース] %sがエントリー解除しました", player.getName());

        AtomicInteger totalPlayerCount = new AtomicInteger(0);
        AllTeamColors(t -> {
            Team p = ensureTeam(t);
            totalPlayerCount.addAndGet(p.getPlayerCount());
        });
        if (totalPlayerCount.get() < 1) {
            setStatus(Status.IDLE);
        }
    }

    private void clearItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (inventory.contains(Material.SNOWBALL)) {
            execute("clear %s snowball{tag:{%s:1b}}", player.getName(), kItemTag);
        }
        if (inventory.contains(Material.CROSSBOW)) {
            execute("clear %s crossbow{tag:{%s:1b}}", player.getName(), kItemTag);
        }
        if (inventory.contains(Material.SPLASH_POTION)) {
            execute("clear %s splash_potion{tag:{%s:1b}}", player.getName(), kItemTag);
        }
        for (Material material : new Material[]{Boat(TeamColor.RED), Boat(TeamColor.YELLOW), Boat(TeamColor.WHITE)}) {
            if (inventory.contains(material)) {
                execute("clear %s %s{tag:{%s:1b}}", player.getName(), material.name().toLowerCase(), kItemTag);
            }
        }
    }

    private @Nullable Participation getCurrentParticipation(@Nonnull Player player) {
        for (TeamColor color : teams.keySet()) {
            Team team = teams.get(color);
            if (team == null) {
                continue;
            }
            @Nullable Role role = team.getCurrentRole(player);
            if (role != null) {
                return new Participation(color, role);
            }
        }
        return null;
    }

    private void onClickStart() {
        if (_status == Status.RUN) {
            return;
        }
        // 1 チームでも準備ができていればスタート可能にする
        int totalPlayerCount = 0;
        for (TeamColor color : new TeamColor[]{TeamColor.RED, TeamColor.WHITE, TeamColor.YELLOW}) {
            Team team = ensureTeam(color);
            totalPlayerCount += team.getPlayerCount();
        }
        if (totalPlayerCount < 1) {
            broadcastUnofficial(ChatColor.RED + "[水上レース] 参加者が 0 人です");
            return;
        }
        broadcast("");
        broadcast("-----------------------");
        for (TeamColor color : new TeamColor[]{TeamColor.YELLOW, TeamColor.RED, TeamColor.WHITE}) {
            Team team = ensureTeam(color);
            int count = team.getPlayerCount();
            if (count < 1) {
                broadcast("%sの参加者が見つかりません", ToString(color));
            } else {
                broadcast("%s が競技に参加します（参加者%d人）", ToColoredString(color), count);
            }
        }
        AllTeamColors(color -> {
            Team team = ensureTeam(color);
            team.updatePlayerStatus(Role.DRIVER, PlayerStatus.STARTED);
            team.updatePlayerStatus(Role.SHOOTER, PlayerStatus.STARTED);
            if (team.getPlayerCount() > 0) {
                finishedServerTime.put(color, (long) -1);
            }
        });
        broadcast("-----------------------");
        broadcast("");
        broadcast("[水上レース] 競技を開始します！");
        broadcast("");
        setStatus(Status.COUNTDOWN);

        // 場内に居るボートに tag を付ける. 競技終了した時このタグが付いているボートを kill する.
        execute("tag @e[type=boat,%s] add %s", TargetSelector.Of(getFieldBounds()), kItemTag);

        delegate.mainCountdownThen(new BoundingBox[]{offset(kAnnounceBoundsNorth), offset(kAnnounceBoundsSouth)}, (count) -> _status == Status.COUNTDOWN, () -> {
            if (_status != Status.COUNTDOWN) {
                return false;
            }
            setStatus(Status.RUN);
            return true;
        }, 20);
    }

    private BoundingBox offset(BoundingBox box) {
        return new BoundingBox(x(box.getMinX()), y(box.getMinY()), z(box.getMinZ()), x(box.getMaxX()), y(box.getMaxY()), z(box.getMaxZ()));
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

    private double x(double x) {
        // 座標が間違っていたらここでオフセットする
        return x;
    }

    private double y(double y) {
        // 座標が間違っていたらここでオフセットする
        return y;
    }

    private double z(double z) {
        // 座標が間違っていたらここでオフセットする
        return z;
    }

    private void execute(String format, Object... args) {
        delegate.mainExecute(format, args);
    }

    @Override
    public boolean competitionIsJoined(Player player) {
        return getCurrentParticipation(player) != null;
    }

    @Override
    public @NotNull CompetitionType competitionGetType() {
        return CompetitionType.BOAT_RACE;
    }

    @Override
    public void competitionClearItems(Player player) {
        clearItems(player);
    }
}