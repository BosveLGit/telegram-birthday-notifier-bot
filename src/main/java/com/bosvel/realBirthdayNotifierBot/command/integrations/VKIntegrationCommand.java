package com.bosvel.realBirthdayNotifierBot.command.integrations;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.model.dao.VKAuthDataDAO;
import com.bosvel.realBirthdayNotifierBot.service.CryptoTool;
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
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.*;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

public class VKIntegrationCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final VKAuthDataDAO vkAuthDataDAO;
    private final CryptoTool cryptoTool;
    private final String vkRedirectToAuth;
    private final String keyCommand = VK_INTEGRATION.getCommandName();

    public VKIntegrationCommand(SendBotMessageService sendBotMessageService,
                                UserSession userSession,
                                VKAuthDataDAO vkAuthDataDAO,
                                CryptoTool cryptoTool,
                                String vkRedirectToAuth) {

        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
        this.vkAuthDataDAO = vkAuthDataDAO;
        this.cryptoTool = cryptoTool;
        this.vkRedirectToAuth = vkRedirectToAuth;
    }

    @Override
    public void sendCommandMessage(Update update, String prevKey) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");
        Integer messageId;

        Map<String, Boolean> detailsConnection = vkAuthDataDAO.getStatusAuthInVK(userId);

        boolean vkConnected = detailsConnection.getOrDefault("active", false) == true;
        boolean canBeReconnected = detailsConnection.getOrDefault("canbereconnected", false) == true;

        String status = "\uD83D" + (vkConnected ? (canBeReconnected ? "\uDFE1 Требуется обновление" : "\uDFE2 Активна" ) : "\uDD34 Не подключена");

        String text =
        "<b>Интеграция с ВКонтакте (ВК)</b>" +
        "\n\nЧтобы получать напоминания о днях рождения друзей из ВКонтакте, " +
        "просто нажми <b>Подключить</b> и авторизируйся в своем аккаунте ВКонтакте." +
        "\n\nПосле авторизации бот сможет получать даты рождения твоих друзей только один год. " +
        "Через год бот напомнит тебе о необходимости переподключить интеграцию, чтобы ты не пропустил важные дни рождения." +
        "\n\nТы можешь отключить интеграцию в любой момент, нажав кнопку <b>Отключить</b>. " +
        "\n\n<b>Мы ценим твою конфиденциальность: бот загружает только дни рождения твоих друзей и не хранит никакие данные из ВК!</b>" +
        "\n\nПодключайся и будь всегда в курсе дней рождения своих друзей!"+
        "\n\nТекущий статус: " + status;

        if(update.hasCallbackQuery()) {
            messageId = (Integer) messageIDs.get("messageId");
        } else {
            messageId = (Integer) userSession.get(userId, BASE_MESSAGE_ID.getKey(), null);
        }

        Integer newMessageId = sendBotMessageService.execute(createOrEditMessageForCurrentStage(messageId, userId, text,
                getKeyboardMarkupForStartActionOrSettings(detailsConnection, userId)));

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

        if(update.hasCallbackQuery()) {

            String paramFromKey = getParameterFromCallback(key, true);

            if (PARAM_DISABLE_INTEGRATION.getCommandName().equals(paramFromKey)) {
                boolean result = vkAuthDataDAO.deleteVKAuthDataByTelegramUserId(userId) > 0;

                String callbackId = (String) userSession.get(userId, CALLBACK_ID.getKey(), null);
                if (callbackId != null) {
                    String resultText = (result ? "Интеграция отключена!" : "Что-то пошло не так :(");
                    sendBotMessageService.sendAnswerCallbackQuery(callbackId, resultText, false);
                }

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

    private InlineKeyboardMarkup getKeyboardMarkupForStartActionOrSettings(Map<String, Boolean> detailsConnection, Long userId) {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(createRowInlineKeyboardWithButtonGoToBack());

        Boolean vkConnected = detailsConnection.getOrDefault("active", false);
        vkConnected = (vkConnected == null) ? false : vkConnected;

        Boolean canBeReconnected = detailsConnection.getOrDefault("canbereconnected", false);
        canBeReconnected = (canBeReconnected == null) ? false : canBeReconnected;

        if(vkConnected == true) {
            keyboard.add(createRowInlineKeyboardWithOneButton("Отключить",
                    keyCommand+PARAM_DISABLE_INTEGRATION.getCommandName()));
        } else {
            keyboard.add(createRowInlineKeyboardWithOneButtonWithURL("Подключить", getURLToConnectVK(userId)));
        }

        if(vkConnected == true && canBeReconnected == true) {
            keyboard.add(createRowInlineKeyboardWithOneButtonWithURL("Обновить токен", getURLToConnectVK(userId)));
        }

        markup.setKeyboard(keyboard);
        return markup;

    }

    private String getURLToConnectVK(Long userId) {
        String hashId = cryptoTool.hashOf(userId);
        return vkRedirectToAuth+"?id="+hashId;
    }

}
