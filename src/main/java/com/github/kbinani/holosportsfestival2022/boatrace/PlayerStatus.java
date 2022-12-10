package com.github.kbinani.holosportsfestival2022.boatrace;

public class PlayerStatus {
    private final int rawValue;

    private PlayerStatus(int rawValue) {
        this.rawValue = rawValue;
    }

    public static PlayerStatus IDLE = new PlayerStatus(0);
    public static PlayerStatus STARTED = new PlayerStatus(1);
    public static PlayerStatus CLEARED_CHECKPOINT1 = new PlayerStatus(2);
    public static PlayerStatus CLEARED_START_LINE1 = new PlayerStatus(3);
    public static PlayerStatus CLEARED_CHECKPOINT2 = new PlayerStatus(4);
    public static PlayerStatus FINISHED = new PlayerStatus(5);

    public boolean less(PlayerStatus other) {
        return rawValue < other.rawValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PlayerStatus s)) {
            return false;
        }
        return rawValue == s.rawValue;
    }

    public int remainingRound() {
        if (this == IDLE || this == STARTED || this == CLEARED_CHECKPOINT1) {
            return 2;
        } else if (this == CLEARED_START_LINE1 || this == CLEARED_CHECKPOINT2) {
            return 1;
        } else {
            return 0;
        }
    }
}
