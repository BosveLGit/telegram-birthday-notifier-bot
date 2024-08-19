package com.bosvel.realBirthdayNotifierBot.service;

import com.bosvel.realBirthdayNotifierBot.botConfig.TelegramBot;
import com.bosvel.realBirthdayNotifierBot.model.dao.ScheduledMessageStackDAO;
import com.bosvel.realBirthdayNotifierBot.model.dao.VKAuthDataDAO;
import com.bosvel.realBirthdayNotifierBot.model.entity.ScheduledMessageStack;
import com.bosvel.realBirthdayNotifierBot.utils.HTTPService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bosvel.realBirthdayNotifierBot.utils.CommonBotUtils.*;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private ScheduledMessageStackDAO scheduledMessageStackDAO;
    @Autowired
    private VKAuthDataDAO vkAuthDataDAO;
    @Autowired
    public PlatformTransactionManager transactionManager;

    public void fillBirthdayMessageStack() {

        //Тестовая дата.
        //Date basedate = Date.valueOf(LocalDate.of(2024, 6, 20));

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        LocalDate baseLocalDate = LocalDate.now().plusDays(1);
        Date basedate = Date.valueOf(baseLocalDate);

        List<Object[]> VKUsersData = new ArrayList<>();
        List<Object[]> VKAuthDataWithExpireDate = new ArrayList<>();

        try {

            scheduledMessageStackDAO.createTemporaryTableAndCalculate(basedate);
            scheduledMessageStackDAO.insertIntoScheduledMessageStackFromTemp(SocialNetworks.TELEGRAM.getValue());
            VKUsersData = scheduledMessageStackDAO.getVKAccessTokensFromTemp();
            VKAuthDataWithExpireDate = scheduledMessageStackDAO.findVKAuthDataByExpireDate(
                    baseLocalDate.plusDays(1),
                    baseLocalDate.plusDays(3),
                    baseLocalDate.plusDays(7),
                    baseLocalDate);

            transactionManager.commit(status);

        } catch (Exception e) {
            transactionManager.rollback(status);
            log.error("An error occurred in NotificationService, fillBirthdayMessageStack() ", e);
        }

        if (!VKUsersData.isEmpty()) {
            getBirthdaysFromVKAndFillStack(VKUsersData);
        }

        if(!VKAuthDataWithExpireDate.isEmpty()) {
            createNotificationMessageAboutVKTokenRenewalAndFillStack(VKAuthDataWithExpireDate, baseLocalDate);
        }

    }

    public void getBirthdaysFromVKAndFillStack(List<Object[]> arrayVKUsersData) {

        for (Object[] data : arrayVKUsersData) {

            String accessToken = (String) data[0];
            Date birthday = (Date) data[1];

            if((accessToken == null || accessToken.isBlank())
                    || (birthday == null)) {

                continue;
            }

            TelegramBot.HTTPServicesResult queryResult = HTTPService.getListUserBirthdaysFromVK(accessToken, birthday.toLocalDate());

            if(queryResult.result()) {

                Long telegramUserId = (Long) data[2];

                if(queryResult.deleteToken()) {
                    vkAuthDataDAO.deleteVKAuthDataByTelegramUserId(telegramUserId);
                    return;
                }

                @SuppressWarnings("unchecked")
                List<ScheduledMessageStack> arrayBirthdaysVK = (List<ScheduledMessageStack>) queryResult.data();

                if(arrayBirthdaysVK == null || arrayBirthdaysVK.isEmpty()) {
                    continue;
                }

                Timestamp timestampNotification = (Timestamp) data[3];
                Integer daysBeforeNotification =  (Integer) data[4];

                for(ScheduledMessageStack stackElement : arrayBirthdaysVK) {
                    stackElement.setDaysBeforeNotification(daysBeforeNotification);
                    stackElement.setTelegramUserId(telegramUserId);
                    stackElement.setTimestampNotification(timestampNotification.toLocalDateTime());
                    scheduledMessageStackDAO.save(stackElement);
                }

            }

        }
    }

    public void createNotificationMessageAboutVKTokenRenewalAndFillStack(List<Object[]> stackUsers, LocalDate currentDay) {

        for(Object[] data : stackUsers) {

            LocalDate expireDate = ((Date) data[2]).toLocalDate();
            Long telegramUserId = (Long) data[0];

            if(currentDay.equals(expireDate)) {
                //vkAuthDataDAO.deleteVKAuthDataByTelegramUserId(telegramUserId);
                //return;
            }

            // Сообщение дополняется датой при отправке!
            int remainderDays = Period.between(currentDay, expireDate).getDays();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM", new Locale("ru"));
            String representationDate = String.format("Через %s %s (%s)",
                    remainderDays,
                    formatDays(remainderDays),
                    expireDate.format(formatter));

            String textMessage = representationDate + " истечет срок действия токена ВКонтакте. " +
                    "Это означает, что бот не сможет загружать дни рождения из ВК. " +
                    "\nВам необходимо переподключить интеграцию в разделе Настройки -> Интеграция с ВКонтакте.";

            ScheduledMessageStack stackMessage = ScheduledMessageStack.builder()
                    .telegramUserId(telegramUserId)
                    .timestampNotification(((Timestamp) data[1]).toLocalDateTime())
                    .socialNetwork(SocialNetworks.SYSTEM_MESSAGE)
                    .additionalText(textMessage)
                    .daysBeforeNotification(remainderDays)
                    .build();

            scheduledMessageStackDAO.save(stackMessage);

        }

    }

    public void sendMessageFromStackMessages(TelegramBot telegramBot) {

        //Тестовая дата.
        //LocalDateTime sendingTime = LocalDateTime.of(2024, 06, 20, 18, 00, 00);

        LocalDateTime sendingTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        List<ScheduledMessageStack> stackMessages = scheduledMessageStackDAO.getBirthdaysForScheduledSending(sendingTime);

        Map<Long, List<ScheduledMessageStack>> groupedResults = stackMessages.stream()
                .collect(Collectors.groupingBy(ScheduledMessageStack::getTelegramUserId));

        groupedResults.forEach((telegramUserId, userRecords) -> {

            createAndSendGroupedMessage(userRecords, telegramBot, telegramUserId);
            scheduledMessageStackDAO.deleteBirthdaysForScheduledSending(telegramUserId, sendingTime);

        });

    }

    private void createAndSendGroupedMessage(List<ScheduledMessageStack> messages, TelegramBot telegramBot, Long TelegramUserId) {

        if(messages.isEmpty()) {
            return;
        }

        String representationDate;
        int daysBeforeNotification = messages.get(0).getDaysBeforeNotification();
        boolean birthdayToday = daysBeforeNotification == 0;
        LocalDate birthday = messages.get(0).getBirthday();

        StringBuilder messageBuilder = new StringBuilder();

        String systemMessageText = "";
        int sumBirthdays = 0;

        for (ScheduledMessageStack message : messages) {

            if(message.getSocialNetwork() == SocialNetworks.SYSTEM_MESSAGE) {
                systemMessageText = message.getAdditionalText();
                continue;
            }

            sumBirthdays++;

            String representationNameFriend = message.getName();
            String representationAge = "";

            if(message.getSocialNetwork() == SocialNetworks.VK
                    && message.getFriendURL() != null && !message.getFriendURL().isEmpty()) {

                representationNameFriend =
                        String.format("<a href='%s' disable_web_page_preview='true'>%s</a>",
                                message.getFriendURL(),
                                representationNameFriend);

            }

            if(message.getAge() > 0) {
                representationAge = String.format(" - %s %s %s",
                        (birthdayToday) ? "исполняется" : "исполнится",
                        message.getAge(),
                        formatYears(message.getAge()));
            }

            String text = "\n" + representationNameFriend + representationAge;

            messageBuilder.append(text);

        }

        if(birthdayToday) {
            representationDate = "Сегодня";
//        } else if(daysBeforeNotification == 1) {
//            representationDate = "Завтра";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM", new Locale("ru"));
            representationDate = String.format("Через %s %s (%s)",
                    daysBeforeNotification,
                    formatDays(daysBeforeNotification),
                    birthday.format(formatter));
        }

        // В messages.size() могут быть системные сообщения. Поэтому кол-во ДР нужно посчитать вручную.

        String representationCount = (sumBirthdays == 1) ? "вашего друга" : String.format("%s %s", sumBirthdays, "ваших друзей");

        messageBuilder.insert(0,
                String.format("<b>Напоминание</b>\n%s у %s День Рождения:\n", representationDate, representationCount));

        SendMessage sendMessage;

        sendMessage = new SendMessage(TelegramUserId.toString(), messageBuilder.toString());
        sendMessage.setParseMode("HTML");
        sendMessage.disableWebPagePreview();
        telegramBot.executeMessageCommand(sendMessage);

        if(!systemMessageText.isBlank()) {
            sendMessage = new SendMessage(TelegramUserId.toString(), systemMessageText);
            sendMessage.setParseMode("HTML");
            sendMessage.disableWebPagePreview();
            telegramBot.executeMessageCommand(sendMessage);
        }

    }

}