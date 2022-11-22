package com.github.kbinani.holosportsfestival2022.daruma;

import com.github.kbinani.holosportsfestival2022.Announcer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

class Race {
    private final List<Goal> order = new LinkedList<>();
    private final Set<UUID> finished = new HashSet<>();
    private final Set<UUID> running = new HashSet<>();
    // ゴール判定の結果順位が変わる可能性がある.
    // なので本人に通知した後に順位が変動した場合再アナウンスできるよう, アナウンスした順位を覚えておく.
    private final Map<UUID, Integer> announcedOrder = new HashMap<>();
    private static final String kLogPrefix = "[だるまさんがころんだ]";

    void goal(Player player, double tick) {
        UUID uuid = player.getUniqueId();
        if (finished.contains(uuid)) {
            return;
        }
        order.add(new Goal(player, tick));
        finished.add(uuid);
        running.remove(uuid);
    }

    void participate(Player player) {
        running.add(player.getUniqueId());
    }

    void withdraw(Player player) {
        running.remove(player.getUniqueId());
    }

    int getRunningPlayerCount() {
        return running.size();
    }

    boolean isRunning(Player player) {
        return running.contains(player.getUniqueId());
    }

    void announceOrder(Announcer announcer, Player player) {
        order.sort(Comparator.comparingDouble(a -> a.tick));
        enumerateInOrder((order, p) -> {
            if (p.getUniqueId().equals(player.getUniqueId())) {
                announcer.announcerBroadcast("%sが %d位 でクリア！", player.getName(), order).log(kLogPrefix);
                announcedOrder.put(player.getUniqueId(), order);
            } else {
                Integer prev = announcedOrder.get(p.getUniqueId());
                if (prev != null && order != prev) {
                    announcer.announcerBroadcastUnofficial("判定の結果 %s の順位が %d位 から %d位 に変わりました", p.getName(), prev, order).log(kLogPrefix);
                    announcedOrder.put(p.getUniqueId(), order);
                }
            }
        });
    }

    void announceOrders(Announcer announcer) {
        if (running.size() == 0) {
            announcer.announcerBroadcast("");
            announcer.announcerBroadcast("-----------------------");
            announcer.announcerBroadcast("[試合終了]").log(kLogPrefix);
            AtomicBoolean anyoneFinished = new AtomicBoolean(false);
            enumerateInOrder((order, player) -> {
                announcer.announcerBroadcast("%d位 : %s", order, player.getName()).log(kLogPrefix);
                anyoneFinished.set(true);
            });
            if (!anyoneFinished.get()) {
                announcer.announcerBroadcastUnofficial("全員失格").log(kLogPrefix);
            }
            announcer.announcerBroadcast("-----------------------");
            announcer.announcerBroadcast("");
        }
    }

    private void enumerateInOrder(BiConsumer<Integer, Player> action) {
        double prevTick = -1;
        int currentOrder = 0;
        for (Goal goal : order) {
            if (prevTick < goal.tick) {
                currentOrder++;
            }
            prevTick = goal.tick;
            action.accept(currentOrder, goal.player);
        }
    }
}
