package com.bosvel.realBirthdayNotifierBot.command.commandConfig;

import com.bosvel.realBirthdayNotifierBot.command.adminPanelCommand.AdminPanelCommand;
import com.bosvel.realBirthdayNotifierBot.command.birthdayDataEditor.FriendBirthdayEditor;
import com.bosvel.realBirthdayNotifierBot.command.birthdayDataEditor.FriendNameEditor;
import com.bosvel.realBirthdayNotifierBot.command.birthdayListCommands.FilterBirthdaysCommand;
import com.bosvel.realBirthdayNotifierBot.command.birthdayListCommands.ListBirthdaysCommand;
import com.bosvel.realBirthdayNotifierBot.command.birthdayListCommands.ShowBirthdayCommand;
import com.bosvel.realBirthdayNotifierBot.command.integrations.VKIntegrationCommand;
import com.bosvel.realBirthdayNotifierBot.command.mainCommand.*;
import com.bosvel.realBirthdayNotifierBot.command.userSettings.setDaysBeforeNotificationCommand;
import com.bosvel.realBirthdayNotifierBot.command.userSettings.setHourNotificationCommand;
import com.bosvel.realBirthdayNotifierBot.command.userSettings.setTimezoneCommand;
import com.bosvel.realBirthdayNotifierBot.model.dao.AppUserDAO;
import com.bosvel.realBirthdayNotifierBot.model.dao.BirthdayDataDAO;
import com.bosvel.realBirthdayNotifierBot.model.dao.VKAuthDataDAO;
import com.bosvel.realBirthdayNotifierBot.service.CryptoTool;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import com.google.common.collect.ImmutableMap;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.deleteParameterInKeyAndReturn;

@Component
public class CommandContainer {

    private final ImmutableMap<String, Command> commandMap;

    public CommandContainer(SendBotMessageService sendBotMessageService,
                            AppUserDAO appUserDAO,
                            BirthdayDataDAO birthdayDataDAO,
                            UserSession userSession,
                            VKAuthDataDAO vkAuthDataDAO,
                            CryptoTool cryptoTool,
                            Environment env) {

        String vkRedirectToAuth = env.getProperty("vk.URLRedirectToServiceAuth");
        String adminsIDsString = env.getProperty("bot.adminsIDs");

        Set<Long> adminsIDs = new HashSet<>();

        if(!adminsIDsString.isBlank()) {
            adminsIDs = Arrays.stream(adminsIDsString.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
        }

        commandMap = ImmutableMap.<String, Command>builder()
                .put(START.getCommandName(), new StartCommand(sendBotMessageService, userSession, appUserDAO))
                .put(MAIN_MENU.getCommandName(), new MainMenuCommand(sendBotMessageService, userSession))
                .put(BASE_MESSAGE.getCommandName(), new BaseCommand(sendBotMessageService, userSession, adminsIDs))
                .put(EDIT_NAME_FRIEND.getCommandName(),
                        new FriendNameEditor(sendBotMessageService, userSession, birthdayDataDAO))
                .put(EDIT_BIRTHDAY_FRIEND.getCommandName(),
                        new FriendBirthdayEditor(sendBotMessageService, userSession, birthdayDataDAO, appUserDAO))
                .put(LIST_BIRTHDAYS.getCommandName(),
                        new ListBirthdaysCommand(sendBotMessageService, userSession, birthdayDataDAO))
                .put(SHOW_BIRTHDAY.getCommandName(),
                        new ShowBirthdayCommand(sendBotMessageService, userSession, birthdayDataDAO))
                .put(SETTINGS.getCommandName(), new SettingsCommand(sendBotMessageService, userSession, appUserDAO))
                .put(SET_HOUR_NOTIFICATION.getCommandName(), new setHourNotificationCommand(sendBotMessageService, userSession, appUserDAO))
                .put(SET_TIMEZONE.getCommandName(), new setTimezoneCommand(sendBotMessageService, userSession, appUserDAO))
                .put(SET_DAYS_BEFORE_NOTIFICATION.getCommandName(), new setDaysBeforeNotificationCommand(sendBotMessageService, userSession, appUserDAO))
                .put(VK_INTEGRATION.getCommandName(), new VKIntegrationCommand(sendBotMessageService, userSession, vkAuthDataDAO, cryptoTool, vkRedirectToAuth))
                .put(FILTER_LIST_BIRTHDAYS.getCommandName(), new FilterBirthdaysCommand(sendBotMessageService, userSession))
                .put(HELP.getCommandName(), new HelpCommand(sendBotMessageService, userSession))
                .put(ADMINPANEL.getCommandName(), new AdminPanelCommand(sendBotMessageService, userSession))
                .build();

    }


    public Command findCommand(String commandIdentifier) {
        if (commandIdentifier.contains("?")) {
            commandIdentifier = deleteParameterInKeyAndReturn(commandIdentifier);
        }
        return commandMap.getOrDefault(commandIdentifier, null);
    }

}
