package com.bosvel.realBirthdayNotifierBot.botConfig;

import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RequiredArgsConstructor
@RestController
public class WebHookController {

    private final SendBotMessageService sendBotMessageService;

    @RequestMapping(value = "/callback/update", method = RequestMethod.POST)
    public ResponseEntity<?> onUpdateReceived(@RequestBody Update update) {
        sendBotMessageService.processUpdate(update);
        return ResponseEntity.ok().build();
    }
}
