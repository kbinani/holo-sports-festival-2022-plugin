package com.github.kbinani.holosportsfestival2022.boatrace;

import com.github.kbinani.holosportsfestival2022.TeamColor;

class Participation {
    final TeamColor color;
    final Role role;

    Participation(TeamColor color, Role role) {
        this.color = color;
        this.role = role;
    }
}
