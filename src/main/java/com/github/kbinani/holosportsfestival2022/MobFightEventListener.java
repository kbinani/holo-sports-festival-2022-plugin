package com.github.kbinani.holosportsfestival2022;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
            Level level = newLevel(tc);
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

    abstract class Stage {
        protected final Point3i origin;

        // ステージ室内の北西下の角の座標を指定してステージを初期化する
        Stage(Point3i origin) {
            this.origin = new Point3i(origin);
        }

        // 入り口を開閉する
        abstract void setEntranceOpened(boolean opened);

        // 出口を開閉する
        abstract void setExitOpened(boolean opened);

        // ステージ室内の内法寸法を返す
        abstract Point3i getSize();

        BoundingBox getBounds() {
            Point3i size = getSize();
            return new BoundingBox(origin.x, origin.y, origin.z, origin.x + size.x, origin.y + size.y, origin.z + size.z);
        }

        protected void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
            execute("fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block);
        }
    }

    class PlainsStage extends Stage {
        PlainsStage(Point3i origin) {
            super(origin);
        }

        @Override
        public void setEntranceOpened(boolean opened) {
            fill(x(0), y(-59), z(-255), x(1), y(-58), z(-255), opened ? "air" : "bedrock");
        }

        @Override
        public void setExitOpened(boolean opened) {
            fill(x(1), y(-59), z(-276), x(0), y(-58), z(-276), opened ? "air" : "bedrock");
            fill(x(1), y(-48), z(-276), x(0), y(-47), z(-276), opened ? "air" : "bedrock");
        }

        @Override
        public Point3i getSize() {
            return new Point3i(20, 20, 20);
        }

        // 黄色チーム用 plains ステージの原点: (-9, -59, -275)

        private int x(int x) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.x + (x - (-9));
        }

        private int y(int y) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.y + (y - (-59));
        }

        private int z(int z) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.z + (z - (-275));
        }
    }

    class OceanMonumentStage extends Stage {
        OceanMonumentStage(Point3i origin) {
            super(origin);
        }

        @Override
        public void setEntranceOpened(boolean opened) {
            fill(x(1), y(-59), z(-280), x(0), y(-58), z(-280), opened ? "air" : "bedrock");
        }

        @Override
        public void setExitOpened(boolean opened) {
            fill(x(0), y(-59), z(-301), x(0), y(-58), z(-301), opened ? "warped_wall_sign[facing=east]" : "bedrock");
            fill(x(1), y(-59), z(-301), x(0), y(-58), z(-301), opened ? "warped_wall_sign[facing=west]" : "bedrock");
        }

        @Override
        public Point3i getSize() {
            return new Point3i(20, 20, 20);
        }

        // 黄色チーム用 ocean monument ステージの原点: (-9, -59, -300)

        private int x(int x) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.x + (x - (-9));
        }

        private int y(int y) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.y + (y - (-59));
        }

        private int z(int z) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.z + (z - (-300));
        }
    }

    class NetherStage extends Stage {
        NetherStage(Point3i origin) {
            super(origin);
        }

        @Override
        public void setEntranceOpened(boolean opened) {
            fill(x(1), y(-59), z(-305), x(0), y(-58), z(-305), opened ? "air" : "bedrock");
        }

        @Override
        public void setExitOpened(boolean opened) {
            fill(x(1), y(-59), z(-326), x(0), y(-58), z(-326), opened ? "air" : "bedrock");
        }

        @Override
        public Point3i getSize() {
            return new Point3i(20, 20, 20);
        }

        // 黄色チーム用 nether ステージの原点: (-9, -59, -325)

        private int x(int x) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.x + (x - (-9));
        }

        private int y(int y) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.y + (y - (-59));
        }

        private int z(int z) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.z + (z - (-325));
        }
    }

    class WoodlandMansionStage extends Stage {
        WoodlandMansionStage(Point3i origin) {
            super(origin);
        }

        @Override
        public void setEntranceOpened(boolean opened) {
            fill(x(1), y(-59), z(-330), x(0), y(-58), z(-330), opened ? "air" : "bedrock");
        }

        @Override
        public void setExitOpened(boolean opened) {
            fill(x(1), y(-59), z(-352), x(0), y(-58), z(-352), opened ? "air" : "bedrock");
        }

        @Override
        public Point3i getSize() {
            return new Point3i(20, 20, 21);
        }

        // 黄色チーム用 woodland mansion ステージの原点: (-9, -59, -351)

        private int x(int x) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.x + (x - (-9));
        }

        private int y(int y) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.y + (y - (-59));
        }

        private int z(int z) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.z + (z - (-351));
        }
    }

    class ShootingStage extends Stage {
        ShootingStage(Point3i origin) {
            super(origin);
        }

        @Override
        public void setEntranceOpened(boolean opened) {
            fill(x(1), y(-59), z(-356), x(0), y(-58), z(-356), opened ? "air" : "bedrock");
        }

        @Override
        public void setExitOpened(boolean opened) {
            fill(x(1), y(-59), z(-377), x(0), y(-58), z(-377), opened ? "air" : "bedrock");
        }

        @Override
        public Point3i getSize() {
            return new Point3i(20, 20, 20);
        }

        // 黄色チーム用 shooting ステージの原点: (-9, -59, -376)

        private int x(int x) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.x + (x - (-9));
        }

        private int y(int y) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.y + (y - (-59));
        }

        private int z(int z) {
            // 黄色ステージの即値を使って実装するのでオフセットする.
            return origin.z + (z - (-376));
        }
    }

    class Level {
        private final List<Stage> stages;

        // エントリー解除看板が貼ってあるブロックの座標を原点としてステージ群を初期化する
        Level(Point3i origin) {
            this.stages = new LinkedList<>();
            // origin = (-9, -59, -254) の時:
            // (-9, -59, -275)
            this.stages.add(new PlainsStage(new Point3i(origin.x, origin.y, origin.z - 21)));
            // (-9, -59, -300)
            this.stages.add(new OceanMonumentStage(new Point3i(origin.x, origin.y, origin.z - 46)));
            // (-9, -59, -325)
            this.stages.add(new NetherStage(new Point3i(origin.x, origin.y, origin.z - 71)));
            // (-9, -59, -351)
            this.stages.add(new WoodlandMansionStage(new Point3i(origin.x, origin.y, origin.z - 97)));
            // (-9, -59, -376)
            this.stages.add(new ShootingStage(new Point3i(origin.x, origin.y, origin.z - 122)));
            for (Stage stage : stages) {
                stage.setEntranceOpened(false);
                stage.setExitOpened(false);
            }
        }
    }

    private Level newLevel(TeamColor color) {
        switch (color) {
            case YELLOW:
                return new Level(offset(new Point3i(-9, -59, -254)));
            case RED:
                return new Level(offset(new Point3i(22, -59, -254)));
            case WHITE:
                return new Level(offset(new Point3i(53, -59, -254)));
        }
        return null;
    }

    private void execute(String format, Object... args) {
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