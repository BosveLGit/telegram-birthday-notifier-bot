package com.bosvel.realBirthdayNotifierBot.command.mainCommand;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.BASE_MESSAGE_ID;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.getIDsFromMessage;

public class MainMenuCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final String keyCommand = MAIN_MENU.getCommandName();

    public MainMenuCommand(SendBotMessageService sendBotMessageService, UserSession userSession) {
        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
    }

    @Override
    public void sendCommandMessage(Update update, String prevKey) {

        Map<String, Object> messageIDs = getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");
        userSession.removeValue(userId, BASE_MESSAGE_ID.getKey());
        sendBotMessageService.setNewCommandAndExecute(update, userId, BASE_MESSAGE.getCommandName());

    }

    @Override
    public void handleResponseMessage(Update update, String key) {}

    @Override
    public boolean shouldProcessResponse() {
        return false;
    }

    @Override
    public String getKeyCommand() {
        return keyCommand;
    }

}
