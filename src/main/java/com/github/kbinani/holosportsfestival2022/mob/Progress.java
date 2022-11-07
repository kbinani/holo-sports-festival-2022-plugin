package com.github.kbinani.holosportsfestival2022.mob;

class Progress {
    final int stage;
    final int step;

    Progress(int stage, int step) {
        this.stage = stage;
        this.step = step;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof Progress)) {
            return false;
        }
        Progress o = (Progress) other;
        return o.stage == stage && o.step == step;
    }

    static Progress Zero() {
        return new Progress(0, 0);
    }
}
