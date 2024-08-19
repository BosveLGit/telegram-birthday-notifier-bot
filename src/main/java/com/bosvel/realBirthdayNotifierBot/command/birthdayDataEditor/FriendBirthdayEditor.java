package com.bosvel.realBirthdayNotifierBot.command.birthdayDataEditor;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.model.dao.AppUserDAO;
import com.bosvel.realBirthdayNotifierBot.model.dao.BirthdayDataDAO;
import com.bosvel.realBirthdayNotifierBot.model.entity.AppUser;
import com.bosvel.realBirthdayNotifierBot.model.entity.BirthdayData;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.*;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

@Slf4j
public class FriendBirthdayEditor implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final BirthdayDataDAO birthdayDataDAO;
    private final AppUserDAO appUserDAO;
    private final String keyCommand = EDIT_BIRTHDAY_FRIEND.getCommandName();

    public FriendBirthdayEditor(SendBotMessageService sendBotMessageService, UserSession userSession,
                                BirthdayDataDAO birthdayDataDAO, AppUserDAO appUserDAO) {

        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
        this.birthdayDataDAO = birthdayDataDAO;
        this.appUserDAO = appUserDAO;
    }

    @Override
    public void sendCommandMessage(Update update, String key) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");
        Integer messageId;
        boolean itEditBirthday = false;

        if(key != null && key.contains("?")) {
            itEditBirthday = getIdFromCallbackAndParseLong(key) > 0;
        }

        String text = String.format("Введите %sдату рождения друга или родственника в одном из форматов:" +
                "\nДень.Месяц.Год (<u>например 10.02.1999</u>) " +
                "\nДень.Месяц (<u>например 09.01</u>):", (itEditBirthday ? "новую " : ""));

        if(update.hasCallbackQuery()) {
            messageId = (Integer) messageIDs.get("messageId");
            if (itEditBirthday) {
                userSession.put(userId, CALLBACK_ID.getKey(), update.getCallbackQuery().getId());
            }
        } else {
            messageId = (Integer) userSession.get(userId, BASE_MESSAGE_ID.getKey(), null);
        }

        sendBotMessageService.execute(createOrEditMessageForCurrentStage(messageId, userId, text,
                createInlineKeyboardGoToBack()));

    }

    @Override
    public void handleResponseMessage(Update update, String key) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");

        userSession.put(userId, BASE_MESSAGE_ID.getKey(), 0);

        Message message = update.getMessage();

        LocalDate parsedDate = parseDate(message.getText().trim());

        if(parsedDate == null) {
            String text = "Введенная дата не соответствует форматам День.Месяц.Год или День.Месяц, введите дату еще раз. " +
                    "\nВот несколько примеров:" +
                    "\n<b>1 января 2000 - 01.01.2000</b>" +
                    "\n<b>20 октября - 20.10</b>";
            SendMessage sendMessage = new SendMessage(userId.toString(), text);
            sendMessage.setParseMode("HTML");
            sendMessage.setReplyMarkup(createInlineKeyboardGoToBack());

            Integer newMessageId = sendBotMessageService.execute(sendMessage);
            userSession.put(userId, BASE_MESSAGE_ID.getKey(), newMessageId);
            return;
        }

        if(key != null && key.contains("?")) {

            long birthdayId = getIdFromCallbackAndParseLong(key);
            if(birthdayId > 0) {

                boolean result = updateBirthdayById(birthdayId, parsedDate);

                String callbackId = (String) userSession.get(userId, CALLBACK_ID.getKey(), null);
                if (callbackId != null) {
                    sendBotMessageService.sendAnswerCallbackWithResultOfOperation(result, callbackId);
                }

                sendBotMessageService.setNewCommandAndExecute(update, userId, RETURN_BACK.getCommandName());
            }

        } else {

            String nameFriend = userSession.get(userId, FRIEND_NAME.getKey(), "<Не указано>").toString();
            boolean result = saveBirthdayData(userId, nameFriend, parsedDate);

            if(result) {
                sendBotMessageService.prepareAndSendMessage(userId, "Дата рождения успешно сохранена!");
            } else {
                sendBotMessageService.prepareAndSendMessage(userId, "Не удалось добавить дату рождения. Попробуйте еще раз!");
            }

            // Не сможем вернуться назад через RETURN_BACK. Возвращаемся поиском и выполнением BASE_MESSAGE в очереди.
            sendBotMessageService.returnToSelectedCommandAndExecute(update, userId, BASE_MESSAGE.getCommandName(), null);

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

    public boolean saveBirthdayData(Long userID, String nameFriend, LocalDate birthdayFriend) {

        AppUser appUser = appUserDAO.findAppUserByTelegramUserId(userID);

        BirthdayData birthdayData = BirthdayData.builder()
                .appUser(appUser)
                .name(nameFriend)
                .birthday(birthdayFriend)
                .dayBirthday(birthdayFriend.getDayOfMonth())
                .monthBirthday(birthdayFriend.getMonthValue())
                .yearBirthday(birthdayFriend.getYear())
                .build();

        birthdayDataDAO.save(birthdayData);

        return birthdayData.getId() != null;

    }

    private boolean updateBirthdayById(long birthdayId, LocalDate newBirthday) {
        return birthdayDataDAO.updateBirthdayById(birthdayId, newBirthday) > 0;
    }

    public static LocalDate parseDate(String birthday) {
        // Регулярные выражения для проверки форматов даты
        String fullDatePattern = "\\d{1,2}[./,-]\\d{1,2}[./,-]\\d{4}";
        String partialDatePattern = "\\d{1,2}[./,-]\\d{1,2}";

        if (birthday.matches(fullDatePattern)) {
            // Убираем все разделители
            String[] parts = birthday.split("[./,-]");
            String day = parts[0];
            String month = parts[1];
            String year = parts[2];
            return parseDateComponents(day, month, year);
        } else if (birthday.matches(partialDatePattern)) {
            // Убираем все разделители
            String[] parts = birthday.split("[./,-]");
            String day = parts[0];
            String month = parts[1];
            String year = "0001";
            return parseDateComponents(day, month, year);
        }

        return null;

    }

    private static LocalDate parseDateComponents(String day, String month, String year) {

        String formattedDay = day.length() == 1 ? "0" + day : day;
        String formattedMonth = month.length() == 1 ? "0" + month : month;
        String date = formattedDay + formattedMonth + year;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        try {
            return LocalDate.parse(date, formatter);
        } catch (DateTimeParseException e) {
            log.error("An error occurred in FriendBirthdayEditor, parseDateComponents() ", e);
            return null;
        }
    }


}
