package com.bosvel.realBirthdayNotifierBot.command.birthdayListCommands;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.*;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

public class FilterBirthdaysCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final String keyCommand = FILTER_LIST_BIRTHDAYS.getCommandName();

    public FilterBirthdaysCommand(SendBotMessageService sendBotMessageService, UserSession userSession) {

        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;

    }

    @Override
    public void sendCommandMessage(Update update, String key) {

        Map<String, Object> messageIDs = getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");
        Integer messageId;

        if(update.hasCallbackQuery()) {
            messageId = (Integer) messageIDs.get("messageId");
        } else {
            messageId = (Integer) userSession.get(userId, BASE_MESSAGE_ID.getKey(), null);
        }

        Integer filterMonth = (Integer) userSession.get(userId, LIST_BIRTHDAYS_FILTER_MONTH.getKey(), null);
        String text = "<b>Здесь вы можете отфильтровать список сохраненных дней рождений</b>";

        Integer newMessageId = sendBotMessageService.execute(createOrEditMessageForCurrentStage(messageId, userId, text,
                getInlineKeyboardMarkup(filterMonth != null)));

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

            boolean result = false;

            if(PARAM_DISABLE_FILTER.getCommandName().equals(getParameterFromCallback(key, true))) {
                userSession.removeValue(userId, LIST_BIRTHDAYS_FILTER_MONTH.getKey());
                result = true;
            } else {
                int monthId = getIdFromCallbackAndParseInt(key);
                if(monthId > 0) {
                    userSession.put(userId, LIST_BIRTHDAYS_FILTER_MONTH.getKey(), monthId);
                    result = true;
                }
            }

            userSession.removeValue(userId, LIST_BIRTHDAYS_PAGE.getKey());

            CallbackQuery callbackQuery = update.getCallbackQuery();
            sendBotMessageService.sendAnswerCallbackWithResultOfOperation(result, callbackQuery.getId());

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

    private InlineKeyboardMarkup getInlineKeyboardMarkup(Boolean filterApplied) {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(createRowInlineKeyboardWithButtonGoToBack());

        if(filterApplied != null && filterApplied) {
            keyboard.add(createRowInlineKeyboardWithOneButton("\u274C Отключить фильтр", keyCommand + PARAM_DISABLE_FILTER.getCommandName()));
        }

        Locale locale = new Locale("ru");
        int numMonth = 1;

        for (int row = 0; row < 3; row++) {
            List<InlineKeyboardButton> rowButtons = new ArrayList<>();
            for(int monthButton = 0; monthButton < 4; monthButton++) {

                Month month = Month.of(numMonth);
                String monthName = month.getDisplayName(TextStyle.FULL_STANDALONE, locale);
                monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);

                rowButtons.add(createInlineKeyboardButton(monthName, combineKeyWithId(keyCommand, numMonth)));
                numMonth++;

            }

            keyboard.add(rowButtons);

        }

        markup.setKeyboard(keyboard);
        return markup;

    }

}
