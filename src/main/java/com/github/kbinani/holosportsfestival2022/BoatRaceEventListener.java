package com.github.kbinani.holosportsfestival2022;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

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

    class Participant {
        Player driver;
        Player shooter;
    }

    private final Map<Team, Participant> teams = new HashMap<>();
    private static final Point3i kYellowEntryShooter = new Point3i(-56, -59, -198);
    private static final Point3i kYellowEntryDriver = new Point3i(-56, -59, -200);
    private static final Point3i kWhiteEntryShooter = new Point3i(-56, -59, -202);
    private static final Point3i kWhiteEntryDriver = new Point3i(-56, -59, -204);
    private static final Point3i kRedEntryShooter = new Point3i(-56, -59, -206);
    private static final Point3i kRedEntryDriver = new Point3i(-56, -59, -208);
    private static final Point3i kLeaveButton = new Point3i(-56, -59, -210);

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
        WallSign.Place(offset(kYellowEntryShooter), BlockFace.WEST, "黄組", "エントリー", "（妨害担当）");
        WallSign.Place(offset(kYellowEntryDriver), BlockFace.WEST, "黄組", "エントリー", "（操縦担当）");
        WallSign.Place(offset(kWhiteEntryShooter), BlockFace.WEST, "白組", "エントリー", "（妨害担当）");
        WallSign.Place(offset(kWhiteEntryDriver), BlockFace.WEST, "白組", "エントリー", "（操縦担当）");
        WallSign.Place(offset(kRedEntryShooter), BlockFace.WEST, "赤組", "エントリー", "（妨害担当）");
        WallSign.Place(offset(kRedEntryDriver), BlockFace.WEST, "赤組", "エントリー", "（操縦担当）");
        WallSign.Place(offset(kLeaveButton), BlockFace.WEST, "エントリー解除");
    }

    private void broadcast(String msg) {
        owner.getServer().broadcastMessage(msg);
    }

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    private void broadcastUnofficial(String msg) {

    }

    private void onClickJoin(Player player, Team team, Role tole) {

    }

    private void onClickLeave(Player player) {

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
}