package com.github.kbinani.holosportsfestival2022.fencing;

import com.github.kbinani.holosportsfestival2022.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class FencingEventListener implements Listener, Competition {
    private final JavaPlugin owner;
    private @Nullable Player left;
    private @Nullable Player right;
    private int hitpointLeft = 3;
    private int hitpointRight = 3;
    private Bossbar bossbarLeft;
    private Bossbar bossbarRight;
    private boolean initialized = false;
    private final long loadDelay;
    private final MainDelegate delegate;

    static final String kBossbarLeft = "sports_festival_2022_bossbar_left";
    static final String kBossbarRight = "sports_festival_2022_bossbar_right";
    static final String kWeaponCustomTag = "hololive_sports_festival_2022_fencing";
    static final int kWeaponKnockbackLevel = 10;

    public FencingEventListener(JavaPlugin owner, MainDelegate delegate, long loadDelay) {
        this.owner = owner;
        this.loadDelay = loadDelay;
        this.delegate = delegate;
    }

    enum Status {
        IDLE,
        AWAIT_COUNTDOWN,
        COUNTDOWN,
        RUN,
        AWAIT_DEATH,
    }

    enum Team {
        LEFT,
        RIGHT,
    }

    private FencingEventListener.Status _status = FencingEventListener.Status.IDLE;

    private void setStatus(Status status) {
        if (status == _status) {
            return;
        }
        _status = status;
        switch (status) {
            case IDLE:
                clearField();
                left = null;
                right = null;
                hitpointRight = 3;
                hitpointLeft = 3;
                break;
            case AWAIT_COUNTDOWN:
                // 範囲内に居るプレイヤーを観客席側に排除する
                Server server = owner.getServer();
                BoundingBox box = offset(kFieldBounds);
                server.getOnlinePlayers().forEach(player -> {
                    Location loc = player.getLocation();
                    if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                        return;
                    }
                    if (box.contains(loc.toVector())) {
                        loc.setZ(z(-273));
                        loc.setY(y(-19));
                        player.teleport(loc);
                    }
                });

                // 左ゲート構築
                fill(new Point3i(165, -16, -268), new Point3i(165, -18, -264), "white_concrete");
                fill(new Point3i(165, -17, -267), new Point3i(165, -18, -265), "glass");
                setBlock(new Point3i(164, -17, -266), "iron_door[facing=west,half=upper,hinge=left]");
                setBlock(new Point3i(164, -18, -266), "iron_door[facing=west,half=lower,hinge=left]");
                setBlock(new Point3i(165, -17, -266), "air");
                setBlock(new Point3i(165, -18, -266), "heavy_weighted_pressure_plate");

                // 右ゲート構築
                fill(new Point3i(103, -16, -264), new Point3i(103, -18, -268), "white_concrete");
                fill(new Point3i(103, -17, -265), new Point3i(103, -18, -267), "glass");
                setBlock(new Point3i(104, -17, -266), "iron_door[facing=east,half=upper,hinge=left]");
                setBlock(new Point3i(104, -18, -266), "iron_door[facing=east,half=lower,hinge=left]");
                setBlock(new Point3i(103, -17, -266), "air");
                setBlock(new Point3i(103, -18, -266), "heavy_weighted_pressure_plate");

                // バリアブロックの柵
                fill(new Point3i(165, -17, -269), new Point3i(103, -17, -269), "barrier");
                fill(new Point3i(104, -17, -263), new Point3i(165, -17, -263), "barrier");

                break;
            case COUNTDOWN:
                hitpointRight = 3;
                hitpointLeft = 3;

                // bossbar 追加
                bossbarLeft.setValue(3);
                bossbarRight.setValue(3);
                bossbarLeft.setVisible(true);
                bossbarRight.setVisible(true);

                broadcast("");
                broadcast("[フェンシング] 競技を開始します！");
                broadcast("");
                break;
            case RUN:
                break;
            case AWAIT_DEATH:
                clearField();
                break;
        }
    }

    private void fill(Point3i from, Point3i to, String block) {
        Editor.Fill(offset(from), offset(to), block);
    }

    private void setBlock(Point3i p, String block) {
        Editor.SetBlock(offset(p), block);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (_status != Status.RUN && _status != Status.AWAIT_DEATH) {
            return;
        }
        if (right == null || left == null) {
            return;
        }
        Entity entity = e.getEntity();
        Entity damager = e.getDamager();
        if (entity.getType() != EntityType.PLAYER || damager.getType() != EntityType.PLAYER) {
            return;
        }
        Player defence = (Player) entity;
        Player offence = (Player) damager;
        String offenceName = offence.getName();

        Team defenceTeam = getCurrentTeam(defence);
        Team offenceTeam = getCurrentTeam(offence);
        if (defenceTeam == null || offenceTeam == null) {
            return;
        }
        if (defenceTeam == offenceTeam) {
            // ここには来ないはず
            return;
        }

        Server server = owner.getServer();
        World world = entity.getWorld();

        BoundingBox box = offset(kFieldBounds);
        //NOTE: 攻撃側がフィールド内に居るかどうかだけ判定する.
        if (!box.contains(offence.getLocation().toVector())) {
            return;
        }
        EntityEquipment equipment = offence.getEquipment();
        if (equipment == null) {
            return;
        }
        ItemStack mainHand = equipment.getItemInMainHand();
        if (mainHand.getType() != Material.IRON_SWORD) {
            e.setCancelled(true);
            return;
        }
        int knockback = mainHand.getEnchantments().get(Enchantment.KNOCKBACK);
        if (knockback != kWeaponKnockbackLevel) {
            e.setCancelled(true);
            return;
        }

        boolean shouldKill = false;
        if (defenceTeam == Team.RIGHT) {
            hitpointRight = Math.max(0, hitpointRight - 1);
            shouldKill = hitpointRight == 0;
        } else {
            hitpointLeft = Math.max(0, hitpointLeft - 1);
            shouldKill = hitpointLeft == 0;
        }

        bossbarLeft.setValue(hitpointLeft);
        bossbarRight.setValue(hitpointRight);

        if (shouldKill) {
            BukkitScheduler scheduler = owner.getServer().getScheduler();
            scheduler.runTaskLater(owner, this::decideResult, 1);
        }
    }

    private void decideResult() {
        if (_status != Status.RUN) {
            return;
        }

        // 同一 tick 内で攻撃しあって相打ちになった場合を考慮して, 1 tick 後に勝敗の判定をする.
        setStatus(Status.AWAIT_DEATH);

        if (left == null || right == null) {
            // 不在なのでノーコンテストに戻す
            setStatus(Status.IDLE);
            return;
        }

        //NOTE: onEntityDamageByEntity と同一 tick 内で velocity を変更しても効果がないので 1 tick 後に変更する.
        if (hitpointLeft == 0) {
            float yaw = right.getLocation().getYaw();
            Vector velocity = new Vector(yaw > 0 ? -100 : 100, 10, 0);
            left.setVelocity(velocity);
        }
        if (hitpointRight == 0) {
            float yaw = left.getLocation().getYaw();
            Vector velocity = new Vector(yaw > 0 ? -100 : 100, 10, 0);
            right.setVelocity(velocity);
        }

        BukkitScheduler scheduler = owner.getServer().getScheduler();
        scheduler.runTaskLater(owner, this::killLosers, 30);
    }

    private void killLosers() {
        if (_status != Status.AWAIT_DEATH) {
            return;
        }
        //NOTE: 敗北者を kill する前にアイテムを回収
        execute("clear @a iron_sword{tag:{%s:1b}}", kWeaponCustomTag);

        if (left == null || right == null) {
            // 不在なのでノーコンテストに戻す
            setStatus(Status.IDLE);
            return;
        }

        // 敗北者を kill する
        if (hitpointLeft == 0) {
            Location loc = left.getLocation();
            // https://symtm.blog.fc2.com/blog-entry-96.html
            execute("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Flicker:0b,Trail:0b,Colors:[I;14188952],FadeColors:[I;14188952]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ());
            execute("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:0,Flicker:1b,Trail:0b,Colors:[I;15790320],FadeColors:[I;15790320]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ());
            execute("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:4,Flicker:0b,Trail:0b,Colors:[I;14602026],FadeColors:[I;14602026]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ());
            left.setHealth(0);
        }
        if (hitpointRight == 0) {
            Location loc = right.getLocation();
            // https://symtm.blog.fc2.com/blog-entry-96.html
            execute("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Flicker:0b,Trail:0b,Colors:[I;14188952],FadeColors:[I;14188952]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ());
            execute("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:0,Flicker:1b,Trail:0b,Colors:[I;15790320],FadeColors:[I;15790320]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ());
            execute("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:4,Flicker:0b,Trail:0b,Colors:[I;14602026],FadeColors:[I;14602026]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ());
            right.setHealth(0);
        }

        String message;
        if (hitpointLeft == 0 && hitpointRight == 0) {
            message = "相打ち！両者引き分け！！";
        } else if (hitpointLeft == 0) {
            message = right.getName() + "が勝利！";
        } else {
            message = left.getName() + "が勝利！";
        }

        // 結果を通知する
        broadcast("");
        broadcast("-----------------------");
        broadcast("[試合終了]");
        broadcast(message);
        broadcast("-----------------------");
        broadcast("");
    }

    private void clearField() {
        fill(new Point3i(102, -16, -269), new Point3i(165, -18, -264), "air");
        bossbarLeft.setVisible(false);
        bossbarRight.setVisible(false);
        execute("clear @a iron_sword{tag:{%s:1b}}", kWeaponCustomTag);
        Editor.WallSign(offset(new Point3i(101, -19, -265)), BlockFace.NORTH, "右側エントリー");
        Editor.WallSign(offset(new Point3i(99, -19, -265)), BlockFace.NORTH, "エントリー解除");
        Editor.WallSign(offset(new Point3i(167, -19, -265)), BlockFace.NORTH, "左側エントリー");
        Editor.WallSign(offset(new Point3i(169, -19, -265)), BlockFace.NORTH, "エントリー解除");
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onBlockRedstoneEvent(BlockRedstoneEvent e) {
        if (e.getOldCurrent() != 0 || e.getNewCurrent() <= 0) {
            return;
        }

        Point3i location = new Point3i(e.getBlock().getLocation());
        if (location.equals(offset(kButtonStart))) {
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
        if (location.equals(offset(kButtonRightJoin))) {
            onClickJoin(player, Team.RIGHT);
        } else if (location.equals(offset(kButtonLeftJoin))) {
            onClickJoin(player, Team.LEFT);
        } else if (location.equals(offset(kButtonLeftLeave))) {
            onClickLeave(player);
        } else if (location.equals(offset(kButtonRightLeave))) {
            onClickLeave(player);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        Team team = getCurrentTeam(player);
        if (team != null) {
            clearPlayer(team);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (initialized) {
            return;
        }
        initialized = true;

        owner.getServer().getScheduler().runTaskLater(owner, () -> {
            BoundingBox bounds = getAnnounceBounds();
            bossbarLeft = new Bossbar(owner, kBossbarLeft, "<<< " + TeamName(Team.LEFT) + " <<<", bounds);
            bossbarLeft.setMax(3);
            bossbarLeft.setValue(3);
            bossbarLeft.setColor("green");

            bossbarRight = new Bossbar(owner, kBossbarRight, ">>> " + TeamName(Team.RIGHT) + " >>>", bounds);
            bossbarRight.setMax(3);
            bossbarRight.setValue(3);
            bossbarRight.setColor("green");

            clearField();
        }, loadDelay);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (_status != Status.RUN) {
            return;
        }
        Player player = e.getEntity();
        Team team = getCurrentTeam(player);
        if (team != null) {
            e.setKeepInventory(true);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        Team team = getCurrentTeam(player);
        if (team == null) {
            return;
        }
        Point3i respawn = offset(kRespawnLocation);
        Location location = player.getLocation();
        location.setX(respawn.x);
        location.setY(respawn.y);
        location.setZ(respawn.z);
        e.setRespawnLocation(location);

        // ここで setStatus(Status.IDLE) すると相討ちの場合に後に onPlayerRespawn する側のリスポン位置が設定されない.
        // 1 tick 遅れて IDLE に戻す.

        Server server = owner.getServer();
        BukkitScheduler scheduler = server.getScheduler();
        scheduler.runTask(owner, () -> {
            setStatus(Status.IDLE);
        });
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

    private void joinPlayer(@Nonnull Player player, Team team) {
        if (getCurrentTeam(player) != null) {
            broadcast("[フェンシング] %sはエントリー済みです", player.getName());
            return;
        }
        if (team == Team.RIGHT) {
            right = player;
        } else if (team == Team.LEFT) {
            left = player;
        }
        execute("give @p[name=\"%s\"] %s", player.getName(), Weapon());
        broadcast("[フェンシング] %sがエントリーしました（%s）", player.getName(), TeamName(team));
        if (right == null && left == null) {
            setStatus(Status.IDLE);
        } else {
            setStatus(Status.AWAIT_COUNTDOWN);
        }
    }

    private String Weapon() {
        UUID attributeUid = UUID.randomUUID();
        return String.format("iron_sword{tag:{%s:1b},HideFlags:2,Enchantments:[{id:knockback,lvl:10}],AttributeModifiers:[{AttributeName:\"generic.attack_damage\",Amount:0,Operation:0,UUID:%s}]}", kWeaponCustomTag, UUIDString(attributeUid));
    }

    // [I;-519191173,-134519044,310236205,-136580550]
    static String UUIDString(UUID uid) {
        byte[] bytes = new byte[16];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(uid.getMostSignificantBits());
        bb.putLong(uid.getLeastSignificantBits());
        bb.flip();
        return "[I;" + bb.getInt(0) + "," + bb.getInt(1) + "," + bb.getInt(2) + "," + bb.getInt(3) + "]";
    }

    private void clearPlayer(Team team) {
        if (team == Team.RIGHT) {
            right = null;
        } else if (team == Team.LEFT) {
            left = null;
        }
        if (right == null && left == null) {
            setStatus(Status.IDLE);
        } else {
            setStatus(Status.AWAIT_COUNTDOWN);
        }
    }

    private void broadcast(String format, Object... args) {
        String msg = String.format(format, args);
        execute("tellraw @a[%s] \"%s\"", TargetSelector.Of(getAnnounceBounds()), msg);
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String format, Object... args) {
        broadcast(format, args);
    }

    private void onClickJoin(@Nonnull Player player, Team team) {
        if (_status != Status.IDLE && _status != Status.AWAIT_COUNTDOWN) {
            return;
        }
        CompetitionType type = delegate.getCurrentCompetition(player);
        if (type != null && type != CompetitionType.FENCING) {
            broadcastUnofficial("[フェンシング] %sは既に%sにエントリー済みです", player.getName(), CompetitionTypeHelper.ToString(type));
            return;
        }
        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) {
            player.sendMessage("[フェンシング] ゲームモードはサバイバルかアドベンチャーの場合のみ参加可能です");
            return;
        }
        if (getCurrentTeam(player) != null) {
            //TODO: 既に join 済みの時のメッセージ
            return;
        }
        joinPlayer(player, team);
    }

    @Nullable
    Team getCurrentTeam(Player player) {
        if (left != null && left.getUniqueId().equals(player.getUniqueId())) {
            return Team.LEFT;
        }
        if (right != null && right.getUniqueId().equals(player.getUniqueId())) {
            return Team.RIGHT;
        }
        return null;
    }

    private static String TeamName(Team team) {
        switch (team) {
            case LEFT:
                return "LEFT SIDE";
            case RIGHT:
                return "RIGHT SIDE";
            default:
                return "";
        }
    }

    private void onClickLeave(@Nonnull Player player) {
        // status をリセットするため _status == Status.IDLE チェックは入れずに強制的にエントリー解除処理する
        Team color = getCurrentTeam(player);
        if (color == null) {
            return;
        }
        clearPlayer(color);
        execute("clear @p[name=\"%s\"] iron_sword{tag:{%s:1b}}", player.getName(), kWeaponCustomTag);
        broadcast("[フェンシング] %sがエントリー解除しました", player.getName());
    }

    private void onClickStart() {
        if (_status != Status.IDLE && _status != Status.AWAIT_COUNTDOWN) {
            return;
        }
        Player left = null;
        Player right = null;
        int numLeft = 0;
        int numRight = 0;
        if (this.left != null) {
            left = this.left;
            numLeft += 1;
        }
        if (this.right != null) {
            right = this.right;
            numRight += 1;
        }
        if (numLeft != numRight || numLeft < 1 || numRight < 1) {
            broadcast("参加人数が正しくありません（%s : %d人、%s : %d人）", TeamName(Team.LEFT), numLeft, TeamName(Team.RIGHT), numRight);
            return;
        }
        regenerate(left);
        regenerate(right);

        setStatus(Status.COUNTDOWN);
        Countdown.Then(getAnnounceBounds(), owner, (count) -> _status == Status.COUNTDOWN, () -> {
            if (_status != Status.COUNTDOWN) {
                return false;
            }
            setStatus(Status.RUN);
            return true;
        });
    }

    private void regenerate(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            player.setHealth(maxHealth.getValue());
        }
    }

    private BoundingBox getAnnounceBounds() {
        return offset(kAnnounceBounds);
    }

    private Point3i offset(Point3i p) {
        // 座標が間違っていたらここでオフセットする
        return new Point3i(p.x, p.y, p.z);
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

    private BoundingBox offset(BoundingBox box) {
        return new BoundingBox(x(box.getMinX()), y(box.getMinY()), z(box.getMinZ()), x(box.getMaxX()), y(box.getMaxY()), z(box.getMaxZ()));
    }

    private void execute(String format, Object... args) {
        Server server = owner.getServer();
        CommandSender sender = server.getConsoleSender();
        server.dispatchCommand(sender, String.format(format, args));
    }

    @Override
    public boolean isJoined(Player player) {
        return getCurrentTeam(player) != null;
    }

    private static final Point3i kButtonRightJoin = new Point3i(101, -19, -265);
    private static final Point3i kButtonLeftJoin = new Point3i(167, -19, -265);
    private static final Point3i kButtonLeftLeave = new Point3i(169, -19, -265);
    private static final Point3i kButtonRightLeave = new Point3i(99, -19, -265);
    private static final Point3i kButtonStart = new Point3i(134, -18, -272);
    private static final BoundingBox kAnnounceBounds = new BoundingBox(85, -20, -280, 171, 384, -253);
    private static final BoundingBox kFieldBounds = new BoundingBox(104, -18, -268, 104 + 61, -18 + 5, -268 + 4);
    private static final Point3i kRespawnLocation = new Point3i(96, -19, -274);
}