package com.github.kbinani.holosportsfestival2022.mob;

import java.util.HashSet;
import java.util.Set;

class Race {
    final Set<TeamColor> participants = new HashSet<>();

    void add(TeamColor color) {
        participants.add(color);
    }

    void remove(TeamColor color) {
        participants.remove(color);
    }
}
