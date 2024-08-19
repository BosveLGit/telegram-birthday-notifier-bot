package com.bosvel.realBirthdayNotifierBot.command.birthdayDataEditor;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.model.dao.BirthdayDataDAO;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.*;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.createInlineKeyboardGoToBack;

public class FriendNameEditor implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final BirthdayDataDAO birthdayDataDAO;
    private final String keyCommand = EDIT_NAME_FRIEND.getCommandName();

    public FriendNameEditor(SendBotMessageService sendBotMessageService, UserSession userSession, BirthdayDataDAO birthdayDataDAO) {
        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
        this.birthdayDataDAO = birthdayDataDAO;
    }

    @Override
    public void sendCommandMessage(Update update, String key) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");
        Integer messageId;
        boolean itEditName = false;

        if(key != null && key.contains("?")) {
            itEditName = getIdFromCallbackAndParseLong(key) > 0;
        }

        String text = String.format("Введите %sпредставление вашего друга/ родственника." +
                "\nЭто может быть:" +
                        "\n- юзернейм пользователя начиная с @" +
                        "\n- имя и фамилия" +
                        "\n  или что-то другое",
                (itEditName ? "новое " : ""));

        if(update.hasCallbackQuery()) {
            messageId = (Integer) messageIDs.get("messageId");
            if (itEditName) {
                userSession.put(userId, CALLBACK_ID.getKey(), update.getCallbackQuery().getId());
            }
        } else {
            messageId = (Integer) userSession.get(userId, BASE_MESSAGE_ID.getKey(), null);
        }

        Integer newMessageId = sendBotMessageService.execute(createOrEditMessageForCurrentStage(messageId, userId, text,
                createInlineKeyboardGoToBack()));

        if(!update.hasCallbackQuery() && (messageId == null || messageId == 0)) {
            userSession.put(userId, BASE_MESSAGE_ID.getKey(), newMessageId);
        }

    }

    @Override
    public void handleResponseMessage(Update update, String key) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");

        Message message = update.getMessage();
        String name = message.getText().trim();

        if(name.length() < 3) {
            String text = "Это имя не подходит. Имя должно содержать минимум 3 символа.";
            SendMessage sendMessage = new SendMessage(userId.toString(), text);
            sendMessage.setReplyMarkup(createInlineKeyboardGoToBack());

            Integer newMessageId = sendBotMessageService.execute(sendMessage);
            userSession.put(userId, BASE_MESSAGE_ID.getKey(), newMessageId);
            return;
        }

        userSession.put(userId, BASE_MESSAGE_ID.getKey(), 0);

        if(key != null && key.contains("?")) {

            long birthdayId = getIdFromCallbackAndParseLong(key);
            if(birthdayId > 0) {

                boolean result = updateNameById(birthdayId, name);
                String callbackId = (String) userSession.get(userId, CALLBACK_ID.getKey(), null);
                if(callbackId != null) {
                    sendBotMessageService.sendAnswerCallbackWithResultOfOperation(result, callbackId);
                }

                sendBotMessageService.setNewCommandAndExecute(update, userId, RETURN_BACK.getCommandName());

            }

        } else {

            userSession.put(userId, FRIEND_NAME.getKey(), name);
            sendBotMessageService.setNewCommandAndExecute(update, userId, EDIT_BIRTHDAY_FRIEND.getCommandName());

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

    private boolean updateNameById(long birthdayId, String newName) {
        return birthdayDataDAO.updateNameById(birthdayId, newName) > 0;
    }


}
