package com.github.kbinani.holosportsfestival2022;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BoatRaceEventListener implements Listener {
    private final JavaPlugin owner;

    enum Team {
        RED,
        WHITE,
        YELLOW,
    }

    enum Role {
        DRIVER,
        SHOOTER,
    }

    enum Status {
        IDLE,
        COUNTDOWN,
        RUN,
    }

    private Status _status = Status.IDLE;

    private void setStatus(Status status) {
        if (status == _status) {
            return;
        }
        _status = status;
        switch (status) {
            case IDLE:
                resetField();
                clearItems("@a");
                break;
            case RUN:
                // ゴールラインの柵を撤去
                execute("fill %s %s air", xyz(-52, -58, -196), xyz(-25, -58, -196));

                // スタートラインの柵を撤去
                execute("fill %s %s air", xyz(-26, -58, -186), xyz(-36, -58, -186));
                execute("fill %s %s air", xyz(-37, -58, -187), xyz(-44, -58, -187));
                execute("fill %s %s air", xyz(-45, -58, -188), xyz(-52, -58, -188));

                // 妨害装置を起動
                //TODO: 同時起動だと妨害装置が同期するので適当にずらす
                for (Point3i p : kJammingBlockStarterBlocks) {
                    setLeverPowered(offset(p), true);
                }
                break;
        }
    }

    private void resetField() {
        // 操作用の看板を設置
        WallSign.Place(offset(kYellowEntryShooter), BlockFace.WEST, "黄組", "エントリー", ToString(Role.SHOOTER));
        WallSign.Place(offset(kYellowEntryDriver), BlockFace.WEST, "黄組", "エントリー", ToString(Role.DRIVER));
        WallSign.Place(offset(kWhiteEntryShooter), BlockFace.WEST, "白組", "エントリー", ToString(Role.SHOOTER));
        WallSign.Place(offset(kWhiteEntryDriver), BlockFace.WEST, "白組", "エントリー", ToString(Role.DRIVER));
        WallSign.Place(offset(kRedEntryShooter), BlockFace.WEST, "赤組", "エントリー", ToString(Role.SHOOTER));
        WallSign.Place(offset(kRedEntryDriver), BlockFace.WEST, "赤組", "エントリー", ToString(Role.DRIVER));
        WallSign.Place(offset(kLeaveButton), BlockFace.WEST, "エントリー解除");

        // ゴールラインに柵を設置
        execute("fill %s %s bedrock", xyz(-52, -58, -196), xyz(-25, -58, -196));

        // スタートラインに柵を設置
        execute("fill %s %s bedrock", xyz(-26, -58, -186), xyz(-36, -58, -186));
        execute("fill %s %s bedrock", xyz(-37, -58, -187), xyz(-44, -58, -187));
        execute("fill %s %s bedrock", xyz(-45, -58, -188), xyz(-52, -58, -188));

        // 妨害装置を停止
        for (Point3i p : kJammingBlockStarterBlocks) {
            setLeverPowered(offset(p), false);
        }
    }

    private void setLeverPowered(Point3i pos, boolean powered) {
        World world = overworld().orElse(null);
        if (world == null) {
            return;
        }
        Block block = world.getBlockAt(pos.x, pos.y, pos.z);
        BlockData data = block.getBlockData();
        if (data.getMaterial() != Material.LEVER) {
            System.out.println("is not lever:" + data.getMaterial());
            return;
        }
        if (!(data instanceof Powerable)) {
            System.out.println("is not powerable" + data);
            return;
        }
        Powerable lever = (Powerable) data;
        lever.setPowered(powered);
        world.setBlockData(pos.x, pos.y, pos.z, data);
    }

    private Optional<World> overworld() {
        return owner.getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
    }

    static String ToString(Role role) {
        if (role == Role.DRIVER) {
            return "（操縦担当）";
        } else {
            return "（妨害担当）";
        }
    }

    static String ToColoredString(Team team) {
        if (team == Team.RED) {
            return ChatColor.RED + "TEAM RED" + ChatColor.RESET;
        } else if (team == Team.YELLOW) {
            return ChatColor.YELLOW + "TEAM YELLOW" + ChatColor.RESET;
        } else {
            return ChatColor.GRAY + "TEAM WHITE" + ChatColor.RESET;
        }
    }

    class Participation {
        final Team team;
        final Role role;

        Participation(Team team, Role role) {
            this.team = team;
            this.role = role;
        }
    }

    class Participant {
        private @Nullable Player shooter;
        private @Nullable Player driver;

        void setPlayer(Role role, @Nullable Player player) {
            if (role == Role.DRIVER) {
                this.driver = player;
            } else {
                this.shooter = player;
            }
        }

        @Nullable
        Player getPlayer(Role role) {
            if (role == Role.DRIVER) {
                return driver;
            } else {
                return shooter;
            }
        }

        @Nullable
        Role getCurrentRole(@Nonnull Player player) {
            if (driver != null && driver.getUniqueId().equals(player.getUniqueId())) {
                return Role.DRIVER;
            }
            if (shooter != null && shooter.getUniqueId().equals(player.getUniqueId())) {
                return Role.SHOOTER;
            }
            return null;
        }

        int getPlayerCount() {
            // 一人でもメンバーが居れば準備済み扱いにする
            int result = 0;
            if (driver != null && driver.isOnline()) {
                result++;
            }
            if (shooter != null && shooter.isOnline()) {
                result++;
            }
            return result;
        }
    }

    private final Map<Team, Participant> teams = new HashMap<>();

    private @Nonnull Participant ensureTeam(Team team) {
        Participant t = teams.get(team);
        if (t == null) {
            t = new Participant();
            teams.put(team, t);
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
    private static final Point3i kFieldNorthWest = new Point3i(-106, -60, -294);
    private static final Point3i kFieldSouthEast = new Point3i(-24, -30, -127);

    BoatRaceEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {

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
        if (location.equals(offset(kYellowEntryShooter))) {
            onClickJoin(player, Team.YELLOW, Role.SHOOTER);
        } else if (location.equals(offset(kYellowEntryDriver))) {
            onClickJoin(player, Team.YELLOW, Role.DRIVER);
        } else if (location.equals(offset(kWhiteEntryShooter))) {
            onClickJoin(player, Team.WHITE, Role.SHOOTER);
        } else if (location.equals(offset(kWhiteEntryDriver))) {
            onClickJoin(player, Team.WHITE, Role.DRIVER);
        } else if (location.equals(offset(kRedEntryShooter))) {
            onClickJoin(player, Team.RED, Role.SHOOTER);
        } else if (location.equals(offset(kRedEntryDriver))) {
            onClickJoin(player, Team.RED, Role.DRIVER);
        } else if (location.equals(offset(kLeaveButton))) {
            onClickLeave(player);
        }
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
    public void onEntitySpawn(EntitySpawnEvent e) {
        if (e.getEntityType() != EntityType.DROPPED_ITEM) {
            return;
        }
        Item item = (Item) e.getEntity();
        Point3i pos = new Point3i(item.getLocation());
        ItemStack itemStack = item.getItemStack();
        Material material = itemStack.getType();
        Server server = owner.getServer();
        UUID id = e.getEntity().getUniqueId();
        if (material != Boat(Team.RED) && material == Boat(Team.WHITE) && material == Boat(Team.YELLOW)) {
            return;
        }
        BoundingBox bounds = getBounds();
        if (!bounds.contains(pos.x, pos.y, pos.z)) {
            return;
        }

        // onEntitySpawn と同一 tick で data コマンド実行しても反映されないので次の tick で実行する.
        BukkitScheduler scheduler = owner.getServer().getScheduler();
        scheduler.runTask(owner, () -> {
            execute("data merge entity %s {Item:{tag:{%s:1b}}}", id, kItemTag);
        });
    }

    private BoundingBox getBounds() {
        return new BoundingBox(x(kFieldNorthWest.x), y(kFieldNorthWest.y), z(kFieldNorthWest.z), x(kFieldSouthEast.x), y(kFieldSouthEast.y), z(kFieldSouthEast.z));
    }

    private void broadcast(String msg) {
        owner.getServer().broadcastMessage(msg);
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String msg) {
        broadcast(msg);
    }

    private static Material Boat(Team team) {
        if (team == Team.WHITE) {
            return Material.BIRCH_BOAT;
        } else if (team == Team.YELLOW) {
            return Material.JUNGLE_BOAT;
        } else {
            return Material.MANGROVE_BOAT;
        }
    }

    private void onClickJoin(Player player, Team team, Role role) {
        @Nullable Participation current = getCurrentParticipation(player);
        if (current == null) {
            Participant p = ensureTeam(team);
            p.setPlayer(role, player);
            if (role == Role.DRIVER) {
                execute("give @p[name=\"%s\"] %s{tag:{%s:1b}}", player.getName(), Boat(team).name().toLowerCase(), kItemTag);
            } else {
                execute("give @p[name=\"%s\"] snowball{tag:{%s:1b},display:{Name:'[{\"text\":\"[水上レース専用] 暗闇（弱）\"}]'}}", player.getName(), kItemTag);
                execute("give @p[name=\"%s\"] crossbow{tag:{%s:1b}}", player.getName(), kItemTag);
                execute("give @p[name=\"%s\"] splash_potion{Potion:darkness,tag:{%s:1b},display:{Name:'[{\"text\":\"[水上レース専用] 暗闇（強）\"}]'}}", player.getName(), kItemTag);
            }
            broadcast(String.format("[水上レース] %sが%s%sにエントリーしました", player.getName(), ToColoredString(team), ToString(role)));
        } else {
            broadcastUnofficial(ChatColor.RED + String.format("[水上レース] %sは%s%sで既にエントリー済みです", ChatColor.RESET + player.getName(), ToColoredString(team), ChatColor.RED + ToString(role)));
        }
    }

    private void onClickLeave(Player player) {
        @Nullable Participation current = getCurrentParticipation(player);
        if (current == null) {
            return;
        }
        Participant team = ensureTeam(current.team);
        team.setPlayer(current.role, null);
        clearItems(String.format("@p[name=\"%s\"]", player.getName()));
        broadcastUnofficial(String.format("[水上レース] %sが%s%sのエントリー解除しました", player.getName(), ToColoredString(current.team), ToString(current.role)));
        if (_status == Status.RUN) {
            setStatus(Status.IDLE);
        }
    }

    private void clearItems(String selector) {
        for (String item : new String[]{"snowball", "crossbow", "splash_potion", Boat(Team.RED).name().toLowerCase(), Boat(Team.YELLOW).name().toLowerCase(), Boat(Team.WHITE).name().toLowerCase()}) {
            execute("clear %s %s{tag:{%s:1b}}", selector, item, kItemTag);
        }
    }

    private @Nullable Participation getCurrentParticipation(@Nonnull Player player) {
        for (Team team : teams.keySet()) {
            Participant participant = teams.get(team);
            if (participant == null) {
                continue;
            }
            @Nullable Role role = participant.getCurrentRole(player);
            if (role != null) {
                return new Participation(team, role);
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
        boolean ready = true;
        for (Team team : new Team[]{Team.RED, Team.WHITE, Team.YELLOW}) {
            Participant participant = ensureTeam(team);
            totalPlayerCount += participant.getPlayerCount();
        }
        if (totalPlayerCount < 1) {
            broadcastUnofficial(ChatColor.RED + "[水上レース] 参加者が 0 人です");
            return;
        }
        broadcast("");
        broadcast("-----------------------");
        for (Team team : new Team[]{Team.RED, Team.WHITE, Team.YELLOW}) {
            Participant participant = ensureTeam(team);
            int count = participant.getPlayerCount();
            if (count < 1) {
                continue;
            }
            broadcast(String.format("%s が競技に参加します（参加者%d人）", ToColoredString(team), count));
        }
        broadcast("-----------------------");
        broadcast("");
        broadcast("[水上レース] 競技を開始します！");
        broadcast("");
        setStatus(Status.COUNTDOWN);

        Countdown.Then(getBounds(), owner, (count) -> {
            return _status == Status.COUNTDOWN;
        }, () -> {
            if (_status != Status.COUNTDOWN) {
                return false;
            }
            setStatus(Status.RUN);
            return true;
        });
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
}