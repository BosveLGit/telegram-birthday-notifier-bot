package com.bosvel.realBirthdayNotifierBot.command.commandConfig;

public enum CommandName {

    START("/start"),
    MAIN_MENU("/menu"),

    BASE_MESSAGE("callback_base_message"),

    SHOW_BIRTHDAY("callback_showBirthday"),
    DELETE_BIRTHDAY("callback_deleteBirthday"),
    LIST_BIRTHDAYS("callback_listBirthdays"),
    LIST_PREV_PAGE("callbackList_previousPage"),
    LIST_NEXT_PAGE("callbackList_nextPage"),
    FILTER_LIST_BIRTHDAYS("callback_filterListBirthdays"),
    EDIT_NAME_FRIEND("callback_editNameFriend"),
    EDIT_BIRTHDAY_FRIEND("callback_editBirthdayFriend"),

    VK_INTEGRATION("callback_vkIntegration"),

    LIST_GROUPS("callback_listGroups"),

    SETTINGS("callback_settings"),
    SET_TIMEZONE("callback_setTimezone"),
    SET_HOUR_NOTIFICATION("callback_setHourNotification"),
    SET_DAYS_BEFORE_NOTIFICATION("callback_setDaysBeforeNotification"),

    ADMINPANEL("callback_adminPanel"),
    ADMIN_STATS("callback_adminStats"),
    ADMIN_SEND_SYS_MESSAGE("callback_adminSendSysMessage"),
    ADMIN_DEL_SYS_MESSAGE("callback_adminDelSysMessage"),

    SUGGEST_IDEA("callback_suggestIdea"),
    HELP("callback_help"),
    NO_PROCESS("callback_noProcessing"),
    RETURN_BACK("callback_goToBack"),

    PARAM_PREV_PAGE("?previousPage"),
    PARAM_NEXT_PAGE("?nextPage"),
    PARAM_DISABLE_INTEGRATION("?disableIntegration"),
    PARAM_DISABLE_FILTER("?disableFilter");

    private final String commandName;

    CommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getCommandName() {
        return commandName;
    }
}
