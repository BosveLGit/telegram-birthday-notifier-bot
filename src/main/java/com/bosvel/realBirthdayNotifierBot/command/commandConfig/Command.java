package com.bosvel.realBirthdayNotifierBot.command.commandConfig;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface Command {

    void sendCommandMessage(Update update, String key);
    void handleResponseMessage(Update update, String key);
    boolean shouldProcessResponse();

    String getKeyCommand();

}
