package com.github.kbinani.holosportsfestival2022.daruma;

import com.github.kbinani.holosportsfestival2022.*;
import org.bukkit.*;
import org.bukkit.block.*;
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
import org.bukkit.inventory.Inventory;
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
import java.security.SecureRandom;
import java.util.*;

public class DarumaEventListener implements Listener, Announcer, Competition {
    private boolean initialized = false;
    private Status _status = Status.IDLE;
    private @Nullable Race race;
    private boolean manual = true;
    private final long loadDelay;
    private final MainDelegate delegate;
    private final Map<UUID, Point3i> respawn = new HashMap<>();
    private final Random random;
    private final Map<TeamColor, Team> teams = new HashMap<>();
    private boolean blockGreenSignal = false;
    private final Map<UUID, Vector> positionWhenRed = new HashMap<>();

    public DarumaEventListener(MainDelegate delegate, long loadDelay) {
        this.loadDelay = loadDelay;
        this.delegate = delegate;
        Random random = null;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (Throwable e) {
            random = new Random();
        }
        this.random = random;
        delegate.mainRunTaskTimer(this::onTick, 0, 20);
    }

    private void onTick() {
        Calendar now = GregorianCalendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        Calendar next = getNextAutoStart();
        Calendar last = (Calendar) next.clone();
        last.add(Calendar.MINUTE, -kAutoStartIntervalMinutes);

        long wait = next.getTimeInMillis() - now.getTimeInMillis();
        long passed = now.getTimeInMillis() - last.getTimeInMillis();

        switch (_status) {
            case IDLE:
                if (wait <= kTimerIntervalMillis * 0.5 || passed <= kTimerIntervalMillis * 0.5) {
                    manual = false;
                    start();
                } else {
                    int stay = kTimerIntervalMillis * 20 / 1000 + 20;
                    String titleString = "";
                    String subtitleString = "";
                    if (30 * 1000 >= wait) {
                        int seconds = (int) (wait / 1000);
                        titleString = String.format("開始まであと %d 秒です！", seconds);
                        subtitleString = "";
                    } else {
                        titleString = "";
                        subtitleString = String.format("次回のスタートは %02d 時 %02d 分です (JST)", next.get(Calendar.HOUR_OF_DAY), next.get(Calendar.MINUTE));
                    }
                    final String title = titleString;
                    final String subtitle = subtitleString;
                    Players.Within(delegate.mainGetWorld(), getAnnounceBounds(), player -> player.sendTitle(title, subtitle, 0, stay, 20));
                }
                break;
            case START:
            case RED:
                if (!manual) {
                    if (60 * 1000 >= wait) {
                        // 次回のスタート時刻まで 60 秒を切っているにもかかわらず競技が続いている.
                        // AFK によって次回のスタートが阻止されるのを防ぐため強制的に競技を止める.
                        broadcastUnofficial("[だるまさんがころんだ] 競技が時間内に終わらなかったため強制終了となります").log();
                        setStatus(Status.IDLE);
                        break;
                    }
                    int random = this.random.nextInt(100);
                    if (random < 50 && !blockGreenSignal) {
                        triggerGreen();
                    }
                }
                break;
            case GREEN:
                if (!manual) {
                    if (60 * 1000 >= wait) {
                        // 次回のスタート時刻まで 60 秒を切っているにもかかわらず競技が続いている.
                        // AFK によって次回のスタートが阻止されるのを防ぐため強制的に競技を止める.
                        broadcastUnofficial("[だるまさんがころんだ] 競技が時間内に終わらなかったため強制終了となります").log();
                        setStatus(Status.IDLE);
                        break;
                    }
                    int random = this.random.nextInt(100);
                    if (random < 20) {
                        triggerRed();
                    }
                }
                break;
        }
    }

