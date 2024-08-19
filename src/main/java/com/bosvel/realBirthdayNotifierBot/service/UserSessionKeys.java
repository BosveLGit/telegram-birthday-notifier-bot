package com.bosvel.realBirthdayNotifierBot.service;

public enum UserSessionKeys {

    BASE_MESSAGE_ID("baseMessageID"),
    FRIEND_NAME("friendName"),
    BIRTHDAY_ID("birthdayID"),
    FRIEND_BIRTHDAY("friendBirthday"),
    LIST_BIRTHDAYS_PAGE("listBirthdays_page"),
    LIST_BIRTHDAYS_TOTAL_PAGE("listBirthdays_totalPages"),
    LIST_BIRTHDAYS_FILTER_MONTH("listBirthdays_filter_month"),
    CALLBACK_ID("callbackID");

    private final String key;

    UserSessionKeys(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
