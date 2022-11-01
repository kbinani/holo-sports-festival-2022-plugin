package com.github.kbinani.holosportsfestival2022;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FencingEventListener implements Listener {
    private final JavaPlugin owner;
    private @Nullable UUID playerLeft;
    private @Nullable UUID playerRight;
    private int hitpointLeft = 3;
    private int hitpointRight = 3;
    private @Nullable Boolean showDeathMessage = null;
    private Bossbar bossbarLeft;
    private Bossbar bossbarRight;

    static final String kBossbarLeft = "sports_festival_2022_bossbar_left";
    static final String kBossbarRight = "sports_festival_2022_bossbar_right";
    static final String kWeaponCustomTag = "hololive_sports_festival_2022_fencing";
    static final int kFieldX = 104;
    static final int kFieldY = -18;
    static final int kFieldZ = -268;
    static final int kFieldDx = 61;
    static final int kFieldDz = 4;
    static final int kWeaponKnockbackLevel = 10;

    FencingEventListener(JavaPlugin owner) {
        this.owner = owner;
        overworld().ifPresent(world -> {
            this.showDeathMessage = world.getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
        });
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
                playerLeft = null;
                playerRight = null;
                hitpointRight = 3;
                hitpointLeft = 3;
                overworld().ifPresent(world -> {
                    if (showDeathMessage != null) {
                        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, showDeathMessage);
                    }
                });
                break;
            case AWAIT_COUNTDOWN:
                // 範囲内に居るプレイヤーを観客席側に排除する
                Server server = owner.getServer();
                server.getOnlinePlayers().forEach(player -> {
                    Location loc = player.getLocation();
                    if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                        return;
                    }
                    int bx = loc.getBlockX();
                    int by = loc.getBlockY();
                    int bz = loc.getBlockZ();
                    if (x(kFieldX) <= bx && bx <= x(kFieldX) + kFieldDx && y(kFieldY) <= by && by <= y(kFieldY) + 5 && z(kFieldZ) <= bz && bz <= z(kFieldZ) + kFieldDz) {
                        loc.setZ(z(-273));
                        loc.setY(y(-19));
                        player.teleport(loc);
                    }
                });

                // 左ゲート構築
                execute(String.format("fill %s %s white_concrete", xyz(165, -16, -268), xyz(165, -18, -264)));
                execute(String.format("fill %s %s glass", xyz(165, -17, -267), xyz(165, -18, -265)));
                execute(String.format("setblock %s iron_door[facing=west,half=upper,hinge=left]", xyz(164, -17, -266)));
                execute(String.format("setblock %s iron_door[facing=west,half=lower,hinge=left]", xyz(164, -18, -266)));
                execute(String.format("setblock %s air", xyz(165, -17, -266)));
                execute(String.format("setblock %s heavy_weighted_pressure_plate", xyz(165, -18, -266)));

                // 右ゲート構築
                execute(String.format("fill %s %s white_concrete", xyz(103, -16, -264), xyz(103, -18, -268)));
                execute(String.format("fill %s %s glass", xyz(103, -17, -265), xyz(103, -18, -267)));
                execute(String.format("setblock %s iron_door[facing=east,half=upper,hinge=left]", xyz(104, -17, -266)));
                execute(String.format("setblock %s iron_door[facing=east,half=lower,hinge=left]", xyz(104, -18, -266)));
                execute(String.format("setblock %s air", xyz(103, -17, -266)));
                execute(String.format("setblock %s heavy_weighted_pressure_plate", xyz(103, -18, -266)));

                // バリアブロックの柵
                execute(String.format("fill %s %s barrier", xyz(165, -17, -269), xyz(103, -17, -269)));
                execute(String.format("fill %s %s barrier", xyz(104, -17, -263), xyz(165, -17, -263)));

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
                overworld().ifPresent(world -> {
                    Boolean value = world.getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
                    if (value != null) {
                        this.showDeathMessage = value;
                    }
                    world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
                });
                clearField();
                break;
        }
    }

    private Optional<World> overworld() {
        return owner.getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
    }

    private String xyz(int x, int y, int z) {
        // 座標が間違っていたらここでオフセットする
        return String.format("%d %d %d", x, y, z);
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

    @EventHandler
    @SuppressWarnings("unused")
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (_status != Status.RUN && _status != Status.AWAIT_DEATH) {
            return;
        }
        if (playerRight == null || playerLeft == null) {
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

        Team defenceTeam = null;
        if (defence.getUniqueId().equals(playerLeft) && offence.getUniqueId().equals(playerRight)) {
            defenceTeam = Team.LEFT;
        } else if (defence.getUniqueId().equals(playerRight) && offence.getUniqueId().equals(playerLeft)) {
            defenceTeam = Team.RIGHT;
        } else {
            return;
        }
        Team offenceTeam = TeamHostile(defenceTeam);

        Server server = owner.getServer();
        World world = entity.getWorld();

        Location location = offence.getLocation();
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();
        //NOTE: 攻撃側がフィールド内に居るかどうかだけ判定する.
        if (bx < x(kFieldX) || x(kFieldX) + kFieldDx < bx || by < y(kFieldY) || bz < z(kFieldZ) || z(kFieldZ) + kFieldDz < bz) {
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

        Player left = null, right = null;
        if (playerLeft != null) {
            left = getPlayer(playerLeft);
        }
        if (playerRight != null) {
            right = getPlayer(playerRight);
        }
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
        execute("clear @a iron_sword{tag:{" + kWeaponCustomTag + ":1b}}");

        Player left = null, right = null;
        if (playerLeft != null) {
            left = getPlayer(playerLeft);
        }
        if (playerRight != null) {
            right = getPlayer(playerRight);
        }
        if (left == null || right == null) {
            // 不在なのでノーコンテストに戻す
            setStatus(Status.IDLE);
            return;
        }

        // 敗北者を kill する
        if (hitpointLeft == 0) {
            Location loc = left.getLocation();
            // https://symtm.blog.fc2.com/blog-entry-96.html
            execute(String.format("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Flicker:0b,Trail:0b,Colors:[I;14188952],FadeColors:[I;14188952]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ()));
            execute(String.format("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:0,Flicker:1b,Trail:0b,Colors:[I;15790320],FadeColors:[I;15790320]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ()));
            execute(String.format("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:4,Flicker:0b,Trail:0b,Colors:[I;14602026],FadeColors:[I;14602026]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ()));
            left.setHealth(0);
        }
        if (hitpointRight == 0) {
            Location loc = right.getLocation();
            // https://symtm.blog.fc2.com/blog-entry-96.html
            execute(String.format("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Flicker:0b,Trail:0b,Colors:[I;14188952],FadeColors:[I;14188952]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ()));
            execute(String.format("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:0,Flicker:1b,Trail:0b,Colors:[I;15790320],FadeColors:[I;15790320]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ()));
            execute(String.format("summon firework_rocket %f %f %f {LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:4,Flicker:0b,Trail:0b,Colors:[I;14602026],FadeColors:[I;14602026]}],Flight:1}}}}", loc.getX(), loc.getY(), loc.getZ()));
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

        playerLeft = null;
        playerRight = null;
        setStatus(Status.IDLE);
    }

    private void clearField() {
        execute(String.format("fill %s %s air", xyz(102, -16, -269), xyz(165, -18, -264)));
        bossbarLeft.setVisible(false);
        bossbarRight.setVisible(false);
        execute("clear @a iron_sword{tag:{" + kWeaponCustomTag + ":1b}}");
        overworld().ifPresent(world -> {
            PlaceBlock(world, x(101), y(-19), z(-265), Material.BIRCH_WALL_SIGN, "[facing=north]", block -> {
                BlockState state = block.getState();
                if (!(state instanceof Sign)) {
                    return;
                }
                Sign sign = (Sign) state;
                sign.setLine(0, "§l[看板を右クリック]§r");
                sign.setLine(1, "右側エントリー");
                sign.update();
            });
            PlaceBlock(world, x(99), y(-19), z(-265), Material.BIRCH_WALL_SIGN, "[facing=north]", block -> {
                BlockState state = block.getState();
                if (!(state instanceof Sign)) {
                    return;
                }
                Sign sign = (Sign) state;
                sign.setLine(0, "§l[看板を右クリック]§r");
                sign.setLine(1, "エントリー解除");
                sign.update();
            });
            PlaceBlock(world, x(167), y(-19), z(-265), Material.BIRCH_WALL_SIGN, "[facing=north]", block -> {
                BlockState state = block.getState();
                if (!(state instanceof Sign)) {
                    return;
                }
                Sign sign = (Sign) state;
                sign.setLine(0, "§l[看板を右クリック]§r");
                sign.setLine(1, "左側エントリー");
                sign.update();
            });
            PlaceBlock(world, x(169), y(-19), z(-265), Material.BIRCH_WALL_SIGN, "[facing=north]", block -> {
                BlockState state = block.getState();
                if (!(state instanceof Sign)) {
                    return;
                }
                Sign sign = (Sign) state;
                sign.setLine(0, "§l[看板を右クリック]§r");
                sign.setLine(1, "エントリー解除");
                sign.update();
            });
        });
    }

    private static void PlaceBlock(World world, int x, int y, int z, Material material, String data, Consumer<Block> then) {
        int cx = x >> 4;
        int cz = z >> 4;
        boolean loaded = world.isChunkLoaded(cx, cz);
        world.loadChunk(cx, cz);
        BlockData blockData = material.createBlockData(data);
        world.setBlockData(x, y, z, blockData);
        Block block = world.getBlockAt(x, y, z);
        then.accept(block);
        if (!loaded) {
            world.unloadChunk(cx, cz);
        }
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

        if (bx == x(134) && by == y(-18) && bz == z(-272)) {
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
        Location location = block.getLocation();
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();
        if (bx == x(101) && by == y(-19) && bz == z(-265)) {
            onClickJoin(player, Team.RIGHT);
        } else if (bx == x(167) && by == y(-19) && bz == z(-265)) {
            onClickJoin(player, Team.LEFT);
        } else if (bx == x(169) && by == y(-19) && bz == z(-265)) {
            onClickLeave(player, Team.LEFT);
        } else if (bx == x(99) && by == y(-19) && bz == z(-265)) {
            onClickLeave(player, Team.RIGHT);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (playerLeft != null && player.getUniqueId().equals(playerLeft)) {
            clearPlayer(Team.LEFT);
        }
        if (playerRight != null && player.getUniqueId().equals(playerRight)) {
            clearPlayer(Team.RIGHT);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onServerLoad(ServerLoadEvent e) {
        if (e.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }
        BoundingBox bounds = getBounds();
        bossbarLeft = new Bossbar(owner, kBossbarLeft, "<<< " + TeamName(Team.LEFT) + " <<<", bounds);
        bossbarLeft.setMax(3);
        bossbarLeft.setValue(3);
        bossbarLeft.setColor("green");

        bossbarRight = new Bossbar(owner, kBossbarRight, ">>> " + TeamName(Team.RIGHT) + " >>>", bounds);
        bossbarRight.setMax(3);
        bossbarRight.setValue(3);
        bossbarRight.setColor("green");

        clearField();
    }

    private @Nullable UUID getPlayerUid(Team team) {
        if (team == Team.RIGHT) {
            return playerRight;
        } else if (team == Team.LEFT) {
            return playerLeft;
        } else {
            return null;
        }
    }

    private void joinPlayer(@Nonnull Player player, Team team) {
        if (team == Team.RIGHT) {
            if (playerLeft != null && playerLeft.equals(player.getUniqueId())) {
                broadcastUnofficial(ChatColor.RED + "[フェンシング] " + player.getName() + "は" + TeamName(Team.LEFT) + "としてエントリー済みです");
            } else {
                playerRight = player.getUniqueId();
                execute("give @p[name=\"" + player.getName() + "\"] " + Weapon());
                broadcast("[フェンシング] " + player.getName() + "がエントリーしました（" + TeamName(team) + "）");
            }
        } else if (team == Team.LEFT) {
            if (playerRight != null && playerRight.equals(player.getUniqueId())) {
                broadcastUnofficial(ChatColor.RED + "[フェンシング] " + player.getName() + "は" + TeamName(Team.RIGHT) + "としてエントリー済みです");
            } else {
                playerLeft = player.getUniqueId();
                execute("give @p[name=\"" + player.getName() + "\"] " + Weapon());
                broadcast("[フェンシング] " + player.getName() + "がエントリーしました（" + TeamName(team) + "）");
            }
        }
        if (playerRight == null && playerLeft == null) {
            setStatus(Status.IDLE);
        } else {
            setStatus(Status.AWAIT_COUNTDOWN);
        }
    }

    private String Weapon() {
        UUID attributeUid = UUID.randomUUID();
        return "iron_sword{tag:{" + kWeaponCustomTag + ":1b},HideFlags:2,Enchantments:[{id:knockback,lvl:10}],AttributeModifiers:[{AttributeName:\"generic.attack_damage\",Amount:0,Operation:0,UUID:" + UUIDString(attributeUid) + "}]}";
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
            playerRight = null;
        } else if (team == Team.LEFT) {
            playerLeft = null;
        }
        if (playerRight == null && playerLeft == null) {
            setStatus(Status.IDLE);
        } else {
            setStatus(Status.AWAIT_COUNTDOWN);
        }
    }

    private void broadcast(String message) {
        owner.getServer().broadcastMessage(message);
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String message) {
        broadcast(message);
    }

    private void onClickJoin(@Nonnull Player player, Team team) {
        if (_status != Status.IDLE && _status != Status.AWAIT_COUNTDOWN) {
            return;
        }
        UUID uid = getPlayerUid(team);
        if (uid != null) {
            //TODO: 既に join 済みの時のメッセージ
            return;
        }
        joinPlayer(player, team);
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

    private static Team TeamHostile(Team team) {
        if (team == Team.LEFT) {
            return Team.RIGHT;
        } else {
            return Team.LEFT;
        }
    }

    private void onClickLeave(@Nonnull Player player, Team team) {
        // status をリセットするため _status == Status.IDLE チェックは入れずに強制的にエントリー解除処理する
        UUID uid = getPlayerUid(team);
        if (uid == null) {
            return;
        }
        if (!uid.equals(player.getUniqueId())) {
            return;
        }
        clearPlayer(team);
        execute("clear @p[name=\"" + player.getName() + "\"]" + " iron_sword{tag:{" + kWeaponCustomTag + ":1b}}");
        broadcastUnofficial("[フェンシング] " + player.getName() + "がエントリー解除しました（" + TeamName(team) + "）");
    }

    private void onClickStart() {
        if (_status != Status.IDLE && _status != Status.AWAIT_COUNTDOWN) {
            return;
        }
        Player left = null;
        Player right = null;
        int numLeft = 0;
        int numRight = 0;
        if (playerLeft != null) {
            left = getPlayer(playerLeft);
            if (left == null) {
                clearPlayer(Team.LEFT);
            } else {
                numLeft += 1;
            }
        }
        if (playerRight != null) {
            right = getPlayer(playerRight);
            if (right == null) {
                clearPlayer(Team.RIGHT);
            } else {
                numRight += 1;
            }
        }
        if (numLeft != numRight || numLeft < 1 || numRight < 1) {
            broadcast("参加人数が正しくありません（" + TeamName(Team.LEFT) + " : " + numLeft + "人、" + TeamName(Team.RIGHT) + " : " + numRight + "人）");
            return;
        }
        left.setHealth(left.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        right.setHealth(right.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        setStatus(Status.COUNTDOWN);
        Countdown.Then(owner, (count) -> {
            Server server = owner.getServer();
            PlayNote(server, this::isInField, Instrument.BIT, new Note(12));
            execute(String.format("title %s title %d", getPlayersSelector(), count));
        }, () -> {
            if (_status == Status.COUNTDOWN) {
                PlaySound(owner.getServer(), this::isInField, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
                execute(String.format("title %s title \"START!!!\"", getPlayersSelector()));
                setStatus(Status.RUN);
            }
        });
    }

    private String getPlayersSelector() {
        BoundingBox box = getBounds();
        return String.format("@p[x=%f,y=%f,z=%f,dx=%f,dy=%f,dz=%f]", box.getMinX(), box.getMinY(), box.getMinZ(), box.getWidthX(), box.getHeight(), box.getWidthZ());
    }

    private BoundingBox getBounds() {
        return new BoundingBox(x(85), y(-20), z(-280), x(171), y(384), z(-253));
    }

    private boolean isInField(Player it) {
        Location loc = it.getLocation();
        double bx = loc.getX();
        double by = loc.getY();
        double bz = loc.getZ();
        BoundingBox box = getBounds();
        return box.contains(bx, by, bz) && it.getWorld().getEnvironment() == World.Environment.NORMAL;
    }

    private void execute(String command) {
        Server server = owner.getServer();
        CommandSender sender = server.getConsoleSender();
        server.dispatchCommand(sender, command);
    }

    private @Nullable Player getPlayer(@Nonnull UUID uid) {
        World world = overworld().orElse(null);
        if (world == null) {
            return null;
        }
        return world.getPlayers().stream().filter(it -> it.getUniqueId().equals(uid)).findFirst().orElse(null);
    }

    private Player findNearest(Location location) {
        World world = overworld().orElse(null);
        if (world == null) {
            return null;
        }
        Optional<Entity> entity = world.getNearbyEntities(location, 2.5, 2.5, 2.5, (it) -> it.getType() == EntityType.PLAYER).stream().min((a, b) -> {
            double distanceToA = a.getLocation().distanceSquared(location);
            double distanceToB = b.getLocation().distanceSquared(location);
            return Double.compare(distanceToA, distanceToB);
        });
        if (entity.isEmpty()) {
            return null;
        }
        Entity e = entity.get();
        if (e instanceof Player) {
            return (Player) e;
        } else {
            return null;
        }
    }

    static void PlayNote(Server server, Predicate<Player> predicate, Instrument instrument, Note note) {
        server.getOnlinePlayers().stream().filter(predicate).forEach(player -> {
            player.playNote(player.getLocation(), instrument, note);
        });
    }

    static void PlaySound(Server server, Predicate<Player> predicate, Sound sound, float volume, float pitch) {
        server.getOnlinePlayers().stream().filter(predicate).forEach(player -> {
            player.playSound(player.getLocation(), sound, volume, pitch);
        });
    }
}