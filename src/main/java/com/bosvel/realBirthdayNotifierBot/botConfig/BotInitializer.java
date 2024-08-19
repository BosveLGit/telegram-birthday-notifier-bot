package com.bosvel.realBirthdayNotifierBot.botConfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class BotInitializer {

    @Autowired
    TelegramBot bot;

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {

        bot.registerBot();
        initBotMenu();

    }

    private void initBotMenu() {

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/menu", "Главное меню"));
        bot.executeMessageCommand(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));

    }

}