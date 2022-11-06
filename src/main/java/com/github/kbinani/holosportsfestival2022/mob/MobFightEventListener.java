package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.Loader;
import com.github.kbinani.holosportsfestival2022.Point3i;
import com.github.kbinani.holosportsfestival2022.WallSign;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.Optional;

public class MobFightEventListener implements Listener, StageDelegate {
    private final JavaPlugin owner;
    private boolean initialized = false;

    public MobFightEventListener(JavaPlugin owner) {
        this.owner = owner;
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

    private void resetField() {
        overworld().ifPresent(world -> {
            Loader.LoadChunk(world, offset(kBounds));
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
            newLevel(tc);
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

    enum TeamColor {
        RED,
        YELLOW,
        WHITE,
    }

    private static final BoundingBox kBounds = new BoundingBox(-26, -61, -424, 79, -19, -244);

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
}