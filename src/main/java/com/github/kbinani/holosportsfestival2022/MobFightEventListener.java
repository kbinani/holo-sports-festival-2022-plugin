package com.github.kbinani.holosportsfestival2022;

import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MobFightEventListener implements Listener {
    private final JavaPlugin owner;
    private boolean initialized = false;

    MobFightEventListener(JavaPlugin owner) {
        this.owner = owner;
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (initialized) {
            return;
        }
        initialized = true;
        //TODO: チャンクを読み込む処理
        resetField();
    }

    private void resetField() {
        WallSign.Place(offset(kButtonWhiteLeave), BlockFace.SOUTH, "エントリー解除");
        WallSign.Place(offset(kButtonWhiteJoinArrow), BlockFace.SOUTH, "白組", "エントリー", "（弓）");
        WallSign.Place(offset(kButtonWhiteJoinSword), BlockFace.SOUTH, "白組", "エントリー", "（剣）");

        WallSign.Place(offset(kButtonRedLeave), BlockFace.SOUTH, "エントリー解除");
        WallSign.Place(offset(kButtonRedJoinArrow), BlockFace.SOUTH, "赤組", "エントリー", "（弓）");
        WallSign.Place(offset(kButtonRedJoinSword), BlockFace.SOUTH, "赤組", "エントリー", "（剣）");

        WallSign.Place(offset(kButtonYellowLeave), BlockFace.SOUTH, "エントリー解除");
        WallSign.Place(offset(kButtonYellowJoinArrow), BlockFace.SOUTH, "黃組", "エントリー", "（弓）");
        WallSign.Place(offset(kButtonYellowJoinSword), BlockFace.SOUTH, "黃組", "エントリー", "（剣）");
    }

    private Point3i offset(Point3i p) {
        // 座標が間違っていたらここでオフセットする
        return new Point3i(p.x, p.y, p.z);
    }

    private static final Point3i kButtonWhiteLeave = new Point3i(53, -59, -253);
    private static final Point3i kButtonWhiteJoinArrow = new Point3i(55, -59, -253);
    private static final Point3i kButtonWhiteJoinSword = new Point3i(57, -59, -253);

    private static final Point3i kButtonRedLeave = new Point3i(22, -59, -253);
    private static final Point3i kButtonRedJoinArrow = new Point3i(24, -59, -253);
    private static final Point3i kButtonRedJoinSword = new Point3i(26, -59, -253);

    private static final Point3i kButtonYellowLeave = new Point3i(-9, -59, -253);
    private static final Point3i kButtonYellowJoinArrow = new Point3i(-7, -59, -253);
    private static final Point3i kButtonYellowJoinSword = new Point3i(-5, -59, -253);
}