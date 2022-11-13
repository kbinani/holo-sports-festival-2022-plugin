package com.github.kbinani.holosportsfestival2022;

interface Announcer {
    void broadcast(String format, Object ...args);

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    void broadcastUnofficial(String format, Object ...args);
}