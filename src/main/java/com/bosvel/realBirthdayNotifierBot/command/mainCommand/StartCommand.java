package com.bosvel.realBirthdayNotifierBot.command.mainCommand;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.model.dao.AppUserDAO;
import com.bosvel.realBirthdayNotifierBot.model.entity.AppUser;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.BASE_MESSAGE;
import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.START;

public class StartCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final AppUserDAO appUserDAO;
    private final String keyCommand = START.getCommandName();

    public StartCommand(SendBotMessageService sendBotMessageService, UserSession userSession, AppUserDAO appUserDAO) {
        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
        this.appUserDAO = appUserDAO;
    }

    @Override
    public void sendCommandMessage(Update update, String prevKey) {

        User telegramUser = update.getMessage().getFrom();
        Long userId = telegramUser.getId();

        boolean itNewUser = registerIfNewUser(telegramUser);
        SendMessage sendMessage;

        String text = "Привет! Я — твой личный бот-напоминалка о днях рождения. " +
                "\n\nС моей помощью ты никогда не забудешь поздравить своих друзей и близких! " +
                "Добавляй имя и день рождения любого человека, и я вовремя напомню тебе об этом событии. " +
                "\n<i>Ты можешь настроить за сколько дней и в какое время я буду напоминать о празднике.</i> " +
                "\n\nА еще у меня есть крутая интеграция с ВКонтакте: " +
                "я могу автоматически получать информацию о днях рождения твоих друзей из VK и также напоминать тебе о них. " +
                "\nБудь всегда готов к важным датам с нашим ботом!";

        sendMessage = new SendMessage(userId.toString(), text);
        sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true, null));
        sendMessage.setParseMode("HTML");
        sendBotMessageService.execute(sendMessage);

        if(itNewUser) {
            text = "\n\nДля того, чтоб я смог присылать напоминания, " +
                    "<b>тебе необходимо зайти в Настройки (сообщение ниже) и указать:</b>" +
                    "\n<i> - твой часовой пояс</i>" +
                    "\n<i> - время напоминания</i>";

            sendMessage = new SendMessage(userId.toString(), text);
            sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true, null));
            sendMessage.setParseMode("HTML");
            sendBotMessageService.execute(sendMessage);

        }

        userSession.clear(userId);
        sendBotMessageService.setNewCommandAndExecute(update, userId, BASE_MESSAGE.getCommandName());

    }

    @Override
    public void handleResponseMessage(Update update, String key) {}

    @Override
    public boolean shouldProcessResponse() {
        return false;
    }

    @Override
    public String getKeyCommand() {
        return keyCommand;
    }

    private boolean registerIfNewUser(User telegramUser) {

        AppUser appUser = appUserDAO.findAppUserByTelegramUserId(telegramUser.getId());

        if(appUser == null) {
            appUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .daysBeforeNotification(0)
                    .build();
            appUserDAO.save(appUser);

            return true;

        }

        return false;

    }

}
