package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Countdown;
import com.github.kbinani.holosportsfestival2022.Loader;
import com.github.kbinani.holosportsfestival2022.Point3i;
import com.github.kbinani.holosportsfestival2022.WallSign;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MobFightEventListener implements Listener, LevelDelegate {
    private final JavaPlugin owner;
    private boolean initialized = false;
    private final Map<TeamColor, Level> levels = new HashMap<>();
    private Map<TeamColor, Team> teams = new HashMap<>();
    private Status _status = Status.IDLE;
    private @Nullable Race race;

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
                clearItem("@a");
                for (Level level : levels.values()) {
                    level.reset();
                }
                //TODO: ここでステージ内にいるプレイヤーを入り口に戻すのが良さそう
                break;
            case AWAIT_COUNTDOWN:
                for (Level level : levels.values()) {
                    level.reset();
                }
                //TODO: ここでステージ内にいるプレイヤーを入り口に戻すのが良さそう
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
            BoundingBox box = level.getBounds();
            if (!box.contains(location)) {
                continue;
            }
            Progress current = level.getProgress();
            Stage stage = level.getStage(current.stage);
            if (stage == null) {
                continue;
            }
            Progress next = level.consumeDeadMob(entity);
            if (current.equals(next)) {
                continue;
            }
            Stage nextStage = level.getStage(next.stage);
            if (nextStage == null) {
                continue;
            }
            if (current.stage == next.stage) {
                // 同一 stage の次の step に
                broadcast("%s WAVE%d CLEAR !", ToColoredString(color), current.step + 1);
                Server server = owner.getServer();
                server.getScheduler().runTaskLater(owner, () -> {
                    stage.summonMobs(next.step);
                }, 20 * 3);
            } else {
                // 次の stage へ
                level.showTitle("WAVE CLEAR !", "yellow");
                Server server = owner.getServer();
                server.getScheduler().runTaskLater(owner, () -> {
                    stage.setExitOpened(true);
                    nextStage.summonMobs(0);
                    nextStage.setEntranceOpened(true);
                }, 20 * 3);
            }
        }
    }

    void showTitle(String selector, String text, String color) {
        execute("title %s title {\"text\": \"%s\", \"bold\": true, \"color\": \"%s\"}", selector, text, color);
    }

    String getTargetSelectorWithinAnnounceArea() {
        BoundingBox box = offset(kAnnounceBounds);
        return String.format("@a[x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f]", box.getMinX(), box.getMinY(), box.getMinZ(), box.getWidthX(), box.getHeight(), box.getWidthZ());
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
        if (_status != Status.RUN) {
            return;
        }
        Player player = e.getPlayer();
        Vector location = player.getLocation().toVector();
        for (Map.Entry<TeamColor, Level> it : levels.entrySet()) {
            TeamColor color = it.getKey();
            Level level = it.getValue();
            Progress progress = level.getProgress();
            if (progress.stage != level.getStageCount() - 1) {
                continue;
            }
            Team team = ensureTeam(color);
            if (team.getCurrentRole(player) == null) {
                continue;
            }

            FinalStage stage = level.finalStage;
            if (!stage.isCreeperSpawned() && stage.getCreeperSpawnBounds().contains(location)) {
                stage.summonCreepers();
            }
            if (!team.isPlayerFinished(player) && stage.getGoalDetectionBounds().contains(location)) {
                int finishedPlayerCount = team.setFinished(player);
                if (finishedPlayerCount == team.getPlayerCount()) {
                    broadcast("%s GAME CLEAR !!", ToColoredString(color));
                    level.showTitle("GAME CLEAR !!", "gold");
                    stage.reset();
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
                        broadcast("[結果発表]");
                        for (int i = 0; i < race.order.size(); i++) {
                            TeamColor tc = race.order.get(i);
                            broadcast("%d位 : %s", i + 1, ToColoredString(tc));
                        }
                        broadcast("-----------------------");
                        broadcast("");
                        for (TeamColor tc : race.getTeamColors()) {
                            Team t = ensureTeam(tc);
                            t.reset();
                        }
                        race = null;
                        setStatus(Status.IDLE);
                        return;
                    }
                }
            }
        }
    }

    void onClickJoin(Player player, TeamColor color, Role role) {
        if (_status != Status.IDLE && _status != Status.AWAIT_COUNTDOWN) {
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
        execute("give %s iron_leggings{tag:{%s:1b},Enchantments:[{id:protection,lvl:4},{id:unbreaking,lvl:3}]}", player.getName(), kItemTag);
        execute("give %s iron_chestplate{tag:{%s:1b},Enchantments:[{id:protection,lvl:4},{id:unbreaking,lvl:3}]}", player.getName(), kItemTag);
        execute("give %s iron_helmet{tag:{%s:1b},Enchantments:[{id:protection,lvl:4},{id:respiration,lvl:3},{id:unbreaking,lvl:3}]}", player.getName(), kItemTag);
        execute("give %s iron_boots{tag:{%s:1b},Enchantments:[{id:depth_strider,lvl:3},{id:protection,lvl:4},{id:unbreaking,lvl:3}]}", player.getName(), kItemTag);
        execute("give %s golden_apple{tag:{%s:1b}} 35", player.getName(), kItemTag);
        execute("give %s cooked_beef{tag:{%s:1b}} 35", player.getName(), kItemTag);
        switch (role) {
            case ARROW:
                execute("give %s bow{tag:{%s:1b},Enchantments:[{id:infinity,lvl:1},{id:power,lvl:5},{id:unbreaking,lvl:3}]}", player.getName(), kItemTag);
                execute("give %s arrow{tag:{%s:1b}}", player.getName(), kItemTag);
                break;
            case SWORD:
                execute("give %s shield{tag:{%s:1b},Enchantments:[{id:unbreaking,lvl:3}]}", player.getName(), kItemTag);
                execute("give %s iron_sword{tag:{%s:1b},Enchantments:[{id:knockback,lvl:1},{id:smite,lvl:5},{id:unbreaking,lvl:3}]}", player.getName(), kItemTag);
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

        if (getPlayerCount() > 0) {
            clearItem(String.format("@p[name=\"%s\"]", player.getName()));
            setStatus(Status.AWAIT_COUNTDOWN);
        } else {
            setStatus(Status.IDLE);
        }
        broadcast("[MOB討伐レース] %sがエントリー解除しました", player.getName());
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
                broadcast("%s が競技に参加します（参加者%d人）", ToColoredString(color), count);
                race.add(color);
            } else {
                broadcast("%sの参加者が見つかりません", ToString(color));
            }
        }
        broadcast("-----------------------");
        broadcast("");
        broadcast("[MOB討伐レース] 競技を開始します！");
        broadcast("");
        setStatus(Status.COUNTDOWN);
        Countdown.Then(offset(kAnnounceBounds), owner, (count) -> _status == Status.COUNTDOWN, () -> {
            if (_status != Status.COUNTDOWN) {
                return false;
            }
            this.race = race;
            for (TeamColor color : race.getTeamColors()) {
                Level level = ensureLevel(color);
                level.reset();
                level.setExitOpened(false);
                Stage stage = level.getStage(0);
                stage.summonMobs(0);
                stage.setEntranceOpened(true);
            }
            setStatus(Status.RUN);
            return true;
        });
    }

    int getPlayerCount() {
        int count = 0;
        for (Map.Entry<TeamColor, Team> it : teams.entrySet()) {
            count += it.getValue().getPlayerCount();
        }
        return count;
    }

    void clearItem(String selector) {
        for (String item : new String[]{"iron_leggings", "iron_chestplate", "iron_helmet", "iron_boots", "golden_apple", "cooked_beef", "bow", "arrow", "shield", "iron_sword"}) {
            execute("clear %s %s{tag:{%s:1b}}", selector, item, kItemTag);
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
        overworld().ifPresent(world -> {
            Loader.LoadChunk(world, offset(kAnnounceBounds));
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

    private static final String kItemTag = "hololive_sports_festival_2022_mob";

    private static final BoundingBox kAnnounceBounds = new BoundingBox(-26, -61, -424, 79, -19, -244);

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
}