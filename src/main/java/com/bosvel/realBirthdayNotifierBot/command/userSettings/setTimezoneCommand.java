package com.bosvel.realBirthdayNotifierBot.command.userSettings;

import com.bosvel.realBirthdayNotifierBot.botConfig.TelegramBot;
import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.model.dao.AppUserDAO;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils;
import com.bosvel.realBirthdayNotifierBot.utils.HTTPService;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;

import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.BASE_MESSAGE_ID;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.CALLBACK_ID;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

public class setTimezoneCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final AppUserDAO appUserDAO;
    private final String keyCommand = SET_TIMEZONE.getCommandName();

    public setTimezoneCommand(SendBotMessageService sendBotMessageService, UserSession userSession, AppUserDAO appUserDAO) {
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
        int messageId = (Integer) messageIDs.get("messageId");

        String text = "Для того, чтоб я смог отправлять вам напоминания о днях рождения, " +
                "мне необходимо узнать ваш часовой пояс." +
                "\nПоделитесь вашей геолокацией или напишите ваш город <i>(Например, Кишинев)</i>";

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(userId.toString());
        editMessageText.setMessageId(messageId);
        editMessageText.setText(text);
        editMessageText.setReplyMarkup(createInlineKeyboardGoToBack());
        editMessageText.setParseMode("HTML");
        sendBotMessageService.execute(editMessageText);

        if(update.hasCallbackQuery()) {
            userSession.put(userId, CALLBACK_ID.getKey(), update.getCallbackQuery().getId());
        }

    }

    @Override
    public void handleResponseMessage(Update update, String key) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");
        String timezone = null;
        TelegramBot.HTTPServicesResult queryResult;

        userSession.put(userId, BASE_MESSAGE_ID.getKey(), 0);

        Message message = update.getMessage();
        if(message.hasLocation()) {
            Location location = message.getLocation();
            location.getLatitude();
            location.getLongitude();

            queryResult = HTTPService.getTimeZoneByCoordinates(location.getLatitude(), location.getLongitude());

        } else if(message.hasText()) {

            String searchLine = message.getText().trim();

            if (searchLine.length() > 100) {
                searchLine = searchLine.substring(0, 100);
            }
            searchLine = searchLine.replaceAll("[^\\p{L}0-9,\\s]", "");

            queryResult = HTTPService.getTimeZoneBySearchLine(searchLine);

        } else {
            queryResult = new TelegramBot.HTTPServicesResult(false, null, false);
        }

        Boolean resultUpdateData = null;

        if(queryResult.result() && queryResult.data() != null) {
            timezone = (String) queryResult.data();
        }

        if(timezone != null && !timezone.isBlank()) {
            resultUpdateData = appUserDAO.setTimezone(timezone, message.getFrom().getId()) > 0;
        }

        String callbackID = (String) userSession.get(userId, CALLBACK_ID.getKey(), null);
        if(callbackID != null) {
            sendBotMessageService.sendAnswerCallbackWithResultOfOperation(resultUpdateData,
                    callbackID,
                    (resultUpdateData == null) ? "Не удалось определить часовой пояс!": null);
        }

        sendBotMessageService.setNewCommandAndExecute(update, userId, RETURN_BACK.getCommandName());

    }

    @Override
    public boolean shouldProcessResponse() {
        return true;
    }

    @Override
    public String getKeyCommand() {
        return keyCommand;
    }

//    private ReplyKeyboardMarkup getReplyKeyboardMarkup() {
//
//        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
//        replyKeyboardMarkup.setOneTimeKeyboard(true);
//        replyKeyboardMarkup.setIsPersistent();
//        replyKeyboardMarkup.setSelective(true);
//        replyKeyboardMarkup.setResizeKeyboard(true);
//
//        List<KeyboardRow> listRows = new ArrayList<>();
//        KeyboardRow keyboardRow = new KeyboardRow();
//        KeyboardButton keyboardButton = new KeyboardButton("Отправить геолокацию");
//        keyboardButton.setRequestLocation(true);
//        keyboardRow.add(keyboardButton);
//
//        listRows.add(keyboardRow);
//        replyKeyboardMarkup.setKeyboard(listRows);
//
//        return replyKeyboardMarkup;
//
//    }

}
