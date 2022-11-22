package com.github.kbinani.holosportsfestival2022;

public interface Announcer {
    ConsoleLogger announcerBroadcast(String format, Object ...args);

    // 本家側とメッセージが同一かどうか確認できてないものを broadcast する
    ConsoleLogger announcerBroadcastUnofficial(String format, Object ...args);
}