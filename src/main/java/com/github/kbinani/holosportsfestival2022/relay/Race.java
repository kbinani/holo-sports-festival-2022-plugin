package com.github.kbinani.holosportsfestival2022.relay;

import com.github.kbinani.holosportsfestival2022.TeamColor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class Race {
    final int numberOfLaps;
    final List<TeamColor> order = new LinkedList<>();
    final Set<TeamColor> participants = new HashSet<>();

    Race(int numberOfLaps) {
        this.numberOfLaps = numberOfLaps;
    }

    int getTeamCount() {
        return participants.size();
    }

    void add(TeamColor color) {
        participants.add(color);
    }

    boolean isActive(TeamColor color) {
        return participants.contains(color);
    }

    void remove(TeamColor color) {
        participants.remove(color);
    }

    void pushOrder(TeamColor teamColor) {
        if (!participants.contains(teamColor)) {
            return;
        }
        order.add(teamColor);
    }

    boolean isAlreadyFinished(TeamColor color) {
        return order.contains(color);
    }
}
