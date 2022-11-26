package com.github.kbinani.holosportsfestival2022.fencing;

import com.github.kbinani.holosportsfestival2022.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
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
import org.bukkit.inventory.ItemFlag;
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

public class FencingEventListener implements Listener, Competition {
    private @Nullable Player left;
    private @Nullable Player right;
    private int hitpointLeft = 3;
    private int hitpointRight = 3;
    private @Nullable Bossbar bossbarLeft;
    private @Nullable Bossbar bossbarRight;
    private boolean initialized = false;
    private final long loadDelay;
    private final MainDelegate delegate;

    static final String kBossbarLeft = "sports_festival_2022_bossbar_left";
    static final String kBossbarRight = "sports_festival_2022_bossbar_right";
    static final String kWeaponCustomTag = "hololive_sports_festival_2022_fencing";
    static final int kWeaponKnockbackLevel = 10;

    public FencingEventListener(MainDelegate delegate, long loadDelay) {
        this.loadDelay = loadDelay;
        this.delegate = delegate;
    }

    private Status _status = Status.IDLE;

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

                // 北側バリアブロックの柵
                // 間違って入場してしまった場合自力で脱出できるよう, 北側の柵はカウントダウンが始まってから設置する
                fill(new Point3i(165, -17, -269), new Point3i(103, -17, -269), "air");

                // 南側バリアブロックの柵
                fill(new Point3i(104, -17, -263), new Point3i(165, -17, -263), "barrier");

                ensureRightBossbar().setVisible(false);
                ensureLeftBossbar().setVisible(false);

                break;
            case COUNTDOWN:
                // 北側バリアブロックの柵
                // 間違って入場してしまった場合自力で脱出できるよう, 北側の柵はカウントダウンが始まってから設置する
                fill(new Point3i(165, -17, -269), new Point3i(103, -17, -269), "barrier");

                // 範囲内に居るプレイヤーを観客席側に排除する
                Players.Within(delegate.mainGetWorld(), offset(kFieldBounds), player -> {
                    if (getCurrentTeam(player) != null) {
                        return;
                    }
                    if (player.getGameMode() == GameMode.SPECTATOR) {
                        return;
                    }
                    Location loc = player.getLocation();
                    loc.setZ(z(-273));
                    loc.setY(y(-19));
                    player.teleport(loc);
                });

                hitpointRight = 3;
                hitpointLeft = 3;

                // bossbar 追加
                ensureRightBossbar().setValue(3);
                ensureRightBossbar().setVisible(true);
                ensureLeftBossbar().setValue(3);
                ensureLeftBossbar().setVisible(true);

