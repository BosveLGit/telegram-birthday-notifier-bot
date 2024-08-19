package com.bosvel.realBirthdayNotifierBot.command.mainCommand;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.HELP;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.BASE_MESSAGE_ID;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

public class HelpCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final String keyCommand = HELP.getCommandName();

    public HelpCommand(SendBotMessageService sendBotMessageService, UserSession userSession) {
        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
    }

    @Override
    public void sendCommandMessage(Update update, String prevKey) {

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

        String text = "<b>Ответы на возможные вопросы:</b>" +
                "\n\n<b><i>Как добавить день рождения и просмотреть список дней рождений?</i></b>" +
                "\nНажмите в главном меню \"Добавить день рождения\" и следуйте указаниям. " +
                "Для просмотра сохранненных дней рождений в главном меню нажмите \"Мои дни рождения\"." +
                "\nДни рождения из ВКонтакте не хранятся в боте и загружаются в момент составления списка дней рождений!" +
                "\n\n<b><i>Зачем указывать свою геолокацию?</i></b>" +
                "\nГеолокация необходима для определения вашего местного времени и отправки уведомлений вовремя." +
                "\n\n<b><i>Безопасно ли подключать ВКонтакте?</i></b>" +
                "\nПри подключении ВКонтакте боту предоставляется доступ только к вашим друзьям! " +
                "Бот не хранит никаких ваших данных, только API токен и запрашивает информацию каждый день. " +
                "\nБлагодаря этому вы получаете актуальные дни рожддения " +
                "и в любой момент можете отозвать токен в личном кабинете ВКонтакте." +
                "\n\n<b><i>Что делать, если у меня что-то не работает или я хочу предложить идею?</i></b>" +
                "\nНажмите ниже \"Предложить идею\" и опишите вашу проблему/ предложение.";

        sendBotMessageService.execute(createOrEditMessageForCurrentStage(messageId, userId, text,
                getKeyboardMarkupForStartActionOrSettings()));

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

    private InlineKeyboardMarkup getKeyboardMarkupForStartActionOrSettings() {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(createRowInlineKeyboardWithButtonGoToBack());

        keyboard.add(createRowInlineKeyboardWithOneButtonWithURL("\uD83D\uDCA1 Предложить идею",
                "https://t.me/FeedbackBirthdaysNotifierBot"));

        markup.setKeyboard(keyboard);
        return markup;

    }

}
