package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.TeamColor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class Race {
    private final Set<TeamColor> participants = new HashSet<>();
    final List<Goal> order = new LinkedList<>();
    private long startTime;

    Race() {
        memoStartTime();
    }

    void memoStartTime() {
        this.startTime = System.currentTimeMillis();
    }

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
        double seconds = System.currentTimeMillis() - startTime;
        order.add(new Goal(color, seconds / 1000.0));
    }
}