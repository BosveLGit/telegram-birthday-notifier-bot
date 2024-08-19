package com.bosvel.realBirthdayNotifierBot;

import com.bosvel.realBirthdayNotifierBot.botConfig.TelegramBot;
import com.bosvel.realBirthdayNotifierBot.service.CryptoTool;
import com.bosvel.realBirthdayNotifierBot.service.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@Slf4j
public class RealBirthdayNotifierBotApplication implements SmartLifecycle {

    @Autowired
    Environment environment;

    @Autowired
    CryptoTool cryptoTool;

    private final TelegramBot telegramBot;
    private final UserSession userSession;
    private boolean running = false;

    @Autowired
    public RealBirthdayNotifierBotApplication(TelegramBot telegramBot, UserSession userSession) {
        this.telegramBot = telegramBot;
        this.userSession = userSession;
    }

    public static void main(String[] args) {
        SpringApplication.run(RealBirthdayNotifierBotApplication.class, args);
    }

    @Override
    public void start() {
        if(telegramBot != null) {
            telegramBot.loadCommandQueueFromRedis();
        }
        if(userSession != null) {
            userSession.loadDataFromRedis();
        }
        running = true;
    }

    @Override
    public void stop() {
        if(telegramBot != null) {
            telegramBot.saveCommandQueueToRedis();
        }
        if(userSession != null) {
            userSession.saveDataToRedis();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
