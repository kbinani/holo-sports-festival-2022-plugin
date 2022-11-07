package com.github.kbinani.holosportsfestival2022.mob;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class Race {
    private final Set<TeamColor> participants = new HashSet<>();
    final List<TeamColor> order = new LinkedList<>();

    void add(TeamColor color) {
        participants.add(color);
    }

    void remove(TeamColor color) {
        participants.remove(color);
    }

    Set<TeamColor> getTeamColors() {
        return participants;
    }

    void pushOrder(TeamColor color) {
        if (!participants.contains(color)) {
            return;
        }
        order.add(color);
    }
}