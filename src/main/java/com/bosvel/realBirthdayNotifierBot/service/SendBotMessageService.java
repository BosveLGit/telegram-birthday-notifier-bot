package com.bosvel.realBirthdayNotifierBot.service;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;

public interface SendBotMessageService {

    void processUpdate(Update update);

    Integer execute(BotApiMethod<? extends Serializable> botApiMethod);

    void prepareAndSendMessage(Long chatId, String message);

    void sendAnswerCallbackQuery(String callbackID, String text, boolean showAlert);

    void sendAnswerCallbackWithResultOfOperation(Boolean result, String callbackId);

    void sendAnswerCallbackWithResultOfOperation(Boolean result, String callbackId, String defText);

    void setNewCommandAndExecute(Update update, Long userId, String key);

    void setNewCommandAndExecute(Update update, Long userId, String key, boolean addToCommandQueue);

    void returnToSelectedCommandAndExecute(Update update, Long userId, String key, String defaultKey);

    void updateStatusIntegration(Long userId, String key);

}
