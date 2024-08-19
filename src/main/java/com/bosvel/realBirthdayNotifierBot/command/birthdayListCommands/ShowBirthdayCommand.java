package com.bosvel.realBirthdayNotifierBot.command.birthdayListCommands;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.model.dao.BirthdayDataDAO;
import com.bosvel.realBirthdayNotifierBot.model.entity.BirthdayData;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.BASE_MESSAGE_ID;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

public class ShowBirthdayCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final BirthdayDataDAO birthdayDataDAO;
    private final String keyCommand = SHOW_BIRTHDAY.getCommandName();

    public ShowBirthdayCommand(SendBotMessageService sendBotMessageService, UserSession userSession,
                               BirthdayDataDAO birthdayDataDAO) {

        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
        this.birthdayDataDAO = birthdayDataDAO;

    }

    @Override
    public void sendCommandMessage(Update update, String key) {

        Map<String, Object> messageIDs = getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");
        Integer messageId;
        String callbackId = null;

        if(key.isBlank()) {
            key = (String) messageIDs.get("key");
        }

        if(update.hasCallbackQuery()) {
            messageId = (Integer) messageIDs.get("messageId");
            callbackId = update.getCallbackQuery().getId();
        } else {
            messageId = (Integer) userSession.get(userId, BASE_MESSAGE_ID.getKey(), null);
        }

        long birthdayId = getIdFromCallbackAndParseLong(key);

        if(birthdayId < 0) {
            returnToPreviousIfMissing(update, callbackId, userId);
        }

        BirthdayData birthdayData = birthdayDataDAO.findBirthdayDataById(birthdayId);

        if(birthdayData == null || birthdayData.getBirthday() == null) {
            returnToPreviousIfMissing(update, callbackId, userId);
            return;
        }
        String patternDate = birthdayData.getYearBirthday() == 1 ? "dd.MM" : "dd.MM.yyyy";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(patternDate);
        String formattedDate = birthdayData.getBirthday().format(formatter);
        String text = String.format("<b>%s</b> - %s", birthdayData.getName(), formattedDate);

        Integer newMessageId = sendBotMessageService.execute(createOrEditMessageForCurrentStage(messageId, userId, text,
                getInlineKeyboardMarkup(birthdayData.getId())));

        if(messageId == null || messageId == 0) {
            userSession.put(userId, BASE_MESSAGE_ID.getKey(), newMessageId);
        }
    }

    @Override
    public void handleResponseMessage(Update update, String key) {

        if(update.hasCallbackQuery()) {

            Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
            if(messageIDs == null) {
                return;
            }

            Long userId = (Long) messageIDs.get("userId");

            CallbackQuery callbackQuery = update.getCallbackQuery();

            long birthdayId = getIdFromCallbackAndParseLong(key);

            if(birthdayId < 0) {
                String errorText = "Что-то пошло не так :(";
                sendBotMessageService.sendAnswerCallbackQuery(callbackQuery.getId(), errorText, false);
            }

            if(key.startsWith(DELETE_BIRTHDAY.getCommandName())) {
                birthdayDataDAO.deleteById(birthdayId);
                sendBotMessageService.sendAnswerCallbackQuery(callbackQuery.getId(), "Удалено!", false);
                sendBotMessageService.setNewCommandAndExecute(update, userId, RETURN_BACK.getCommandName());
            }

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

    private InlineKeyboardMarkup getInlineKeyboardMarkup(Long birthdayId) {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(createRowInlineKeyboardWithButtonGoToBack());

        keyboard.add(createRowInlineKeyboardWithOneButton(
                "Изменить имя", combineKeyWithId(EDIT_NAME_FRIEND.getCommandName(), birthdayId)));
        keyboard.add(createRowInlineKeyboardWithOneButton(
                "Изменить дату рождения", combineKeyWithId(EDIT_BIRTHDAY_FRIEND.getCommandName(), birthdayId)));
        keyboard.add(createRowInlineKeyboardWithOneButton(
                "Удалить", combineKeyWithId(DELETE_BIRTHDAY.getCommandName(), birthdayId)));

        markup.setKeyboard(keyboard);
        return markup;

    }

    private void returnToPreviousIfMissing(Update update, String callbackId, Long userId) {

        String errorText = "Не удалось загрузить данные!";
        if(callbackId == null) {
            sendBotMessageService.prepareAndSendMessage(userId, errorText);
        } else {
            sendBotMessageService.sendAnswerCallbackQuery(callbackId, errorText, false);
        }
        sendBotMessageService.setNewCommandAndExecute(update, userId, RETURN_BACK.getCommandName());

    }


}
