package com.bosvel.realBirthdayNotifierBot.service;

public enum SocialNetworks {
    TELEGRAM(0),
    VK(1),
    SYSTEM_MESSAGE(99);

    private final int value;

    SocialNetworks(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
