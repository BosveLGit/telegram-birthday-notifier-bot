package com.bosvel.realBirthdayNotifierBot.command.mainCommand;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.BASE_MESSAGE_ID;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final Set<Long> adminsIDs;
    private final String keyCommand = BASE_MESSAGE.getCommandName();

    public BaseCommand(SendBotMessageService sendBotMessageService, UserSession userSession, Set<Long> adminsIDs) {
        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
        this.adminsIDs = adminsIDs;
    }

    @Override
    public void sendCommandMessage(Update update, String prevKey) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        String text = "<b>Меню:</b>";
        Long userId = (Long) messageIDs.get("userId");

        Integer messageId;

        if(update.hasCallbackQuery()) {
            messageId = (Integer) messageIDs.get("messageId");
        } else {
            messageId = (Integer) userSession.get(userId, BASE_MESSAGE_ID.getKey(), null);
        }

        boolean showButtonAdminPanel = false;
        if(!adminsIDs.isEmpty()) {
            showButtonAdminPanel = adminsIDs.contains(userId);
        }

        Integer newMessageId = sendBotMessageService.execute(createOrEditMessageForCurrentStage(messageId, userId, text,
                getInlineKeyboardMarkup(showButtonAdminPanel)));

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

    private InlineKeyboardMarkup getInlineKeyboardMarkup(boolean showButtonAdminPanel) {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(CommonBotUtils.createRowInlineKeyboardWithOneButton(
                "Добавить день рождения", EDIT_NAME_FRIEND.getCommandName()));
        keyboard.add(CommonBotUtils.createRowInlineKeyboardWithOneButton(
                "Мои дни рождения", LIST_BIRTHDAYS.getCommandName()));
//        keyboard.add(CommonBotUtils.createRowInlineKeyboardWithOneButton(
//                "Мои группы (скоро)", LIST_GROUPS.getCommandName()));
        keyboard.add(CommonBotUtils.createRowInlineKeyboardWithTwoButton(
                "Настройки", SETTINGS.getCommandName(),
                "Помощь", HELP.getCommandName()));
        if(showButtonAdminPanel == true) {
            keyboard.add(CommonBotUtils.createRowInlineKeyboardWithOneButton("Админка", ADMINPANEL.getCommandName()));
        }

        markup.setKeyboard(keyboard);
        return markup;

    }

}
