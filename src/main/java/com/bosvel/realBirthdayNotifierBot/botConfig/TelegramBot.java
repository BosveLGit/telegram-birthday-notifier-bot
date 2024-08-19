package com.bosvel.realBirthdayNotifierBot.botConfig;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandContainer;
import com.bosvel.realBirthdayNotifierBot.model.dao.AppUserDAO;
import com.bosvel.realBirthdayNotifierBot.model.dao.BirthdayDataDAO;
import com.bosvel.realBirthdayNotifierBot.model.dao.ScheduledMessageStackDAO;
import com.bosvel.realBirthdayNotifierBot.model.dao.VKAuthDataDAO;
import com.bosvel.realBirthdayNotifierBot.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.BASE_MESSAGE_ID;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component("telegramBotMain")
public class TelegramBot extends TelegramWebhookBot {

    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String token;

    @Value("${bot.uri}")
    private String botUri;

    private final CommandContainer commandContainer;
    private Map<Long, Deque<String>> commandQueue = new ConcurrentHashMap<>();
    private final UserSession userSession;

    @Autowired
    private RedisService redisService;

    private final ScheduledMessageStackDAO scheduledMessageStackDAO;
    public final NotificationService notificationService;

    public TelegramBot(AppUserDAO appUserDAO,
                       BirthdayDataDAO birthdayDataDAO,
                       UserSession userSession,
                       ScheduledMessageStackDAO scheduledMessageStackDAO,
                       NotificationService notificationService,
                       VKAuthDataDAO vkAuthDataDAO,
                       CryptoTool cryptoTool,
                       Environment env) {

        this.userSession = userSession;
        this.scheduledMessageStackDAO = scheduledMessageStackDAO;
        this.notificationService = notificationService;

        this.commandContainer = new CommandContainer(
                new SendBotMessageServiceImpl(this),
                appUserDAO,
                birthdayDataDAO,
                userSession,
                vkAuthDataDAO,
                cryptoTool,
                env);

    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return null;
    }

    public void registerBot() {

        try {
            SetWebhook setWebhook = SetWebhook.builder()
                    .url(botUri)
                    .build();
            this.setWebhook(setWebhook);
        } catch (TelegramApiException e) {
            log.error("An error occurred in BotInitializer, init() ", e);
        }

    }

    @Override
    public String getBotPath() {
        return "/update";
    }

    public void onUpdateReceived(Update update) {

        String key;
        Long userId;
        boolean addToCommandQueue = true;

        if (update.hasCallbackQuery()) {

            CallbackQuery callbackQuery = update.getCallbackQuery();
            key = callbackQuery.getData();
            userId = callbackQuery.getFrom().getId();

        } else if (update.hasMessage() && update.getMessage() != null) {
            Message message = update.getMessage();
            String chatType = message.getChat().getType().toLowerCase();

            if(!chatType.equals("private")) {

                String errorText = "Пока что я не умею работать в группах :(" +
                        "\nНо вы можете предложить эту идею разработчику (Помощь -> Предложить идею)";
                executeMessageCommand(createOrEditMessageForCurrentStage(null, message.getChatId(), errorText, new InlineKeyboardMarkup()));

                return;
            }

            key = message.getText();
            userId = message.getFrom().getId();

            if(!message.hasLocation() && (key == null || key.toLowerCase().startsWith("callback_"))) {
                return;
            }

            if(START.getCommandName().equals(key) || MAIN_MENU.getCommandName().equals(key)) {
                commandQueue.put(userId, new ArrayDeque<>());
                addToCommandQueue = false;
            }

        } else {
            return;
        }

        addCommandInQueueAndExecute(update, userId, key, addToCommandQueue);
    }

