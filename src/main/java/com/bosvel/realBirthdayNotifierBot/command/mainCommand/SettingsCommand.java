package com.bosvel.realBirthdayNotifierBot.command.mainCommand;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.model.dao.AppUserDAO;
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
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

public class SettingsCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final AppUserDAO appUserDAO;
    private final String keyCommand = SETTINGS.getCommandName();

    public SettingsCommand(SendBotMessageService sendBotMessageService, UserSession userSession, AppUserDAO appUserDAO) {
        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
        this.appUserDAO = appUserDAO;
    }

    @Override
    public void sendCommandMessage(Update update, String prevKey) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");
        Integer messageId;

        Map<String, Object> settingFields = appUserDAO.getSettingFields(userId);

        String text = "<b>Настройки:</b>";

        if(update.hasCallbackQuery()) {
            messageId = (Integer) messageIDs.get("messageId");
        } else {
            messageId = (Integer) userSession.get(userId, BASE_MESSAGE_ID.getKey(), null);
        }

        Integer newMessageId = sendBotMessageService.execute(createOrEditMessageForCurrentStage(messageId, userId, text,
                getKeyboardMarkupForStartActionOrSettings(settingFields)));

        if(!update.hasCallbackQuery() && (messageId == null || messageId == 0)) {
            userSession.put(userId, BASE_MESSAGE_ID.getKey(), newMessageId);
        }

    }

    @Override
    public void handleResponseMessage(Update update, String key) {

    }

    @Override
    public boolean shouldProcessResponse() {
        return false;
    }

    @Override
    public String getKeyCommand() {
        return keyCommand;
    }

    private InlineKeyboardMarkup getKeyboardMarkupForStartActionOrSettings(Map<String, Object> settingFields) {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(createRowInlineKeyboardWithButtonGoToBack());

        String hourNotification = "<не указано>";
        Object hourNotificationObj = settingFields.getOrDefault("hourNotification", null);
        if(hourNotificationObj != null) {
            int hour = (int) hourNotificationObj;
            hourNotification = (hour < 10 ? "0"+hour : String.valueOf(hour)) + ":00";
        }

        String daysBeforeNotification = "<не указан>";
        Object daysBeforeNotificationObj = settingFields.getOrDefault("daysBeforeNotification", null);
        if(daysBeforeNotificationObj != null) {
            int hour = (int) daysBeforeNotificationObj;
            if(hour == 0) {
                daysBeforeNotification = "в день рождения";
            } else {
                daysBeforeNotification = String.format("за %s %s", hour, CommonBotUtils.formatDays(hour));
            }
        }

        Object timezoneObj = settingFields.getOrDefault("timezone", null);
        String timezone = (timezoneObj != null) ? timezoneObj.toString() : "<не указан>";

        keyboard.add(createRowInlineKeyboardWithOneButton(
                "Часовой пояс - " + timezone, SET_TIMEZONE.getCommandName()));
        keyboard.add(createRowInlineKeyboardWithOneButton(
                "Время напоминания - " + hourNotification, SET_HOUR_NOTIFICATION.getCommandName()));
        keyboard.add(createRowInlineKeyboardWithOneButton(
                "Срок напоминания - " + daysBeforeNotification, SET_DAYS_BEFORE_NOTIFICATION.getCommandName()));
        keyboard.add(createRowInlineKeyboardWithOneButton(
                "Интеграция с ВКонтакте", VK_INTEGRATION.getCommandName()));

        markup.setKeyboard(keyboard);
        return markup;

    }


}
