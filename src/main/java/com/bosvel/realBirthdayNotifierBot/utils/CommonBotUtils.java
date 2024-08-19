package com.bosvel.realBirthdayNotifierBot.utils;


import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;

public class CommonBotUtils {

    public static AnswerCallbackQuery createAnswerCallbackQuery(String callbackID, String text, boolean showAlert) {

        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(callbackID);
        answerCallbackQuery.setText(text);
        answerCallbackQuery.setShowAlert(showAlert);
        return answerCallbackQuery;

    }

    public static String deleteIdInKeyAndReturn(String key) {
        int index = key.indexOf("_id_");
        if (index != -1) {
            return key.substring(0, index);
        }
        return key;
    }

    public static String combineKeyWithId(String key, Object Id) {
        return key+"?id"+Id;
    }

    public static String getIdFromCallback(String callbackKey) {
        int indexId = callbackKey.indexOf("?id");
        if(indexId < 0) {
            return null;
        }
        return callbackKey.substring(indexId + 3);
    }

    public static long getIdFromCallbackAndParseLong(String callbackKey) {

        String idString = getIdFromCallback(callbackKey);

        if(idString == null) {
            return -1;
        }

        try {
            return Long.parseLong(idString);
        } catch (NumberFormatException e) {
            return -1;
        }

    }