    public void addCommandInQueueAndExecute(Update update, Long userId, String newCommandName, boolean addToCommandQueue) {

        if(NO_PROCESS.getCommandName().equals(newCommandName)) {
            return;
        }
        boolean findCommand = true;
        boolean itButtonGoToBack = RETURN_BACK.getCommandName().equals(newCommandName);

        // Если нажали на кнопку "Назад" на неглавном сообщении, то ничего не делаем.
        if(itButtonGoToBack && update.hasCallbackQuery()) {
            MaybeInaccessibleMessage maybeInaccessibleMessage = update.getCallbackQuery().getMessage();
            if(maybeInaccessibleMessage != null) {
                Integer baseMessageID = (Integer) userSession.get(userId, BASE_MESSAGE_ID.getKey(), null);
                if (baseMessageID != null
                        && baseMessageID != 0
                        && !maybeInaccessibleMessage.getMessageId().equals(baseMessageID)) {

                    sendCallbackAnswer(update.getCallbackQuery().getId(), "Это не основное сообщение!");
                    return;
                }
            }
        }

        Command commandToExecute;
        String lastCommandName = null;

        boolean isFirstInvocation = false;

        Deque<String> dequeCommand = commandQueue.get(userId);

        if(dequeCommand == null) {
            dequeCommand = new ArrayDeque<>();
        } else {
            lastCommandName = dequeCommand.peekLast();
        }

        if (newCommandName != null && (newCommandName.toLowerCase().startsWith("callback_") || newCommandName.startsWith("/"))) {

            if(itButtonGoToBack) {
                dequeCommand.pollLast();
                newCommandName = (!dequeCommand.isEmpty() ? dequeCommand.peekLast() : BASE_MESSAGE.getCommandName());

                isFirstInvocation = !LIST_BIRTHDAYS.getCommandName().equals(newCommandName);

            } else {

                if(newCommandName.startsWith(DELETE_BIRTHDAY.getCommandName())) {

                    addToCommandQueue = false;
                    // Временно так.
                    findCommand = false;

                } else if (!equalsCommandName(newCommandName, lastCommandName, false)) {

                    // Заменяем текущую команду только если прилетел коллбэк или новая команда.
                    if (addToCommandQueue) {
                        dequeCommand.addLast(newCommandName);
                    }
                    isFirstInvocation = true;

                }
            }

            // Исключительная ситуация.
            if(newCommandName == null) {
                log.warn(String.format("Exceptional situation in the TelegramBot addCommandInQueueAndExecute() " +
                        "userId = %s; " +
                        "lastCommandName = %s; " +
                        "itButtonGoToBack = %s, " +
                        "dequeCommand = %s"
                        , userId, lastCommandName, itButtonGoToBack, dequeCommand.toString()));

                if(update.hasCallbackQuery()) {
                    sendCallbackAnswer(update.getCallbackQuery().getId(), "Возникла ошибка! Вызовите меню заново!");
                }
                return;

            }

            commandToExecute = commandContainer.findCommand(findCommand ? newCommandName : lastCommandName);
            if (commandToExecute == null) {
                log.error("Unknown command in TelegramBot, addCommandInQueueAndExecute() - " + newCommandName);
                return;
            }


            commandQueue.put(userId, dequeCommand);

        } else if(lastCommandName != null) {
            commandToExecute = commandContainer.findCommand(lastCommandName);
            newCommandName = lastCommandName;
        } else {
            return;
        }

        if (!commandQueue.containsKey(userId) || commandToExecute == null) {
            return;
        }

        if(isFirstInvocation) {
            commandToExecute.sendCommandMessage(update, newCommandName);
        } else if(commandToExecute.shouldProcessResponse()) {
            commandToExecute.handleResponseMessage(update, newCommandName);
        }

    }

    public Integer executeMessageCommand(BotApiMethod<? extends Serializable> botApiMethod) {
        try {
            Serializable result = execute(botApiMethod);
            if(result instanceof Message) {
                Message message = (Message) result;
                return message.getMessageId();
            }
        } catch (TelegramApiException e) {
            log.error("An error occurred in TelegramBot, executeMessageCommand() ", e);
        }
        return 0;
    }

    public void returnToSelectedCommandAndExecute(Update update, Long userId, String key, String defaultKey) {

        if(key == null || userId == null) {
            return;
        }

        if(defaultKey == null) {
            defaultKey = BASE_MESSAGE.getCommandName();
        }

        Deque<String> dequeCommand = commandQueue.get(userId);

        if(dequeCommand == null) {
            addCommandInQueueAndExecute(update, userId, defaultKey, true);
        } else {

            while(!dequeCommand.isEmpty()) {
                String current = dequeCommand.peekLast();
                if(key.equals(current)) {
                    dequeCommand.pollLast();
                    addCommandInQueueAndExecute(update, userId, current, true);
                    return;
                } else {
                    dequeCommand.pollLast();
                }
            }
            addCommandInQueueAndExecute(update, userId, defaultKey, true);

        }

    }

    public void updateStatusIntegration(Long userId, String key) {

        if(key == null || userId == null) {
            return;
        }

        Deque<String> dequeCommand = commandQueue.get(userId);

        if(dequeCommand == null || dequeCommand.isEmpty()) {
            return;
        }

        String lastCommandName = dequeCommand.pollLast();

        if (!key.equals(lastCommandName)) {
            return;
        }

        Update update = new Update();
        Message message = new Message();
        message.setFrom(new User(userId, "", false));
        update.setMessage(message);

        addCommandInQueueAndExecute(update, userId, key, false);

    }

    public void sendCallbackAnswer(String callbackID, String text) {
        executeMessageCommand(createAnswerCallbackQuery(callbackID, text, false));
    }

    @Scheduled(cron = "0 10 23 * * *")
    public void refreshStackForNextDay() {
        notificationService.fillBirthdayMessageStack();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void sendMessageFromStackMessages() {
        notificationService.sendMessageFromStackMessages(this);
    }

    public record HTTPServicesResult(Boolean result, Object data, Boolean deleteToken) {}

    public void saveCommandQueueToRedis() {

        if(redisService == null) {
            return;
        }

        try {
            redisService.saveCommandQueue(commandQueue);
        } catch (Exception e) {
            log.error("Error saving data in Redis (TelegramBot): " + e.getMessage(), e);
        }
    }

    public void loadCommandQueueFromRedis() {

        if(redisService == null) {
            return;
        }

        try {
            redisService.loadCommandQueue(commandQueue);
        } catch (Exception e) {
            log.error("Error loading data from Redis (TelegramBot): " + e.getMessage(), e);
        }
    }

}