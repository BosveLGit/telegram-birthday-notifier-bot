package com.bosvel.realBirthdayNotifierBot.command.birthdayListCommands;

import com.bosvel.realBirthdayNotifierBot.command.commandConfig.Command;
import com.bosvel.realBirthdayNotifierBot.model.dao.BirthdayDataDAO;
import com.bosvel.realBirthdayNotifierBot.model.entity.BirthdayData;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.*;
import static com.bosvel.realBirthdayNotifierBot.service.UserSessionKeys.*;
import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

public class ListBirthdaysCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final UserSession userSession;
    private final BirthdayDataDAO birthdayDataDAO;
    private final String keyCommand = LIST_BIRTHDAYS.getCommandName();
    private final int sizeListBirthdays;

    public ListBirthdaysCommand(SendBotMessageService sendBotMessageService,UserSession userSession,
                                BirthdayDataDAO birthdayDataDAO) {
        this.sendBotMessageService = sendBotMessageService;
        this.userSession = userSession;
        this.birthdayDataDAO = birthdayDataDAO;
        this.sizeListBirthdays = 9;
    }

    @Override
    public void sendCommandMessage(Update update, String prevKey) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        String text = "Меню:\nВозможность скоро тут появится еще какая-то информация";
        Long userId = (Long) messageIDs.get("userId");
        int messageId = (Integer) messageIDs.get("messageId");
        //Integer baseMessageID = (Integer) userSession.get(userId, BASE_MESSAGE_ID.getKey(), null);

        int page = 0;
        userSession.put(userId, LIST_BIRTHDAYS_PAGE.getKey(), page);

        createMessageWithBirthdaysForCurrentPage(userId, messageId, page);

    }

    @Override
    public void handleResponseMessage(Update update, String key) {

        Map<String, Object> messageIDs = CommonBotUtils.getIDsFromMessage(update);
        if(messageIDs == null) {
            return;
        }

        Long userId = (Long) messageIDs.get("userId");

        if(update.hasCallbackQuery()) {

            int messageId = (Integer) messageIDs.get("messageId");

            int page = (int) userSession.get(userId, LIST_BIRTHDAYS_PAGE.getKey(), 0);
            boolean updateListBirthdays = true;

            String paramFromKey = getParameterFromCallback(key, true);

            if(PARAM_PREV_PAGE.getCommandName().equals(paramFromKey)) {
                if(page > 0) {
                    page--;
                } else {
                    updateListBirthdays = false;
                }
            } else if(PARAM_NEXT_PAGE.getCommandName().equals(paramFromKey)) {
                int totalPages = (int) userSession.get(userId, LIST_BIRTHDAYS_TOTAL_PAGE.getKey(), -1);
                if(page < totalPages-1) {
                    page++;
                } else {
                    updateListBirthdays = false;
                }
            }

            if(updateListBirthdays) {
                userSession.put(userId, LIST_BIRTHDAYS_PAGE.getKey(), page);
                createMessageWithBirthdaysForCurrentPage(userId, messageId, page);
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

    private void createMessageWithBirthdaysForCurrentPage(Long userId, int messageId, int page) {

        Pageable pageable = PageRequest.of(page, sizeListBirthdays);
        Page<BirthdayData> birthdayDataPage;

        Integer filterMonth = (Integer) userSession.get(userId, LIST_BIRTHDAYS_FILTER_MONTH.getKey(), null);

        if(filterMonth != null && filterMonth > 0 && filterMonth <= 12) {
            birthdayDataPage = birthdayDataDAO.findAllByUserIdAndByMonthWithPagination(userId, pageable, filterMonth);
        } else {
            birthdayDataPage = birthdayDataDAO.findAllByUserIdWithPagination(userId, pageable);
            filterMonth = null;
        }

        List<BirthdayData> listOfBirthdays = birthdayDataPage.getContent();
        int totalPages = birthdayDataPage.getTotalPages();
        userSession.put(userId, LIST_BIRTHDAYS_TOTAL_PAGE.getKey(), totalPages);

        String pagePosition = String.format("%s/%s", page + 1, totalPages);
        InlineKeyboardMarkup inlineKeyboardMarkup = getInlineKeyboardMarkup(listOfBirthdays, pagePosition, filterMonth);

        String text = "<b>Список сохраненных дней рождений:</b>" +
                "\n\n<i>Для редактирования или удаления нажмите на элемент списка</i>";

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(userId.toString());
        editMessageText.setMessageId(messageId);
        editMessageText.setText(text);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);
        editMessageText.setParseMode("HTML");

        sendBotMessageService.execute(editMessageText);

    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup(List<BirthdayData> listOfBirthdays,
                                                         String pagePosition,
                                                         Integer filterMonth) {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        String textFilterBut = "Фильтр";
        if(filterMonth != null) {
            Locale locale = new Locale("ru");
            Month month = Month.of(filterMonth);
            String monthName = month.getDisplayName(TextStyle.FULL_STANDALONE, locale);
            monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);

            textFilterBut = String.format("\u2755 Фильтр (%s)", monthName);
        }

        keyboard.add(createRowInlineKeyboardWithTwoButton("Вернуться назад", RETURN_BACK.getCommandName(),
                textFilterBut, FILTER_LIST_BIRTHDAYS.getCommandName()));

        DateTimeFormatter formatterFull = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter formatterPartial = DateTimeFormatter.ofPattern("dd.MM");
        String template = "%s - %s";
        LocalDate currentDate = LocalDate.now();

        for(BirthdayData bd : listOfBirthdays) {

            String formattedDate;

            if(bd.getYearBirthday() == 1) {
                formattedDate = bd.getBirthday().format(formatterPartial);
            } else {
                formattedDate = bd.getBirthday().format(formatterFull);
                int age = Period.between(bd.getBirthday(), currentDate).getYears();
                formattedDate = String.format("%s (%s %s)", formattedDate, age, formatYears(age));
            }

            String textButton = String.format(template, bd.getName(), formattedDate);

            keyboard.add(createRowInlineKeyboardWithOneButton(
                    textButton, combineKeyWithId(SHOW_BIRTHDAY.getCommandName(), bd.getId())));

        }

        keyboard.add(createRowInlineKeyboardWithPageTurner(pagePosition, keyCommand));

        markup.setKeyboard(keyboard);
        return markup;

    }

}