    public static int getIdFromCallbackAndParseInt(String callbackKey) {
        String idString = getIdFromCallback(callbackKey);

        if(idString == null) {
            return -1;
        }

        try {
            return Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String getParameterFromCallback(String key, boolean returnQuestionMark) {
        int index = key.indexOf("?");
        if (index != -1) {
            return key.substring(index + (returnQuestionMark ? 0 : 1));
        }
        return null;
    }

    public static String deleteParameterInKeyAndReturn(String key) {
        int index = key.indexOf("?");
        if (index != -1) {
            return key.substring(0, index);
        }
        return key;
    }

    public static boolean equalsCommandName(String commandName1, String commandName2, boolean compareParameters) {

        if(commandName1 == null || commandName2 == null) {
            return false;
        }

        String[] parts1 = commandName1.split("\\?");
        String[] parts2 = commandName2.split("\\?");

        if(!compareParameters) {
            return parts1[0].equals(parts2[0]);
        } else {
            if (parts1.length > 1 && parts2.length > 1) {
                return parts1[1].equals(parts2[1]);
            }
            return parts1.length == parts2.length;
        }

    }

    public static String formatDays(int days) {
        if (days == 0 || days >= 5 && days <= 20) {
            return "дней";
        } else if (days == 1 || days % 10 == 1) {
            return "день";
        } else if (days >= 2 && days <= 4 || days % 10 >= 2 && days % 10 <= 4) {
            return "дня";
        } else {
            return "дней";
        }
    }

    public static String formatYears(int age) {

        int lastDigit = age % 10;
        int lastTwoDigits = age % 100;

        String ageString;
        if (lastTwoDigits >= 11 && lastTwoDigits <= 14) {
            ageString = "лет";
        } else {
            switch (lastDigit) {
                case 1:
                    ageString = "год";
                    break;
                case 2:
                case 3:
                case 4:
                    ageString = "года";
                    break;
                default:
                    ageString = "лет";
                    break;
            }
        }

        return ageString;

    }

    public static Map<String, Object> getIDsFromMessage(Update update) {

        Map<String, Object> IDsMessage = new HashMap<>();
        String key;

        MaybeInaccessibleMessage message = null;
        Long userId;
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            message = callbackQuery.getMessage();
            userId = callbackQuery.getFrom().getId();
            key = callbackQuery.getData();
        } else {
            Message updateMessage = update.getMessage();
            userId = updateMessage.getFrom().getId();
            message = updateMessage;
            key = updateMessage.getText();
        }

        if (message == null) {
            return null;
        }

    //    IDsMessage.put("ChatId", message.getChatId());
        IDsMessage.put("userId", userId);
        IDsMessage.put("messageId", message.getMessageId());
        IDsMessage.put("key", key);

        return IDsMessage;

    }

    public static InlineKeyboardButton createInlineKeyboardButton(String text, String callbackCommand) {
        InlineKeyboardButton newButton = new InlineKeyboardButton();
        newButton.setText(text);
        newButton.setCallbackData(callbackCommand);
        return newButton;
    }

    public static List<InlineKeyboardButton> createRowInlineKeyboardWithOneButton(String text, String callbackCommand) {
        List<InlineKeyboardButton> rowInlineKeyboard = new ArrayList<>();
        rowInlineKeyboard.add(createInlineKeyboardButton(text, callbackCommand));
        return rowInlineKeyboard;
    }

    public static List<InlineKeyboardButton> createRowInlineKeyboardWithOneButtonWithURL(String text, String URL) {
        List<InlineKeyboardButton> rowInlineKeyboard = new ArrayList<>();
        rowInlineKeyboard.add(createInlineKeyboardButtonWithURL(text, URL));
        return rowInlineKeyboard;
    }

    public static InlineKeyboardButton createInlineKeyboardButtonWithURL(String text, String URL) {
        InlineKeyboardButton newButton = new InlineKeyboardButton();
        newButton.setText(text);
        newButton.setUrl(URL);
        return newButton;
    }

    public static List<InlineKeyboardButton> createRowInlineKeyboardWithTwoButton(String textBut1, String callbackBut1,
                                                                                  String textBut2, String callbackBut2) {
        List<InlineKeyboardButton> rowInlineKeyboard = new ArrayList<>();
        rowInlineKeyboard.add(createInlineKeyboardButton(textBut1, callbackBut1));
        rowInlineKeyboard.add(createInlineKeyboardButton(textBut2, callbackBut2));
        return rowInlineKeyboard;
    }

    public static List<InlineKeyboardButton> createRowInlineKeyboardWithPageTurner(String pagePosition, String keyCommand) {
        List<InlineKeyboardButton> rowInlineKeyboard = new ArrayList<>();
        rowInlineKeyboard.add(createInlineKeyboardButton("\u2B05 Назад", keyCommand + PARAM_PREV_PAGE.getCommandName()));
        rowInlineKeyboard.add(createInlineKeyboardButton(pagePosition, NO_PROCESS.getCommandName()));
        rowInlineKeyboard.add(createInlineKeyboardButton("Далее \u27A1", keyCommand + PARAM_NEXT_PAGE.getCommandName()));
        return rowInlineKeyboard;
    }

    public static InlineKeyboardMarkup createInlineKeyboardGoToBack() {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(createRowInlineKeyboardWithButtonGoToBack());

        markup.setKeyboard(keyboard);
        return markup;

    }

    public static List<InlineKeyboardButton> createRowInlineKeyboardWithButtonGoToBack() {
        List<InlineKeyboardButton> rowInlineKeyboard = new ArrayList<>();
        rowInlineKeyboard.add(createInlineKeyboardButton("Назад", RETURN_BACK.getCommandName()));
        return rowInlineKeyboard;
    }

    public static BotApiMethod<? extends Serializable> createOrEditMessageForCurrentStage(
            Integer messageId,
            Long userId,
            String text,
            InlineKeyboardMarkup markup) {

        if(messageId == null || messageId == 0) {

            SendMessage sendMessage = new SendMessage(String.valueOf(userId), text);
            sendMessage.setReplyMarkup(markup);
            sendMessage.setParseMode("HTML");
            return sendMessage;

        } else {

            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(String.valueOf(userId));
            editMessageText.setMessageId(messageId);
            editMessageText.setText(text);
            editMessageText.setReplyMarkup(markup);
            editMessageText.setParseMode("HTML");
            return editMessageText;

        }

    }


}