                broadcast("");
                broadcast("[フェンシング] 競技を開始します！").log();
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
        Editor.Fill(delegate.mainGetWorld(), offset(from), offset(to), block);
    }

    private void setBlock(Point3i p, String block) {
        Editor.SetBlock(delegate.mainGetWorld(), offset(p), block);
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

        ensureLeftBossbar().setValue(hitpointLeft);
        ensureRightBossbar().setValue(hitpointRight);

        if (shouldKill) {
            delegate.mainRunTaskLater(this::decideResult, 1);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        if (e.getFrom() == delegate.mainGetWorld()) {
            onClickLeave(player);
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

        clearItem(left);
        clearItem(right);
        giveParticipationReward(left);
        giveParticipationReward(right);

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

        delegate.mainRunTaskLater(this::killLosers, 30);
    }

    private void killLosers() {
        if (_status != Status.AWAIT_DEATH) {
            return;
        }
        //NOTE: 敗北者を kill する前にアイテムを回収
        if (left != null) {
            clearItem(left);
        }
        if (right != null) {
            clearItem(right);
        }

        if (left == null || right == null) {
            // 不在なのでノーコンテストに戻す
            setStatus(Status.IDLE);
            return;
        }

        // 敗北者を kill する
        if (hitpointLeft == 0) {
            Location loc = left.getLocation();
            launchLoserFireworkRocket(loc);
            launchWinnerFireworkRocket(Team.RIGHT);
            left.setHealth(0);
        }
        if (hitpointRight == 0) {
            Location loc = right.getLocation();
            launchLoserFireworkRocket(loc);
            launchWinnerFireworkRocket(Team.LEFT);
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
        broadcast("[試合終了]").log();
        broadcast(message).log();
        broadcast("-----------------------");
        broadcast("");
    }

    private void launchWinnerFireworkRocket(Team team) {
        World world = delegate.mainGetWorld();
        int[] colors = new int[]{FireworkRocket.Color.LIGHT_BLUE, FireworkRocket.Color.PINK, FireworkRocket.Color.YELLOW};
        int x = team == Team.RIGHT ? 99 : 139;
        int index = team == Team.RIGHT ? 2 : 0;
        for (int i = 0; i < 7; i++, index++) {
            int y = i % 2 == 0 ? -11 : -6;
            Point3i pos = offset(new Point3i(x + i * 5, y, -254));
            int color = colors[index % 3];
            FireworkRocket.Launch(world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, new int[]{color}, new int[]{}, 10, 1, false, false);
        }
    }

    private void launchLoserFireworkRocket(Location loc) {
        World world = delegate.mainGetWorld();
        FireworkRocket.Launch(world, loc.getX(), loc.getY(), loc.getZ(), new int[]{FireworkRocket.Color.PINK}, new int[]{FireworkRocket.Color.PINK}, 0, 1, false, false);
        FireworkRocket.Launch(world, loc.getX(), loc.getY(), loc.getZ(), new int[]{FireworkRocket.Color.WHITE}, new int[]{FireworkRocket.Color.WHITE}, 0, 0, true, false);
        FireworkRocket.Launch(world, loc.getX(), loc.getY(), loc.getZ(), new int[]{FireworkRocket.Color.YELLOW}, new int[]{FireworkRocket.Color.YELLOW}, 0, 4, false, false);
    }

    private void clearField() {
        fill(new Point3i(102, -16, -269), new Point3i(165, -18, -264), "air");
        ensureLeftBossbar().setVisible(false);
        ensureRightBossbar().setVisible(false);
        Bukkit.getServer().getOnlinePlayers().forEach(this::clearItem);
        World world = delegate.mainGetWorld();
        Editor.WallSign(world, offset(new Point3i(101, -19, -265)), BlockFace.NORTH, "右側エントリー");
        Editor.WallSign(world, offset(new Point3i(99, -19, -265)), BlockFace.NORTH, "エントリー解除");
        Editor.WallSign(world, offset(new Point3i(167, -19, -265)), BlockFace.NORTH, "左側エントリー");
        Editor.WallSign(world, offset(new Point3i(169, -19, -265)), BlockFace.NORTH, "エントリー解除");
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

        Point3i location = new Point3i(block.getLocation());
        if (location.equals(offset(kButtonStart))) {
            onClickStart();
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
        } else if (location.equals(offset(kButtonReset))) {
            if (player.getGameMode() == GameMode.CREATIVE || player.isOp()) {
                competitionReset();
            }
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

        delegate.mainRunTaskLater(() -> {
            BoundingBox bounds = getAnnounceBounds();
            ensureLeftBossbar();
            ensureRightBossbar();
            clearField();
        }, loadDelay);
    }

    private @Nonnull Bossbar ensureLeftBossbar() {
        if (bossbarLeft == null) {
            bossbarLeft = new Bossbar(delegate, kBossbarLeft, "<<< " + TeamName(Team.LEFT) + " <<<", getAnnounceBounds());
            bossbarLeft.setMax(3);
            bossbarLeft.setValue(3);
            bossbarLeft.setColor(BarColor.GREEN);
        }
        return bossbarLeft;
    }

    private @Nonnull Bossbar ensureRightBossbar() {
        if (bossbarRight == null) {
            bossbarRight = new Bossbar(delegate, kBossbarRight, ">>> " + TeamName(Team.RIGHT) + " >>>", getAnnounceBounds());
            bossbarRight.setMax(3);
            bossbarRight.setValue(3);
            bossbarRight.setColor(BarColor.GREEN);
        }
        return bossbarRight;
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
        Location location = new Location(delegate.mainGetWorld(), respawn.x, respawn.y, respawn.z);
        e.setRespawnLocation(location);

        // ここで setStatus(Status.IDLE) すると相討ちの場合に後に onPlayerRespawn する側のリスポン位置が設定されない.
        // 1 tick 遅れて IDLE に戻す.

        delegate.mainRunTask(() -> {
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

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerMove(PlayerMoveEvent e) {
        if (_status == Status.AWAIT_DEATH) {
            // どこまで飛んでいくかわからないので勝敗が決して死に待ち状態の間は場外に出たかどうかの確認は省略する
            return;
        }
        Player player = e.getPlayer();
        World world = player.getWorld();
        if (delegate.mainGetWorld() != world) {
            return;
        }
        Team team = getCurrentTeam(player);
        if (team == null) {
            return;
        }
        if (getAnnounceBounds().contains(player.getLocation().toVector())) {
            return;
        }
        player.sendMessage(ChatColor.RED + "[フェンシング] 場外に出たためエントリー解除となります");
        onClickLeave(player);
    }

    private void joinPlayer(@Nonnull Player player, Team team) {
        delegate.mainClearCompetitionItems(player);
        ItemStack sword = ItemBuilder.For(Material.IRON_SWORD)
                .amount(1)
                .customByteTag(kWeaponCustomTag, (byte) 1)
                .enchant(Enchantment.KNOCKBACK, 10)
                .attributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, "", 0, AttributeModifier.Operation.ADD_NUMBER)
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        if (!player.getInventory().addItem(sword).isEmpty()) {
            player.sendMessage(ChatColor.RED + "インベントリがいっぱいで競技用アイテムが渡せません");
            clearItem(player);
            return;
        }
        if (team == Team.RIGHT) {
            right = player;
        } else if (team == Team.LEFT) {
            left = player;
        }
        broadcast("[フェンシング] %sがエントリーしました（%s）", player.getName(), TeamName(team)).log();
        if (right == null && left == null) {
            setStatus(Status.IDLE);
        } else {
            setStatus(Status.AWAIT_COUNTDOWN);
        }
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

    private void giveParticipationReward(Player player) {
        ItemStack cookedBeef = ItemBuilder.For(Material.COOKED_BEEF)
                .amount(15)
                .build();
        player.getInventory().addItem(cookedBeef);
    }

    private ConsoleLogger broadcast(String format, Object... args) {
        String msg = String.format(format, args);
        Players.Within(delegate.mainGetWorld(), getAnnounceBounds(), player -> player.sendMessage(msg));
        return new ConsoleLogger(msg, "[フェンシング]", delegate.mainGetLogger());
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private ConsoleLogger broadcastUnofficial(String format, Object... args) {
        return broadcast(format, args);
    }

    private void onClickJoin(@Nonnull Player player, Team team) {
        if (_status != Status.IDLE && _status != Status.AWAIT_COUNTDOWN) {
            return;
        }
        CompetitionType type = delegate.mainGetCurrentCompetition(player);
        if (type != null && type != CompetitionType.FENCING) {
            //NOTE: 本家では全チャになる
            player.sendMessage(String.format("[フェンシング] %sは既に%sにエントリー済みです", player.getName(), CompetitionTypeHelper.ToString(type)));
            return;
        }
        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) {
            player.sendMessage("[フェンシング] ゲームモードはサバイバルかアドベンチャーの場合のみ参加可能です");
            return;
        }
        if (getCurrentTeam(player) != null) {
            broadcast("[フェンシング] %sはエントリー済みです", player.getName());
            return;
        }
        if (team == Team.RIGHT && right != null) {
            player.sendMessage(String.format("[フェンシング] %sには既に%sがエントリー済みです", TeamName(team), right.getName()));
            return;
        }
        if (team == Team.LEFT && left != null) {
            player.sendMessage(String.format("[フェンシング] %sには既に%sがエントリー済みです", TeamName(team), left.getName()));
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
        return switch (team) {
            case LEFT -> "LEFT SIDE";
            case RIGHT -> "RIGHT SIDE";
        };
    }

    private void onClickLeave(@Nonnull Player player) {
        // status をリセットするため _status == Status.IDLE チェックは入れずに強制的にエントリー解除処理する
        Team color = getCurrentTeam(player);
        if (color == null) {
            return;
        }
        clearPlayer(color);
        clearItem(player);
        broadcast("[フェンシング] %sがエントリー解除しました", player.getName()).log();
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
            if (container.has(NamespacedKey.minecraft(kWeaponCustomTag), PersistentDataType.BYTE)) {
                inventory.clear(i);
            }
        }
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
        delegate.mainCountdownThen(new BoundingBox[]{getAnnounceBounds()}, (count) -> _status == Status.COUNTDOWN, () -> {
            if (_status != Status.COUNTDOWN) {
                return false;
            }
            setStatus(Status.RUN);
            return true;
        }, 20, Countdown.TitleSet.Default());
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

    @Override
    public boolean competitionIsJoined(Player player) {
        return getCurrentTeam(player) != null;
    }

    @NotNull
    @Override
    public CompetitionType competitionGetType() {
        return CompetitionType.FENCING;
    }

    @Override
    public void competitionClearItems(Player player) {
        clearItem(player);
    }

    @Override
    public void competitionReset() {
        setStatus(Status.IDLE);
        clearField();
        left = null;
        right = null;
        hitpointLeft = 3;
        hitpointRight = 3;
        if (bossbarLeft != null) {
            bossbarLeft.setVisible(false);
        }
        if (bossbarRight != null) {
            bossbarRight.setVisible(false);
        }
        Bukkit.getServer().broadcastMessage(CompetitionTypeHelper.ToString(competitionGetType()) + "をリセットしました");
    }

    private static final Point3i kButtonRightJoin = new Point3i(101, -19, -265);
    private static final Point3i kButtonLeftJoin = new Point3i(167, -19, -265);
    private static final Point3i kButtonLeftLeave = new Point3i(169, -19, -265);
    private static final Point3i kButtonRightLeave = new Point3i(99, -19, -265);
    private static final Point3i kButtonStart = new Point3i(134, -18, -272);
    private static final Point3i kButtonReset = new Point3i(99, -20, -264);
    private static final BoundingBox kAnnounceBounds = new BoundingBox(85, -20, -280, 171, 384, -253);
    private static final BoundingBox kFieldBounds = new BoundingBox(103, -18, -268, 165, -13, -263);
    private static final Point3i kRespawnLocation = new Point3i(96, -19, -274);
}