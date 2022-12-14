package com.github.kbinani.holosportsfestival2022.boatrace;

import com.github.kbinani.holosportsfestival2022.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    static void AllTeamColorsAndRoles(BiConsumer<TeamColor, Role> callback) {
        AllTeamColors((color) -> {
            callback.accept(color, Role.DRIVER);
            callback.accept(color, Role.SHOOTER);
        });
    }

    private Status _status = Status.IDLE;
    private final Map<TeamColor, Long> finishedServerTime = new HashMap<>();
    private boolean initialized = false;
    private final Map<TeamColor, Team> teams = new HashMap<>();

    private void setStatus(Status status) {
        if (status == _status) {
            return;
        }
        _status = status;
        switch (status) {
            case IDLE:
                resetField();
                Bukkit.getServer().getOnlinePlayers().forEach(this::clearItems);
                AllTeamColorsAndRoles((color, role) -> ensureScoreboardTeam(color, role).unregister());
                finishedServerTime.clear();
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

                AllTeamColorsAndRoles((color, role) -> {
                    var textColor = TextColor(color);
                    var team = ensureScoreboardTeam(color, role);
                    team.prefix(Component.empty());
                    team.color(textColor);
                });
                break;
        }
    }

    private void setStartGateOpened(boolean opened) {
        World world = delegate.mainGetWorld();
        String block = opened ? "air" : "bedrock";
        fill(world, new Point3i(-26, -58, -186), new Point3i(-36, -58, -186), block);
        fill(world, new Point3i(-37, -58, -187), new Point3i(-44, -58, -187), block);
        fill(world, new Point3i(-45, -58, -188), new Point3i(-52, -58, -188), block);
    }

    private void setGoalGateOpened(boolean opened) {
        fill(delegate.mainGetWorld(), new Point3i(-52, -58, -196), new Point3i(-25, -58, -196), opened ? "air" : "bedrock");
    }

    private void fill(@Nonnull World world, Point3i from, Point3i to, String block) {
        Editor.Fill(world, offset(from), offset(to), block);
    }

    private void resetField() {
        World world = delegate.mainGetWorld();

        // 操作用の看板を設置
        Editor.WallSign(world, offset(kYellowEntryShooter), BlockFace.WEST, "黄組", "エントリー", ToString(Role.SHOOTER));
        Editor.WallSign(world, offset(kYellowEntryDriver), BlockFace.WEST, "黄組", "エントリー", ToString(Role.DRIVER));
        Editor.WallSign(world, offset(kWhiteEntryShooter), BlockFace.WEST, "白組", "エントリー", ToString(Role.SHOOTER));
        Editor.WallSign(world, offset(kWhiteEntryDriver), BlockFace.WEST, "白組", "エントリー", ToString(Role.DRIVER));
        Editor.WallSign(world, offset(kRedEntryShooter), BlockFace.WEST, "赤組", "エントリー", ToString(Role.SHOOTER));
        Editor.WallSign(world, offset(kRedEntryDriver), BlockFace.WEST, "赤組", "エントリー", ToString(Role.DRIVER));
        Editor.WallSign(world, offset(kLeaveButton), BlockFace.WEST, "エントリー解除");

        // ゴールラインの柵を撤去
        setGoalGateOpened(true);

        // スタートラインの柵を撤去
        setStartGateOpened(true);

        // 妨害装置を停止
        for (Point3i p : kJammingBlockStarterBlocks) {
            setLeverPowered(offset(p), false);
        }

        // 競技用のエンティティを削除する. 競技場内に居るアイテム化したボート.
        Kill.EntitiesByScoreboardTag(world, getFieldBounds(), kItemTag);
    }

    private void setLeverPowered(Point3i pos, boolean powered) {
        World world = delegate.mainGetWorld();
        Block block = world.getBlockAt(pos.x, pos.y, pos.z);
        BlockData data = block.getBlockData();
        if (data.getMaterial() != Material.LEVER) {
            return;
        }
        if (!(data instanceof Powerable lever)) {
            return;
        }
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
        World world = delegate.mainGetWorld();
        int c = FireworkRocketColor(color);
        for (int i = 0; i < 5; i++) {
            Point3i pos = offset(new Point3i(-51 + i * 6, -50, -196));
            FireworkRocket.Launch(world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, new int[]{c}, new int[]{c}, 10, 1, false, false);
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

    static NamedTextColor TextColor(TeamColor color) {
        return switch (color) {
            case RED -> NamedTextColor.RED;
            case WHITE -> NamedTextColor.GRAY;
            case YELLOW -> NamedTextColor.YELLOW;
        };
    }

    private @Nonnull Team ensureTeam(TeamColor color) {
        Team t = teams.get(color);
        if (t == null) {
            t = new Team();
            teams.put(color, t);
        }
        return t;
    }

    private @Nonnull org.bukkit.scoreboard.Team ensureScoreboardTeam(TeamColor color, Role role) {
        String name = "holosportsfestival_boatrace_team_";
        String prefix = "";
        TextColor textColor = TextColor(color);
        switch (color) {
            case RED -> {
                name += "red";
                prefix += "赤組";
            }
            case WHITE -> {
                name += "white";
                prefix += "白組";
            }
            case YELLOW -> {
                name += "yellow";
                prefix += "黄組";
            }
        }
        switch (role) {
            case DRIVER -> name += "_driver";
            case SHOOTER -> name += "_shooter";
        }
        prefix += ToString(role);
        Scoreboard scoreboard = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
            team.prefix(Component.text(prefix + " ").color(textColor));
            team.color(NamedTextColor.WHITE);
        }
        return team;
    }

    private void clearScoreboardTeam(Player player) {
        AllTeamColorsAndRoles((color, role) -> {
            var team = ensureScoreboardTeam(color, role);
            team.removePlayer(player);
        });
    }

    private static final Point3i kYellowEntryShooter = new Point3i(-56, -59, -198);
    private static final Point3i kYellowEntryDriver = new Point3i(-56, -59, -200);
    private static final Point3i kWhiteEntryShooter = new Point3i(-56, -59, -202);
    private static final Point3i kWhiteEntryDriver = new Point3i(-56, -59, -204);
    private static final Point3i kRedEntryShooter = new Point3i(-56, -59, -206);
    private static final Point3i kRedEntryDriver = new Point3i(-56, -59, -208);
    private static final Point3i kLeaveButton = new Point3i(-56, -59, -210);
    private static final Point3i kResetButtonNorth = new Point3i(-55, -60, -210);
    private static final Point3i kResetButtonSouth = new Point3i(-55, -60, -196);
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
        World world = delegate.mainGetWorld();
        if (player.getWorld() != world) {
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
        if  (playerStatus == PlayerStatus.STARTED) {
            if (x(-65) <= x && x <= x(-59) && y(-47) <= y && y <= y(-44) && z(-293) <= z && z <= z(-253)) {
                // 滝の頂上を通過
                team.updatePlayerStatus(participation.role, PlayerStatus.CLEARED_CHECKPOINT1, (p, r) -> {
                    delegate.mainGetLogger().info(String.format("[水上レース] %s %s%sが1周目のチェックポイントを通過", p.getName(), ToString(participation.color), ToString(r)));
                });
            }
        } else if (playerStatus == PlayerStatus.CLEARED_CHECKPOINT1) {
            if (x(-52) <= x && x <= x(-25) && y(-59) <= y && y <= y(-57) && z(-196) <= z && z <= z(-188)) {
                // ゴールラインを通過
                team.updatePlayerStatus(participation.role, PlayerStatus.CLEARED_START_LINE1, (p, r) -> {
                    delegate.mainGetLogger().info(String.format("[水上レース] %s %s%sが1周目のゴールラインを通過", p.getName(), ToString(participation.color), ToString(r)));
                });
                if (team.getRemainingRound() == 1) {
                    broadcast("%s あと1周！", ToColoredString(participation.color)).log();
                }
            }
        } else if (playerStatus == PlayerStatus.CLEARED_START_LINE1) {
            if (x(-65) <= x && x <= x(-59) && y(-47) <= y && y <= y(-44) && z(-293) <= z && z <= z(-253)) {
                // 滝の頂上を通過
                team.updatePlayerStatus(participation.role, PlayerStatus.CLEARED_CHECKPOINT2, (p, r) -> {
                    delegate.mainGetLogger().info(String.format("[水上レース] %s %s%sが2周目のチェックポイントを通過", p.getName(), ToString(participation.color), ToString(r)));
                });
            }
        } else if (playerStatus == PlayerStatus.CLEARED_CHECKPOINT2) {
            if (x(-52) <= x && x <= x(-25) && y(-59) <= y && y <= y(-57) && z(-196) <= z && z <= z(-188)) {
                // ゴールラインを通過
                team.updatePlayerStatus(participation.role, PlayerStatus.FINISHED, (p, r) -> {
                    delegate.mainGetLogger().info(String.format("[水上レース] %s %s%sが2周目のゴールラインを通過", p.getName(), ToString(participation.color), ToString(r)));
                });
                if (team.getRemainingRound() == 0) {
                    broadcast("%s GOAL !!", ToColoredString(participation.color)).log();
                    launchFireworkRockets(participation.color);
                    finishedServerTime.put(participation.color, player.getWorld().getGameTime());
                    team.eachPlayer((p, role, status) -> {
                        clearItems(p);
                        giveParticipationReward(p);
                    });
                    boolean cleared = true;
                    for (var it : finishedServerTime.entrySet()) {
                        if (ensureTeam(it.getKey()).getPlayerCount() == 0) {
                            continue;
                        }
                        if (it.getValue() < 0) {
                            cleared = false;
                            break;
                        }
                    }
                    if (cleared) {
                        broadcast("");
                        broadcast("-----------------------");
                        broadcast("[結果発表]").log();
                        List<TeamColor> ordered = finishedServerTime.keySet().stream().sorted((a, b) -> {
                            long timeA = finishedServerTime.get(a);
                            long timeB = finishedServerTime.get(b);
                            return (int) (timeA - timeB);
                        }).toList();
                        for (int i = 0; i < ordered.size(); i++) {
                            broadcast("%d位: %s", i + 1, ToColoredString(ordered.get(i))).log();
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
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (player.getWorld() != delegate.mainGetWorld()) {
            return;
        }
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
            } else if (location.equals(offset(kResetButtonNorth)) || location.equals(offset(kResetButtonSouth))) {
                if (player.getGameMode() == GameMode.CREATIVE || player.isOp()) {
                    competitionReset();
                    return;
                }
            }
        }
        if (_status == Status.RUN && item != null && (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)) {
            ItemMeta meta = item.getItemMeta();
            Participation participation = getCurrentParticipation(player);
            if (participation != null && meta != null && participation.role == Role.SHOOTER && meta.getPersistentDataContainer().has(NamespacedKey.minecraft(kItemTag), PersistentDataType.BYTE)) {
                PotionEffect candidate = null;
                if (kPrimaryShootItemDisplayName.equals(meta.getDisplayName())) {
                    item.setAmount(0);
                    broadcast("%s が暗闇（弱）を発動！", ToColoredString(participation.color)).log();
                    candidate = new PotionEffect(PotionEffectType.DARKNESS, 200, 1);
                } else if (kSecondaryShootItemDisplayName.equals(meta.getDisplayName())) {
                    item.setAmount(0);
                    broadcast("%s が暗闇（強）を発動！", ToColoredString(participation.color)).log();
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

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        if (e.getFrom() != delegate.mainGetWorld()) {
            return;
        }
        @Nullable Participation current = getCurrentParticipation(player);
        if (current == null) {
            return;
        }
        onClickLeave(e.getPlayer());
    }

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
            AllTeamColorsAndRoles((color, role) -> {
                var team = ensureScoreboardTeam(color, role);
                team.getEntries().forEach(team::removeEntry);
            });
        }, loadDelay);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onBlockRedstoneEvent(BlockRedstoneEvent e) {
        if (e.getOldCurrent() != 0 || e.getNewCurrent() <= 0) {
            return;
        }
        Block block = e.getBlock();
        if (block.getWorld() != delegate.mainGetWorld()) {
            return;
        }

        Location location = block.getLocation();
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
        if (boat.getWorld() != delegate.mainGetWorld()) {
            return;
        }
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

    private ConsoleLogger broadcast(String format, Object... args) {
        String msg = String.format(format, args);
        World world = delegate.mainGetWorld();
        Players.Within(world, new BoundingBox[]{offset(kAnnounceBoundsNorth), offset(kAnnounceBoundsSouth)}, player -> player.sendMessage(msg));
        return new ConsoleLogger(msg, "[水上レース]", delegate.mainGetLogger());
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private ConsoleLogger broadcastUnofficial(String msg, Object... args) {
        return broadcast(msg, args);
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
            player.sendMessage(String.format("[水上レース] %sは既に%sにエントリー済みです", player.getName(), CompetitionTypeHelper.ToString(type)));
            return;
        }
        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) {
            player.sendMessage("[水上レース] ゲームモードはサバイバルかアドベンチャーの場合のみ参加可能です");
            return;
        }
        @Nullable Participation current = getCurrentParticipation(player);
        if (current != null) {
            // NOTE: 本家は全チャになる
            player.sendMessage(String.format("[水上レース] %sは%sにエントリー済みです", player.getName(), ToColoredString(color)));
            return;
        }
        Team team = ensureTeam(color);
        Player currentRolePlaying = team.getPlayer(role);
        if (currentRolePlaying != null && !currentRolePlaying.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(String.format("[水上レース] %sの%sには%sがエントリー済みです", ToColoredString(color), ToString(role), currentRolePlaying.getName()));
            return;
        }

        delegate.mainClearCompetitionItems(player);
        clearScoreboardTeam(player);

        PlayerInventory inventory = player.getInventory();
        if (role == Role.DRIVER) {
            ItemStack boat =
                    ItemBuilder.For(Boat(color))
                            .amount(1)
                            .customByteTag(kItemTag, (byte) 1)
                            .build();
            var failed = inventory.addItem(boat);
            if (!failed.isEmpty()) {
                player.sendMessage(ChatColor.RED + "インベントリがいっぱいで競技用アイテムが渡せません");
                clearItems(player);
                return;
            }
        } else {
            ItemStack snowball =
                    ItemBuilder.For(Material.SNOWBALL)
                            .amount(1)
                            .displayName(kPrimaryShootItemDisplayName)
                            .customByteTag(kItemTag, (byte) 1)
                            .build();
            ItemStack crossbow =
                    ItemBuilder.For(Material.CROSSBOW)
                            .amount(1)
                            .customByteTag(kItemTag, (byte) 1)
                            .build();
            ItemStack splashPotion =
                    ItemBuilder.For(Material.SPLASH_POTION)
                            .amount(1)
                            .displayName(kSecondaryShootItemDisplayName)
                            .customByteTag(kItemTag, (byte) 1)
                            .potion(PotionType.UNCRAFTABLE)
                            .build();
            Color c = Color.fromRGB(FireworkRocketColor(color));
            FireworkEffect effect = FireworkEffect.builder().withColor(c).withFade(c).build();
            ItemStack fireworkRocket =
                    ItemBuilder.For(Material.FIREWORK_ROCKET)
                            .amount(3)
                            .customByteTag(kItemTag, (byte) 1)
                            .firework(effect)
                            .build();

            var failed = inventory.addItem(snowball, crossbow, splashPotion);
            if (!failed.isEmpty()) {
                player.sendMessage(ChatColor.RED + "インベントリがいっぱいで競技用アイテムが渡せません");
                clearItems(player);
                return;
            }
            ItemStack offHand = inventory.getItemInOffHand();
            if (offHand.getType() == Material.AIR) {
                inventory.setItemInOffHand(fireworkRocket);
            } else {
                int index = inventory.firstEmpty();
                if (index < 0) {
                    player.sendMessage(ChatColor.RED + "インベントリがいっぱいで競技用アイテムが渡せません");
                    clearItems(player);
                    return;
                }
                inventory.setItem(index, offHand);
                inventory.setItemInOffHand(fireworkRocket);
            }
        }

        Team p = ensureTeam(color);
        p.setPlayer(role, player);
        var scoreboardTeam = ensureScoreboardTeam(color, role);
        scoreboardTeam.addPlayer(player);
        var prefix = scoreboardTeam.prefix();
        var style = prefix.style();
        broadcast("[水上レース] %sが%s%sにエントリーしました", player.getName(), ToColoredString(color), ToString(role)).log();
        setStatus(Status.AWAIT_START);
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
        clearScoreboardTeam(player);
        broadcast("[水上レース] %sがエントリー解除しました", player.getName()).log();

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

    private void giveParticipationReward(Player player) {
        ItemStack cookedBeef = ItemBuilder.For(Material.COOKED_BEEF)
                .amount(15)
                .build();
        player.getInventory().addItem(cookedBeef);
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
        AllTeamColors(color -> {
            Team team = ensureTeam(color);
            int count = team.getPlayerCount();
            if (count < 1) {
                broadcast("%sの参加者が見つかりません", ToString(color));
            } else {
                broadcast("%s が競技に参加します（参加者%d人）", ToColoredString(color), count).log();
            }
        });
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
        broadcast("[水上レース] 競技を開始します！").log();
        broadcast("");
        setStatus(Status.COUNTDOWN);

        // 場内に居るボートに tag を付ける. 競技終了した時このタグが付いているボートを kill する.
        World world = delegate.mainGetWorld();
        world.getNearbyEntities(getFieldBounds()).forEach(entity -> {
            if (entity.getType() == EntityType.BOAT) {
                entity.addScoreboardTag(kItemTag);
            }
        });

        delegate.mainCountdownThen(new BoundingBox[]{offset(kAnnounceBoundsNorth), offset(kAnnounceBoundsSouth)}, (count) -> _status == Status.COUNTDOWN, () -> {
            if (_status != Status.COUNTDOWN) {
                return false;
            }
            setStatus(Status.RUN);
            return true;
        }, 20, Countdown.TitleSet.Default());
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

    @Override
    public void competitionReset() {
        setStatus(Status.IDLE);
        resetField();
        finishedServerTime.clear();
        teams.clear();
        for (TeamColor color : new TeamColor[]{TeamColor.RED, TeamColor.YELLOW, TeamColor.WHITE}) {
            for (Role role : new Role[]{Role.DRIVER, Role.SHOOTER}) {
                var team = ensureScoreboardTeam(color, role);
                team.getEntries().forEach(team::removeEntry);
            }
        }

        String message = CompetitionTypeHelper.ToString(competitionGetType()) + "をリセットしました";
        Bukkit.getServer().getOnlinePlayers().forEach(player -> player.sendMessage(message));
        delegate.mainGetLogger().info(message);
    }
}