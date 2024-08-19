package com.bosvel.realBirthdayNotifierBot.service;


import com.bosvel.realBirthdayNotifierBot.botConfig.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;

import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.createAnswerCallbackQuery;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class SendBotMessageServiceImpl implements SendBotMessageService {

    private final TelegramBot telegramBot;

    @Autowired
    public SendBotMessageServiceImpl(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @Override
    public void processUpdate(Update update) {
        telegramBot.onUpdateReceived(update);
    }

    @Override
    public Integer execute(BotApiMethod<? extends Serializable> botApiMethod) {
        return telegramBot.executeMessageCommand(botApiMethod);
    }

    @Override
    public void prepareAndSendMessage(Long chatId, String message) {

        if (isBlank(message)) return;

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.enableHtml(true);
        sendMessage.setText(message);

        execute(sendMessage);

    }

    @Override
    public void sendAnswerCallbackQuery(String callbackID, String text, boolean showAlert) {
        execute(createAnswerCallbackQuery(callbackID, text, showAlert));
    }

    @Override
    public void sendAnswerCallbackWithResultOfOperation(Boolean result, String callbackId) {
        sendAnswerCallbackWithResultOfOperation(result, callbackId, null);
    }

    @Override
    public void sendAnswerCallbackWithResultOfOperation(Boolean result, String callbackId, String defText) {

        String text = "";

        if(result == null) {
            text = (defText == null || defText.isBlank() ? "Что-то пошло не так :(" : defText);
        } else {
            text = (result ? "Успешно обновлено!" : "Что-то пошло не так :(");
        }

        execute(createAnswerCallbackQuery(callbackId, text, false));
    }


    @Override
    public void setNewCommandAndExecute(Update update, Long userId, String key) {
        telegramBot.addCommandInQueueAndExecute(update, userId, key, true);
    }

    @Override
    public void setNewCommandAndExecute(Update update, Long userId, String key, boolean addToCommandQueue) {
        telegramBot.addCommandInQueueAndExecute(update, userId, key, addToCommandQueue);
    }

    @Override
    public void returnToSelectedCommandAndExecute(Update update, Long userId, String key, String defaultKey) {
        telegramBot.returnToSelectedCommandAndExecute(update, userId, key, defaultKey);
    }

    @Override
    public void updateStatusIntegration(Long userId, String key) {
        telegramBot.updateStatusIntegration(userId, key);
    }


}