    private Calendar getNextAutoStart() {
        Calendar now = GregorianCalendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        Calendar next = (Calendar) now.clone();
        int index = next.get(Calendar.MINUTE) / kAutoStartIntervalMinutes;
        int minutes = index * kAutoStartIntervalMinutes;
        next.set(Calendar.MILLISECOND, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MINUTE, minutes);
        while (next.before(now)) {
            next.add(Calendar.MINUTE, kAutoStartIntervalMinutes);
        }
        while (next.after(now)) {
            next.add(Calendar.MINUTE, -kAutoStartIntervalMinutes);
        }
        next.add(Calendar.MINUTE, kAutoStartIntervalMinutes);

        return next;
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!initialized) {
            initialized = true;
            delegate.mainRunTaskLater(this::resetField, loadDelay);
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
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (player.getWorld() != delegate.mainGetWorld()) {
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
        } else if (manual && location.equals(offset(kButtonGreen))) {
            onClickGreen(player);
        } else if (manual && location.equals(offset(kButtonTriggerRed))) {
            onClickTriggerRed(player);
        } else if (location.equals(offset(kButtonReset))) {
            if (player.getGameMode() == GameMode.CREATIVE || player.isOp()) {
                competitionReset();
            }
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        World world = player.getWorld();
        if (delegate.mainGetWorld() != world) {
            return;
        }
        TeamColor color = getCurrentColor(player);
        if (color == null) {
            return;
        }
        if (!offset(kAnnounceBounds).contains(player.getLocation().toVector())) {
            player.sendMessage(ChatColor.RED + "[だるまさんがころんだ] 場外に出たためエントリー解除となります");
            onClickLeave(player);
            return;
        }
        final Race race = this.race;
        if (race == null) {
            return;
        }
        if (!race.isRunning(player)) {
            return;
        }
        Team team = ensureTeam(color);

        Vector location = player.getLocation().toVector();
        BoundingBox goal = offset(kGoalDetectionBox);
        BoundingBox kill = offset(kKillZone);
        BoundingBox habitable = offset(kHabitableZone);

        switch (_status) {
            case GREEN:
            case START:
            case COUNTDOWN_RED:
                if (goal.contains(location)) {
                    // ゴールラインを超えた時刻を計算する.
                    Vector from = e.getFrom().toVector();
                    Vector to = e.getTo().toVector();
                    double z = goal.getMaxZ();
                    double fromZ = from.getZ();
                    double toZ = to.getZ();
                    double tick = world.getFullTime();
                    if (fromZ != toZ && toZ <= z && z <= fromZ) {
                        tick = (z - fromZ) / (toZ - fromZ) + world.getFullTime() - 1;
                    }

                    launchFireworkRocket(xd(134.5), yd(-47.5), zd(-223.5), FireworkRocket.Color.LIGHT_BLUE);
                    launchFireworkRocket(xd(124.5), yd(-47.5), zd(-223.5), FireworkRocket.Color.PINK);
                    launchFireworkRocket(xd(114.5), yd(-47.5), zd(-223.5), FireworkRocket.Color.YELLOW);

                    race.goal(player, tick);
                    team.remove(player);
                    clearItem(player);
                    giveParticipationReward(player);

                    // 同一 tick で box に侵入したという判定になったとしても,
                    // 駆け込んだ時の速度によってはゴールラインを超えた時刻は他の人の方が早いかもしれない.
                    // 1 tick 待ってから順位を発表する.
                    delegate.mainRunTask(() -> {
                        if (this.race == null) {
                            return;
                        }
                        this.race.announceOrder(this, player);
                        if (this.race.getRunningPlayerCount() < 1) {
                            setStatus(Status.IDLE);
                        }
                    });
                } else if (!habitable.contains(location)) {
                    player.sendMessage(ChatColor.RED + "[だるまさんがころんだ] 場外に出たため失格となります");
                    disqualify(player);
                }
                break;
            case RED:
                if (!habitable.contains(location)) {
                    player.sendMessage(ChatColor.RED + "[だるまさんがころんだ] 場外に出たため失格となります");
                    disqualify(player);
                } else if (kill.contains(location)) {
                    Vector from = positionWhenRed.get(player.getUniqueId());
                    if (from == null) {
                        positionWhenRed.put(player.getUniqueId(), location);
                    } else {
                        double dx = location.getX() - from.getX();
                        double dz = location.getZ() - from.getZ();
                        if (dx != 0 || dz != 0) {
                            disqualify(player);
                        }
                    }
                }
                break;
        }
    }

    private void disqualify(Player player) {
        clearItem(player);
        giveParticipationReward(player);
        player.setHealth(0);
    }

    private void launchFireworkRocket(double x, double y, double z, int color) {
        World world = delegate.mainGetWorld();
        FireworkRocket.Launch(world, x, y, z, new int[]{color}, new int[]{color}, 20, 1, false, false);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        Point3i pos = respawn.get(player.getUniqueId());
        if (pos == null) {
            return;
        }
        respawn.remove(player.getUniqueId());
        Location location = new Location(delegate.mainGetWorld(), pos.x + 0.5, pos.y, pos.z + 6.5);
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

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (_status == Status.IDLE || _status == Status.COUNTDOWN_START) {
            return;
        }
        Race race = this.race;
        if (race == null) {
            return;
        }
        Player player = e.getEntity();
        TeamColor color = getCurrentColor(player);
        if (color == null) {
            return;
        }
        Team team = ensureTeam(color);
        team.remove(player);
        race.withdraw(player);

        Point3i pos = getEntryButtonPosition(color);
        respawn.put(player.getUniqueId(), pos);

        broadcast("%s失格！", player.getName()).log();

        if (race.getRunningPlayerCount() == 0) {
            // 最後の走者が失格になったので試合終了
            setStatus(Status.IDLE);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onBlockRedstoneEvent(BlockRedstoneEvent e) {
        if (e.getOldCurrent() != 0 || e.getNewCurrent() <= 0) {
            return;
        }
        World world = delegate.mainGetWorld();
        if (e.getBlock().getWorld() != world) {
            return;
        }

        Point3i location = new Point3i(e.getBlock().getLocation());
        for (Point3i button : kDummyButtons) {
            Point3i pos = offset(button);
            if (pos.equals(location)) {
                BoundingBox box = new BoundingBox(pos.x - 6, pos.y - 6, pos.z - 6, pos.x + 6, pos.y + 6, pos.z + 6);
                Players.Within(world, box, player -> {
                    if (player.getGameMode() == GameMode.CREATIVE) {
                        player.sendMessage(ChatColor.RED + "このボタンは無効になっています. 代わりに看板を右クリックしてください. op のみ操作可能です");
                    }
                });
                e.setNewCurrent(0);
                return;
            }
        }

        if (location.y != y(-60)) {
            return;
        }
        if (location.z == z(-202) || location.z == z(-198)) {
            int dx = location.x - x(104);
            if (dx < 0 || 40 < dx || dx % 2 != 0) {
                return;
            }
        } else if (location.z == z(-200)) {
            int dx = location.x - x(105);
            if (dx < 0 || 38 < dx || dx % 2 != 0) {
                return;
            }
        } else {
            return;
        }
        Block block = world.getBlockAt(location.x, location.y - 1, location.z);
        BlockState state = block.getState();
        if (!(state instanceof Dispenser dispenser)) {
            return;
        }
        Inventory inventory = dispenser.getInventory();
        inventory.clear();
        inventory.setItem(0, new ItemStack(Material.TNT, 1));
        dispenser.dispense();
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (_status == Status.IDLE) {
            return;
        }
        if (race == null) {
            return;
        }
        Entity entity = e.getEntity();
        if (entity.getWorld() != delegate.mainGetWorld()) {
            return;
        }
        Entity damager = e.getDamager();
        if (damager.getWorld() != delegate.mainGetWorld()) {
            return;
        }
        if (entity.getType() != EntityType.PLAYER || damager.getType() != EntityType.PLAYER) {
            return;
        }
        Player player = (Player) entity;
        Player attacker = (Player) damager;

        if (race.isRunning(player)) {
            e.setCancelled(true);

            if (race.isRunning(attacker)) {
                attacker.sendMessage(ChatColor.RED + "[だるまさんがころんだ] 他のプレイヤーを攻撃したため失格となります");
                disqualify(player);
            }
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        if (e.getFrom() != delegate.mainGetWorld()) {
            return;
        }
        onClickLeave(player);
    }

    private void clearDispensers() {
        World world = delegate.mainGetWorld();

        final int y = -61;
        for (int x = 104; x <= 144; x += 2) {
            clearDispenser(world, offset(new Point3i(x, y, -202)));
            clearDispenser(world, offset(new Point3i(x, y, -198)));
        }
        for (int x = 105; x <= 143; x += 2) {
            clearDispenser(world, offset(new Point3i(x, y, -200)));
        }
    }

    private void clearDispenser(World world, Point3i position) {
        int cx = position.x >> 4;
        int cz = position.z >> 4;
        boolean loaded = world.isChunkLoaded(cx, cz);
        if (!loaded) {
            world.loadChunk(cx, cz);
        }
        Block block = world.getBlockAt(position.x, position.y, position.z);
        BlockState state = block.getState();
        if (!(state instanceof Container)) {
            return;
        }
        Dispenser dispenser = (Dispenser) state;
        Inventory inventory = dispenser.getInventory();
        inventory.clear();
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
                Bukkit.getServer().getOnlinePlayers().forEach(this::clearItem);
                clearDispensers();
                if (race != null) {
                    race.announceOrders(this);
                }
                race = null;
                manual = false;
                blockGreenSignal = false;
                break;
            case COUNTDOWN_START:
                setEntranceOpened(false);
                setStartGateOpened(false);

                final BoundingBox startGrid = offset(kStartGridBounds);
                final BoundingBox habitable = offset(kHabitableZone);
                final Point3i safeRespawn = offset(kSafeSpawnLocation);
                Server server = Bukkit.getServer();
                World world = delegate.mainGetWorld();
                server.getOnlinePlayers().forEach(p -> {
                    if (world != p.getWorld()) {
                        return;
                    }
                    Location location = p.getLocation();
                    if (getCurrentColor(p) == null) {
                        // 場内に入ってしまっている人を移動
                        GameMode mode = p.getGameMode();
                        if (habitable.contains(location.toVector()) && (mode == GameMode.ADVENTURE || mode == GameMode.SURVIVAL)) {
                            location.setX(safeRespawn.x + 0.5);
                            location.setY(safeRespawn.y);
                            location.setZ(safeRespawn.z + 0.5);
                            p.teleport(location);
                        }
                    } else {
                        // 参加者だけどスタートラインの柵の中に居ない人を白線まで移動
                        if (!startGrid.contains(location.toVector())) {
                            location.setX(Clamp(location.getX(), startGrid.getMinX(), startGrid.getMaxX()));
                            location.setY(startGrid.getMinY());
                            location.setZ(zd(-120.5));
                            p.teleport(location);
                        }
                    }
                });
                break;
            case START:
                setEntranceOpened(false);
                setStartGateOpened(true);
                break;
            case GREEN:
            case COUNTDOWN_RED:
            case RED:
                break;
        }
    }

    private void onClickJoin(Player player, TeamColor color) {
        if (_status != Status.IDLE) {
            return;
        }
        CompetitionType type = delegate.mainGetCurrentCompetition(player);
        if (type != null && type != CompetitionType.DARUMA) {
            player.sendMessage(String.format("[だるまさんがころんだ] %sは既に%sにエントリー済みです", player.getName(), CompetitionTypeHelper.ToString(type)));
            return;
        }
        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) {
            player.sendMessage("[だるまさんがころんだ] ゲームモードはサバイバルかアドベンチャーの場合のみ参加可能です");
            return;
        }
        TeamColor current = getCurrentColor(player);
        if (current == null) {
            delegate.mainClearCompetitionItems(player);
            if (!giveItem(player)) {
                player.sendMessage(ChatColor.RED + "インベントリがいっぱいで競技用アイテムが渡せません");
                clearItem(player);
                return;
            }

            Team team = ensureTeam(color);
            team.add(player);
            broadcast("[だるまさんがころんだ] %sが%sにエントリーしました", player.getName(), ToColoredString(color)).log();
        } else {
            //NOTE: 本家では全チャになる
            player.sendMessage(String.format("[だるまさんがころんだ] %sは%sにエントリー済みです", player.getName(), ToColoredString(current)));
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
        manual = true;
        start();
    }

    private void start() {
        if (_status != Status.IDLE) {
            return;
        }
        int total = getPlayerCount();
        if (total < 1) {
            if (manual) {
                broadcastUnofficial("[だるまさんがころんだ] 参加者が見つかりません").log();
            } else {
                Calendar next = getNextAutoStart();
                next.add(Calendar.MINUTE, kAutoStartIntervalMinutes);
                broadcastUnofficial("[だるまさんがころんだ] 参加者が見つかりません。次回のスタートは %02d 時 %02d 分です", next.get(Calendar.HOUR_OF_DAY), next.get(Calendar.MINUTE));
            }
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
                broadcast("%s が競技に参加します（参加者%d人）", ToColoredString(color), count).log();
            }
        }
        broadcast("-----------------------");
        broadcast("");

        setStatus(Status.COUNTDOWN_START);
        delegate.mainCountdownThen(new BoundingBox[]{getAnnounceBounds()}, (count) -> _status == Status.COUNTDOWN_START, () -> {
            if (_status != Status.COUNTDOWN_START) {
                return false;
            }
            final Race race = new Race();
            for (Team team : teams.values()) {
                team.eachPlayer(race::participate);
            }
            this.race = race;
            setStatus(Status.START);
            return true;
        }, 20, Countdown.TitleSet.Default());
    }

    private static double Clamp(double v, double min, double max) {
        return Math.min(Math.max(v, min), max);
    }

    private void onClickGreen(Player player) {
        if (!player.isOp()) {
            return;
        }
        triggerGreen();
    }

    private void triggerGreen() {
        if (_status != Status.RED && _status != Status.START) {
            return;
        }
        setTitle("だるまさんが...", "Green light...");
        setStatus(Status.GREEN);
    }

    private void setTitle(@Nullable String title, @Nullable String subtitle) {
        World world = delegate.mainGetWorld();
        Players.Within(world, getAnnounceBounds(), player -> player.sendTitle(title == null ? "" : title, subtitle == null ? "" : subtitle, 10, 70, 20));
    }

    private void onClickTriggerRed(Player player) {
        if (!player.isOp()) {
            return;
        }
        triggerRed();
    }

    private void triggerRed() {
        if (_status != Status.GREEN) {
            return;
        }
        setStatus(Status.COUNTDOWN_RED);
        var titleSet = Countdown.TitleSet.Default();
        titleSet.zero = new Countdown.Title("ころんだ！！！", "Red light!!!");
        delegate.mainCountdownThen(new BoundingBox[]{getAnnounceBounds()}, (count) -> _status == Status.COUNTDOWN_RED, () -> {
            if (_status != Status.COUNTDOWN_RED) {
                return false;
            }
            positionWhenRed.clear();
            Players.Within(delegate.mainGetWorld(), getAnnounceBounds(), player -> {
                positionWhenRed.put(player.getUniqueId(), player.getLocation().toVector());
                player.playSound(player.getLocation(), Sound.ENTITY_GHAST_HURT, 0.25f, 1);
            });
            setStatus(Status.RED);
            if (!manual) {
                blockGreenSignal = true;
                delegate.mainRunTaskLater(() -> {
                    blockGreenSignal = false;
                }, 40);
            }
            return true;
        }, 15, titleSet);
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
        return switch (color) {
            case RED -> "TEAM RED";
            case WHITE -> "TEAM WHITE";
            case YELLOW -> "TEAM YELLOW";
        };
    }

    static String ToColoredString(TeamColor color) {
        return switch (color) {
            case RED -> ChatColor.RED + "TEAM RED" + ChatColor.RESET;
            case WHITE -> ChatColor.GRAY + "TEAM WHITE" + ChatColor.RESET;
            case YELLOW -> ChatColor.YELLOW + "TEAM YELLOW" + ChatColor.RESET;
        };
    }

    private ConsoleLogger broadcast(String format, Object... args) {
        String msg = String.format(format, args);
        Players.Within(delegate.mainGetWorld(), getAnnounceBounds(), player -> player.sendMessage(msg));
        return new ConsoleLogger(msg, "[だるまさんがころんだ]", delegate.mainGetLogger());
    }

    private ConsoleLogger broadcastUnofficial(String msg, Object... args) {
        return broadcast(msg, args);
    }

    @Override
    public ConsoleLogger announcerBroadcast(String format, Object... args) {
        return broadcast(format, args);
    }

    @Override
    public ConsoleLogger announcerBroadcastUnofficial(String format, Object... args) {
        return broadcastUnofficial(format, args);
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
        clearItem(player);
        if (race != null) {
            race.withdraw(player);
        }
        broadcast("[だるまさんがころんだ] %sがエントリー解除しました", player.getName()).log();
        if (getPlayerCount() < 1) {
            setStatus(Status.IDLE);
        }
    }

    private void clearItem(Player player) {
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

    private boolean giveItem(Player player) {
        ItemStack goldenApple = ItemBuilder.For(Material.GOLDEN_APPLE)
                .amount(1)
                .customByteTag(kItemTag, (byte) 1)
                .build();
        return player.getInventory().addItem(goldenApple).isEmpty();
    }

    private void giveParticipationReward(Player player) {
        ItemStack cookedBeef = ItemBuilder.For(Material.COOKED_BEEF)
                .amount(15)
                .build();
        player.getInventory().addItem(cookedBeef);
    }

    private void resetField() {
        World world = delegate.mainGetWorld();
        Editor.WallSign(world, offset(kButtonLeave), BlockFace.SOUTH, "エントリー解除");
        Editor.WallSign(world, offset(kButtonRedJoin), BlockFace.SOUTH, "赤組", "エントリー");
        Editor.WallSign(world, offset(kButtonWhiteJoin), BlockFace.SOUTH, "白組", "エントリー");
        Editor.WallSign(world, offset(kButtonYellowJoin), BlockFace.SOUTH, "黃組", "エントリー");
        setStartGateOpened(false);
        setEntranceOpened(true);
    }

    private void setStartGateOpened(boolean open) {
        stroke(new Point3i(104, -59, -123), new Point3i(144, -59, -123), open ? "air" : "iron_bars[east=true,west=true]");
    }

    private void setEntranceOpened(boolean open) {
        String block = open ? "air" : "iron_bars[east=true,west=true]";
        stroke(new Point3i(103, -58, -118), new Point3i(145, -58, -118), block);
        stroke(new Point3i(143, -59, -118), new Point3i(105, -59, -118), block);
        Editor.SetBlock(delegate.mainGetWorld(), offset(new Point3i(104, -59, -118)), open ? "air" : "iron_bars[east=true]");
        Editor.SetBlock(delegate.mainGetWorld(), offset(new Point3i(144, -59, -118)), open ? "air" : "iron_bars[west=true]");
    }

    private void stroke(Point3i from, Point3i to, String block) {
        Editor.Fill(delegate.mainGetWorld(), offset(from), offset(to), block);
    }

    private Point3i getEntryButtonPosition(TeamColor color) {
        return switch (color) {
            case RED -> offset(kButtonRedJoin);
            case WHITE -> offset(kButtonWhiteJoin);
            case YELLOW -> offset(kButtonYellowJoin);
        };
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

    @Override
    public boolean competitionIsJoined(Player player) {
        return getCurrentColor(player) != null;
    }

    @Override
    public void competitionClearItems(Player player) {
        clearItem(player);
    }

    @Override
    public @NotNull CompetitionType competitionGetType() {
        return CompetitionType.DARUMA;
    }

    @Override
    public void competitionReset() {
        setStatus(Status.IDLE);
        resetField();
        race = null;
        respawn.clear();
        teams.clear();
        blockGreenSignal = false;
        evacuateNonParticipants();
        Bukkit.getServer().broadcastMessage(CompetitionTypeHelper.ToString(competitionGetType()) + "をリセットしました");
    }

    private void evacuateNonParticipants() {
        Point3i safe = offset(kSafeSpawnLocation);
        Players.Within(delegate.mainGetWorld(), offset(kHabitableZone), player -> {
            GameMode mode = player.getGameMode();
            if (mode == GameMode.ADVENTURE || mode == GameMode.SURVIVAL) {
                player.teleport(player.getLocation().set(safe.x + 0.5, safe.y, safe.z + 0.5));
            }
        });
    }

    private static final Point3i kButtonLeave = new Point3i(105, -60, -121);
    private static final Point3i kButtonRedJoin = new Point3i(107, -60, -121);
    private static final Point3i kButtonWhiteJoin = new Point3i(109, -60, -121);
    private static final Point3i kButtonYellowJoin = new Point3i(111, -60, -121);
    // デバッグ用. 本家ではボタン押す形式だけど, ボタンだと誰でも押せてしまう. 誰でもスタートできるとマズいので看板右クリックの形式にする.
    private static final Point3i kButtonStartPreliminary = new Point3i(128, -53, -229);
    // デバッグ用. 本家ではボタン押す形式だけど, ボタンだと誰でも押せてしまう. 誰でもスタートできるとマズいので看板右クリックの形式にする.
    private static final Point3i kButtonStartFinal = new Point3i(126, -53, -229);
    // デバッグ用. 本家ではボタン押す形式だけど, ボタンだと誰でも押せてしまう. 誰でもスタートできるとマズいので看板右クリックの形式にする.
    private static final Point3i kButtonGreen = new Point3i(124, -53, -229);
    // デバッグ用. 本家ではボタン押す形式だけど, ボタンだと誰でも押せてしまう. 誰でもスタートできるとマズいので看板右クリックの形式にする.
    private static final Point3i kButtonTriggerRed = new Point3i(122, -53, -229);
    // 会場を再現するために置いてあるボタン. このボタンを押しても反応しない. おされたら警告メッセージを出す.
    private static final Point3i[] kDummyButtons = new Point3i[]{
            new Point3i(128, -52, -228),
            new Point3i(126, -52, -228),
            new Point3i(124, -52, -228),
            new Point3i(122, -52, -228),
    };
    private static final Point3i kButtonReset = new Point3i(105, -61, -122);

    private static final BoundingBox kAnnounceBounds = new BoundingBox(96, -62, -240, 152, -30, -106);
    private static final BoundingBox kGoalDetectionBox = new BoundingBox(104, -56, -228, 145, -53, -223);
    private static final BoundingBox kStartGridBounds = new BoundingBox(104, -60, -122.5, 145, -58, -120);
    // 競技中 "ころんだ" の時に動くと kill される領域
    private static final BoundingBox kKillZone = new BoundingBox(104, -62, -223, 145, -55, -123);
    // 競技中移動できる領域. この領域から逸脱すると, "ころんだ" の時じゃなくても kill される.
    private static final BoundingBox kHabitableZone = new BoundingBox(104, -62, -223, 145, -55, -117.5);
    // 開始時に場内に居る人を避難させる時の移動先
    private static final Point3i kSafeSpawnLocation = new Point3i(124, -60, -112);

    private static final String kItemTag = "hololive_sports_festival_2022_daruma";

    private static final int kAutoStartIntervalMinutes = 5;
    private static final int kTimerIntervalMillis = 1000;
}