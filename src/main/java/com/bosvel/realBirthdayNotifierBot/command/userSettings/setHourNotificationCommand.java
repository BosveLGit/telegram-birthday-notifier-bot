package com.bosvel.realBirthdayNotifierBot.command.userSettings;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.model.dao.AppUserDAO;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

@Slf4j
public class setHourNotificationCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final AppUserDAO appUserDAO;
    private final String keyCommand = SET_HOUR_NOTIFICATION.getCommandName();

    public setHourNotificationCommand(SendBotMessageService sendBotMessageService, UserSession userSession, AppUserDAO appUserDAO) {
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

        String text = "<b>Выберите час для получения уведомлений:</b>";
        Long userId = (Long) messageIDs.get("userId");
        int messageId = (Integer) messageIDs.get("messageId");

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(userId.toString());
        editMessageText.setMessageId(messageId);
        editMessageText.setText(text);
        editMessageText.setReplyMarkup(getInlineKeyboardMarkup());
        editMessageText.setParseMode("HTML");

        sendBotMessageService.execute(editMessageText);

    }

    @Override
    public void handleResponseMessage(Update update, String key) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");

        if (update.hasCallbackQuery()) {

            CallbackQuery callbackQuery = update.getCallbackQuery();
            String callback = callbackQuery.getData();
            String callbackHour = callback.substring(callback.length() - 2);

            Boolean resultUpdateData = null;

            try {
                int selectedHour = Integer.parseInt(callbackHour);

                User telegramUser = callbackQuery.getFrom();
                resultUpdateData = appUserDAO.setHourNotification(selectedHour, telegramUser.getId()) > 0;

            } catch (NumberFormatException e) {
                log.error("An error occurred in setHourNotificationCommand, handleResponseMessage() ", e);
            }

            sendBotMessageService.sendAnswerCallbackWithResultOfOperation(resultUpdateData,
                    callbackQuery.getId(),
                    (resultUpdateData == null) ? "Не удалось установить время!": null);

            sendBotMessageService.setNewCommandAndExecute(update, userId, RETURN_BACK.getCommandName());
        }
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

        int hour = 0;

        for (int row = 0; row < 6; row++) {
            List<InlineKeyboardButton> rowButtons = new ArrayList<>();
            for(int hourButton = 0; hourButton < 4; hourButton++) {
                String position = (hour < 10) ? "0"+hour : String.valueOf(hour);
                rowButtons.add(createInlineKeyboardButton(position+":00", combineKeyWithId(keyCommand, position)));
                hour++;
            }
            keyboard.add(rowButtons);
        }

        markup.setKeyboard(keyboard);
        return markup;

    }

}
