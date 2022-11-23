package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDeathEvent;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobFightEventListener implements Listener, LevelDelegate, Competition {
    private boolean initialized = false;
    private final Map<TeamColor, Level> levels = new HashMap<>();
    private final Map<TeamColor, Team> teams = new HashMap<>();
    private Status _status = Status.IDLE;
    private @Nullable Race race;
    private final Map<TeamColor, Bossbar> bossbars = new HashMap<>();
    private final long loadDelay;
    private final MainDelegate delegate;

    public MobFightEventListener(MainDelegate delegate, long loadDelay) {
        this.loadDelay = loadDelay;
        this.delegate = delegate;
    }

    void setStatus(Status s) {
        if (_status == s) {
            return;
        }
        _status = s;
        switch (_status) {
            case IDLE:
                resetField();
                Bukkit.getServer().getOnlinePlayers().forEach(this::clearItem);
                for (Level level : levels.values()) {
                    level.reset();
                }
                for (Map.Entry<TeamColor, Bossbar> it : bossbars.entrySet()) {
                    it.getValue().setVisible(false);
                }
                break;
            case AWAIT_COUNTDOWN:
                for (Level level : levels.values()) {
                    Point3i safe = level.getSafeSpawnLocation();
                    Players.Within(level.getBounds(), player -> player.teleport(player.getLocation().set(safe.x, safe.y, safe.z)));
                    level.reset();
                }
                for (Map.Entry<TeamColor, Bossbar> it : bossbars.entrySet()) {
                    it.getValue().setVisible(false);
                }
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
        if (!initialized) {
            initialized = true;
            delegate.mainRunTaskLater(() -> {
                BoundingBox box = offset(kAnnounceBounds);
                Bossbar red = new Bossbar(delegate, kBossbarRed, "", box);
                red.setColor(BarColor.RED);
                Bossbar yellow = new Bossbar(delegate, kBossbarYellow, "", box);
                yellow.setColor(BarColor.YELLOW);
                Bossbar white = new Bossbar(delegate, kBossbarWhite, "", box);
                white.setColor(BarColor.WHITE);
                bossbars.put(TeamColor.RED, red);
                bossbars.put(TeamColor.YELLOW, yellow);
                bossbars.put(TeamColor.WHITE, white);
                resetField();
            }, loadDelay);
        }
        Player player = e.getPlayer();
        if (_status != Status.RUN || getCurrentParticipation(player) == null) {
            for (Level level : levels.values()) {
                if (level.getBounds().contains(player.getLocation().toVector())) {
                    Point3i safe = level.getSafeSpawnLocation();
                    player.teleport(player.getLocation().set(safe.x, safe.y, safe.z));
                }
            }
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onEntityDeathEvent(EntityDeathEvent e) {
        if (_status != Status.RUN || race == null) {
            return;
        }
        Entity entity = e.getEntity();
        Vector location = entity.getLocation().toVector();
        for (Map.Entry<TeamColor, Level> it : levels.entrySet()) {
            Level level = it.getValue();
            TeamColor color = it.getKey();
            if (!race.getTeamColors().contains(color)) {
                continue;
            }
            Progress current = level.getProgress();
            Stage stage = level.getStage(current.stage);
            if (stage == null) {
                continue;
            }
            boolean consumed = level.consumeDeadMob(entity);
            if (consumed) {
                e.setDroppedExp(0);
            }
            Progress next = level.getProgress();
            applyBossbarValue(color, stage.getBossbarValue());
            if (current.equals(next)) {
                continue;
            }
            Stage nextStage = level.getStage(next.stage);
            if (nextStage == null) {
                continue;
            }
            if (current.stage == next.stage) {
                // 同一 stage の次の step に
                delegate.mainRunTaskLater(() -> {
                    if (this.race == null) {
                        return;
                    }
                    if (!this.race.getTeamColors().contains(color)) {
                        return;
                    }
                    stage.summonMobs(next.step);
                }, 20 * 3);
            } else {
                // 次の stage へ
                level.showTitle("WAVE CLEAR !", Color.YELLOW);
                broadcast("%s %s CLEAR !", ToColoredString(color), stage.getMessageDisplayString()).log(kLogPrefix);
                delegate.mainRunTaskLater(() -> {
                    if (this.race == null) {
                        return;
                    }
                    if (!this.race.getTeamColors().contains(color)) {
                        return;
                    }
                    Team team = ensureTeam(color);
                    List<Player> players = new ArrayList<>();
                    team.usePlayers(players::add);
                    stage.setExitOpened(true);
                    nextStage.onStart(players);
                    nextStage.summonMobs(0);
                    nextStage.setEntranceOpened(true);
                    applyBossbarValue(color, nextStage.getBossbarValue());
                }, 20 * 3);
            }
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

    @EventHandler
    @SuppressWarnings("unused")
    public void onBlockRedstoneEvent(BlockRedstoneEvent e) {
        if (e.getOldCurrent() != 0 || e.getNewCurrent() <= 0) {
            return;
        }

        Point3i location = new Point3i(e.getBlock().getLocation());
        if (location.equals(offset(kButtonYellowStart)) || location.equals(offset(kButtonRedStart)) || location.equals(offset(kButtonWhiteStart))) {
            onClickStart();
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        Vector location = player.getLocation().toVector();

        Participation participation = getCurrentParticipation(player);
        if (participation != null) {
            if (!offset(kAnnounceBounds).contains(location)) {
                player.sendMessage(ChatColor.RED + "[MOB討伐レース] 場外に出たためエントリー解除となります");
                onClickLeave(player);
                return;
            }
        }

        if (_status != Status.RUN) {
            return;
        }
        Race race = this.race;
        if (race == null) {
            return;
        }

        for (Map.Entry<TeamColor, Level> it : levels.entrySet()) {
            TeamColor color = it.getKey();
            Level level = it.getValue();

            Team team = ensureTeam(color);
            if (team.getCurrentRole(player) == null) {
                if (level.containsInBounds(location) && player.getGameMode() != GameMode.SPECTATOR) {
                    // 競技場内に侵入した非参加者を安全地帯に移動させる
                    Point3i spawn = level.getSafeSpawnLocation();
                    Location l = player.getLocation();
                    l.setX(spawn.x);
                    l.setY(spawn.y);
                    l.setZ(spawn.z);
                    player.teleport(l);
                }
                continue;
            }

            Progress progress = level.getProgress();
            if (progress.stage != level.getStageCount() - 1) {
                continue;
            }

            FinalStage stage = level.finalStage;
            if (!stage.isCreeperSpawned() && stage.getCreeperSpawnBounds().contains(location)) {
                applyBossbarValue(color, new BossbarValue(0, team.getPlayerCount(), "GO TO GOAL !!"));
                stage.summonCreepers();
            }
            if (!team.isPlayerFinished(player) && stage.getGoalDetectionBounds().contains(location)) {
                int finishedPlayerCount = team.setFinished(player);
                if (finishedPlayerCount == team.getPlayerCount()) {
                    broadcast("%s GAME CLEAR !!", ToColoredString(color)).log(kLogPrefix);
                    level.showTitle("GAME CLEAR !!", Color.fromRGB(0xFFAA00)); // gold
                    level.launchFireworkRockets(FireworkRocketColor(color));
                    applyBossbarValue(color, new BossbarValue(team.getPlayerCount(), team.getPlayerCount(), "GAME CLEAR !!"));
                    race.pushOrder(color);

                    boolean allTeamCleared = true;
                    for (TeamColor tc : race.getTeamColors()) {
                        if (tc == color) {
                            continue;
                        }
                        Team t = ensureTeam(tc);
                        if (!t.isCleared()) {
                            allTeamCleared = false;
                            break;
                        }
                    }
                    if (allTeamCleared) {
                        for (TeamColor tc : race.getTeamColors()) {
                            Level l = levels.get(tc);
                            if (l != null) {
                                l.setExitOpened(true);
                            }
                        }

                        broadcast("");
                        broadcast("-----------------------");
                        broadcast("[結果発表]").log(kLogPrefix);
                        for (int i = 0; i < race.order.size(); i++) {
                            Goal goal = race.order.get(i);
                            broadcast("%d位 : %s (%.2f 秒)", i + 1, ToColoredString(goal.color), goal.seconds).log(kLogPrefix);
                        }
                        broadcast("-----------------------");
                        broadcast("");
                        for (TeamColor tc : race.getTeamColors()) {
                            Team t = ensureTeam(tc);
                            t.reset();
                        }
                        this.race = null;
                        setStatus(Status.IDLE);
                        return;
                    }
                } else {
                    applyBossbarValue(color, new BossbarValue(finishedPlayerCount, team.getPlayerCount(), "GO TO GOAL !!"));
                }
            }
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        onClickLeave(player);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (_status != Status.RUN) {
            return;
        }
        Player player = e.getPlayer();
        Participation participation = getCurrentParticipation(player);
        if (participation == null) {
            return;
        }
        Level level = ensureLevel(participation.color);
        Progress progress = level.getProgress();
        Stage stage = level.getStage(progress.stage);
        Point3i respawn = level.getSafeSpawnLocation();
        if (stage != null) {
            respawn = stage.getRespawnLocation();
        }
        Location location = player.getLocation();
        location.setX(respawn.x);
        location.setY(respawn.y);
        location.setZ(respawn.z);
        e.setRespawnLocation(location);
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

    void applyBossbarValue(TeamColor color, BossbarValue value) {
        Bossbar bar = bossbars.get(color);
        if (bar == null) {
            return;
        }
        bar.setMax(value.max);
        bar.setValue(value.value);
        bar.setName(String.format("%s : %s", ToColoredString(color), value.title));
    }

    void setBossbarVisible(TeamColor color, boolean visible) {
        Bossbar bar = bossbars.get(color);
        if (bar == null) {
            return;
        }
        bar.setVisible(visible);
    }

    void onClickJoin(Player player, TeamColor color, Role role) {
        if (_status != Status.IDLE && _status != Status.AWAIT_COUNTDOWN) {
            return;
        }
        CompetitionType type = delegate.mainGetCurrentCompetition(player);
        if (type != null && type != CompetitionType.MOB) {
            broadcastUnofficial("[MOB討伐レース] %sは既に%sにエントリー済みです", player.getName(), CompetitionTypeHelper.ToString(type));
            return;
        }
        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) {
            player.sendMessage("[MOB討伐レース] ゲームモードはサバイバルかアドベンチャーの場合のみ参加可能です");
            return;
        }
        Participation current = getCurrentParticipation(player);
        if (current != null) {
            //NOTE: 本家では全チャになる
            player.sendMessage(String.format("[MOB討伐レース] %sは%sにエントリー済みです", player.getName(), ToColoredString(current.color)));
            return;
        }
        Team team = ensureTeam(color);
        team.add(player, role);
        broadcast("[MOB討伐レース] %sが%s%sにエントリーしました", player.getName(), ToColoredString(color), ToString(role)).log();

        delegate.mainClearCompetitionItems(player);
        PlayerInventory inventory = player.getInventory();
        ItemStack leggings = ItemBuilder.For(Material.IRON_LEGGINGS)
                .amount(1)
                .customByteTag(kItemTag, (byte) 1)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4)
                .enchant(Enchantment.DURABILITY, 3)
                .build();
        ItemStack chestplate = ItemBuilder.For(Material.IRON_CHESTPLATE)
                .amount(1)
                .customByteTag(kItemTag, (byte) 1)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4)
                .enchant(Enchantment.DURABILITY, 3)
                .build();
        ItemStack helmet = ItemBuilder.For(Material.IRON_HELMET)
                .amount(1)
                .customByteTag(kItemTag, (byte) 1)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4)
                .enchant(Enchantment.OXYGEN, 3)
                .enchant(Enchantment.DURABILITY, 3)
                .build();
        ItemStack boots = ItemBuilder.For(Material.IRON_BOOTS)
                .amount(1)
                .customByteTag(kItemTag, (byte) 1)
                .enchant(Enchantment.DEPTH_STRIDER, 3)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4)
                .enchant(Enchantment.DURABILITY, 3)
                .build();
        ItemStack goldenApple = ItemBuilder.For(Material.GOLDEN_APPLE)
                .amount(35)
                .customByteTag(kItemTag, (byte) 1)
                .build();
        ItemStack cookedBeef = ItemBuilder.For(Material.COOKED_BEEF)
                .amount(35)
                .customByteTag(kItemTag, (byte) 1)
                .build();
        inventory.addItem(leggings, chestplate, helmet, boots, goldenApple, cookedBeef);
        switch (role) {
            case ARROW:
                ItemStack bow = ItemBuilder.For(Material.BOW)
                        .amount(1)
                        .customByteTag(kItemTag, (byte) 1)
                        .enchant(Enchantment.ARROW_INFINITE, 1)
                        .enchant(Enchantment.ARROW_DAMAGE, 5)
                        .enchant(Enchantment.DURABILITY, 3)
                        .build();
                ItemStack arrow = ItemBuilder.For(Material.ARROW)
                        .amount(1)
                        .customByteTag(kItemTag, (byte) 1)
                        .build();
                inventory.addItem(bow, arrow);
                break;
            case SWORD:
                ItemStack shield = ItemBuilder.For(Material.SHIELD)
                        .amount(1)
                        .customByteTag(kItemTag, (byte) 1)
                        .enchant(Enchantment.DURABILITY, 3)
                        .build();
                ItemStack sword = ItemBuilder.For(Material.IRON_SWORD)
                        .amount(1)
                        .customByteTag(kItemTag, (byte) 1)
                        .enchant(Enchantment.KNOCKBACK, 1)
                        .enchant(Enchantment.DAMAGE_UNDEAD, 5)
                        .enchant(Enchantment.DURABILITY, 3)
                        .build();
                inventory.addItem(shield, sword);
                break;
        }
        setStatus(Status.AWAIT_COUNTDOWN);
    }

    void onClickLeave(Player player) {
        Participation current = getCurrentParticipation(player);
        if (current == null) {
            return;
        }
        Team team = ensureTeam(current.color);
        team.remove(player);
        clearItem(player);
        broadcast("[MOB討伐レース] %sがエントリー解除しました", player.getName()).log();

        if (team.getPlayerCount() == 0) {
            if (race != null) {
                race.remove(current.color);
            }
            setBossbarVisible(current.color, false);
            Level level = ensureLevel(current.color);
            level.reset();
        }

        if (_status == Status.RUN || _status == Status.COUNTDOWN) {
            if (getPlayerCount() == 0) {
                setStatus(Status.IDLE);
            }
        } else {
            if (getPlayerCount() > 0) {
                setStatus(Status.AWAIT_COUNTDOWN);
            } else {
                setStatus(Status.IDLE);
            }
        }
    }

    void onClickStart() {
        if (_status != Status.AWAIT_COUNTDOWN) {
            return;
        }
        if (getPlayerCount() < 1) {
            // ここには来ないはず
            broadcastUnofficial(ChatColor.RED + "参加者が見つかりません");
            return;
        }
        broadcast("");
        broadcast("-----------------------");
        Race race = new Race();
        for (Map.Entry<TeamColor, Team> it : teams.entrySet()) {
            TeamColor color = it.getKey();
            Team team = it.getValue();
            int count = team.getPlayerCount();
            if (count > 0) {
                broadcast("%s が競技に参加します（参加者%d人）", ToColoredString(color), count).log(kLogPrefix);
                race.add(color);
            } else {
                broadcast("%sの参加者が見つかりません", ToString(color));
            }
        }
        broadcast("-----------------------");
        broadcast("");
        broadcast("[MOB討伐レース] 競技を開始します！").log();
        broadcast("");
        setStatus(Status.COUNTDOWN);
        for (Level level : levels.values()) {
            Point3i safe = level.getSafeSpawnLocation();
            Players.Within(level.getBounds(), player -> player.teleport(player.getLocation().set(safe.x, safe.y, safe.z)));
        }
        delegate.mainCountdownThen(new BoundingBox[]{offset(kAnnounceBounds)}, (count) -> _status == Status.COUNTDOWN, () -> {
            if (_status != Status.COUNTDOWN) {
                return false;
            }
            race.memoStartTime();
            this.race = race;
            for (TeamColor color : race.getTeamColors()) {
                Level level = ensureLevel(color);
                level.reset();
                level.setExitOpened(false);
                Stage stage = level.getStage(0);
                assert stage != null;
                Team team = ensureTeam(color);
                ArrayList<Player> players = new ArrayList<>();
                team.usePlayers(players::add);
                stage.onStart(players);
                stage.summonMobs(0);
                stage.setEntranceOpened(true);
                applyBossbarValue(color, stage.getBossbarValue());
                setBossbarVisible(color, true);
            }
            setStatus(Status.RUN);
            return true;
        }, 20);
    }

    int getPlayerCount() {
        int count = 0;
        for (Map.Entry<TeamColor, Team> it : teams.entrySet()) {
            count += it.getValue().getPlayerCount();
        }
        return count;
    }

    void clearItem(Player player) {
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
        delegate.mainUsingChunk(offset(kAnnounceBounds), world -> {
            Editor.WallSign(offset(kButtonWhiteLeave), BlockFace.SOUTH, "エントリー解除");
            Editor.WallSign(offset(kButtonWhiteJoinArrow), BlockFace.SOUTH, "白組", "エントリー", "（弓）");
            Editor.WallSign(offset(kButtonWhiteJoinSword), BlockFace.SOUTH, "白組", "エントリー", "（剣）");

            Editor.WallSign(offset(kButtonRedLeave), BlockFace.SOUTH, "エントリー解除");
            Editor.WallSign(offset(kButtonRedJoinArrow), BlockFace.SOUTH, "赤組", "エントリー", "（弓）");
            Editor.WallSign(offset(kButtonRedJoinSword), BlockFace.SOUTH, "赤組", "エントリー", "（剣）");

            Editor.WallSign(offset(kButtonYellowLeave), BlockFace.SOUTH, "エントリー解除");
            Editor.WallSign(offset(kButtonYellowJoinArrow), BlockFace.SOUTH, "黃組", "エントリー", "（弓）");
            Editor.WallSign(offset(kButtonYellowJoinSword), BlockFace.SOUTH, "黃組", "エントリー", "（剣）");

            for (TeamColor tc : kColors) {
                ensureLevel(tc);
            }
        });
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

    @Nullable
    @Override
    public World levelGetWorld() {
        return delegate.mainGetWorld();
    }

    private ConsoleLogger broadcast(String format, Object... args) {
        String msg = String.format(format, args);
        Players.Within(offset(kAnnounceBounds), player -> player.sendMessage(msg));
        return new ConsoleLogger(msg, delegate.mainGetLogger());
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private ConsoleLogger broadcastUnofficial(String msg, Object... args) {
        return broadcast(msg, args);
    }

    @Override
    public boolean competitionIsJoined(Player player) {
        return getCurrentParticipation(player) != null;
    }

    @NotNull
    @Override
    public CompetitionType competitionGetType() {
        return CompetitionType.MOB;
    }

    @Override
    public void competitionClearItems(Player player) {
        clearItem(player);
    }

    static class Participation {
        final TeamColor color;
        final Role role;

        Participation(TeamColor color, Role role) {
            this.color = color;
            this.role = role;
        }
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

    static String ToString(Role role) {
        switch (role) {
            case ARROW:
                return "（弓）";
            case SWORD:
                return "（剣）";
        }
        return "";
    }

    static int FireworkRocketColor(TeamColor color) {
        switch (color) {
            case YELLOW:
                return FireworkRocket.Color.YELLOW;
            case WHITE:
                return FireworkRocket.Color.LIGHT_BLUE;
            case RED:
                return FireworkRocket.Color.PINK;
        }
        return 0;
    }

    static final String kItemTag = "hololive_sports_festival_2022_mob";

    private static final BoundingBox kAnnounceBounds = new BoundingBox(-26, -61, -424, 83, -19, -243);

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

    private static final Point3i kButtonYellowStart = new Point3i(-3, -58, -254);
    private static final Point3i kButtonRedStart = new Point3i(28, -58, -254);
    private static final Point3i kButtonWhiteStart = new Point3i(59, -58, -254);

    private static final String kBossbarRed = "hololive_sports_festival_2022_bossbar_red";
    private static final String kBossbarWhite = "hololive_sports_festival_2022_bossbar_white";
    private static final String kBossbarYellow = "hololive_sports_festival_2022_bossbar_yellow";

    private static final String kLogPrefix = "[MOB討伐レース]";
}