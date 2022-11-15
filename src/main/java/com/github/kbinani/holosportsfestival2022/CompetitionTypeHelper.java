package com.github.kbinani.holosportsfestival2022;

public class CompetitionTypeHelper {
    private CompetitionTypeHelper() {
    }

    public static String ToString(CompetitionType type) {
        switch (type) {
            case DARUMA:
                return "だるまさんがころんだ";
            case RELAY:
                return "リレー";
            case FENCING:
                return "フェンシングPVP";
            case MOB:
                return "MOB討伐レース";
            case BOAT_RACE:
                return "水上レース";
            default:
                return type.toString();
        }
    }
}