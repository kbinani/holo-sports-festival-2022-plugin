package com.github.kbinani.holosportsfestival2022.mob;

import com.github.kbinani.holosportsfestival2022.TeamColor;

class Goal {
    final TeamColor color;
    final double seconds;

    Goal(TeamColor color, double seconds) {
        this.color = color;
        this.seconds = seconds;
    }
}
