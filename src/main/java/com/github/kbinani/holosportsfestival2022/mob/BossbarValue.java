package com.github.kbinani.holosportsfestival2022.mob;

class BossbarValue {
    final int value;
    final int max;
    final String title;

    BossbarValue(int value, int max, String title) {
        this.value = value;
        this.max = max;
        this.title = title;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (!(object instanceof BossbarValue)) {
            return false;
        }
        BossbarValue o = (BossbarValue) object;
        return o.value == value && o.max == max && o.title.equals(title);
    }
}
