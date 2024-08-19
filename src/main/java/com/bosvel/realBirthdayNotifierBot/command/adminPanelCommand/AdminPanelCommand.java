package com.bosvel.realBirthdayNotifierBot.command.adminPanelCommand;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.BASE_MESSAGE_ID;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.createOrEditMessageForCurrentStage;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.createRowInlineKeyboardWithButtonGoToBack;

public class AdminPanelCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final String keyCommand = ADMINPANEL.getCommandName();

    public AdminPanelCommand(SendBotMessageService sendBotMessageService, UserSession userSession) {
        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
    }

    @Override
    public void sendCommandMessage(Update update, String prevKey) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        String text = "<b>Админ. панель:</b>" +
                "\n\nСкоро...";
        Long userId = (Long) messageIDs.get("userId");

        Integer messageId;
        if(update.hasCallbackQuery()) {
            messageId = (Integer) messageIDs.get("messageId");
        } else {
            messageId = (Integer) userSession.get(userId, BASE_MESSAGE_ID.getKey(), null);
        }

        Integer newMessageId = sendBotMessageService.execute(createOrEditMessageForCurrentStage(messageId, userId, text,
                getInlineKeyboardMarkup()));

        if(!update.hasCallbackQuery() && (messageId == null || messageId == 0)) {
            userSession.put(userId, BASE_MESSAGE_ID.getKey(), newMessageId);
        }

    }

    @Override
    public void handleResponseMessage(Update update, String key) {

    }

    @Override
    public boolean shouldProcessResponse() {
        return true;
    }

    @Override
    public String getKeyCommand() {
        return keyCommand;
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup() {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(createRowInlineKeyboardWithButtonGoToBack());

        // Скоро.

//        keyboard.add(CommonBotUtils.createRowInlineKeyboardWithOneButton(
//                "Статистика", ADMIN_STATS.getCommandName()));
//        keyboard.add(CommonBotUtils.createRowInlineKeyboardWithTwoButton(
//                "Создать рассылку", ADMIN_SEND_SYS_MESSAGE.getCommandName(),
//                "Удалить рассылку", ADMIN_DEL_SYS_MESSAGE.getCommandName()));

        markup.setKeyboard(keyboard);
        return markup;

    }

}
