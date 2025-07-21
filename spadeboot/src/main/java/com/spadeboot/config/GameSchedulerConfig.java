// src/main/java/com/pokerapp/config/GameSchedulerConfig.java
package com.spadeboot.config;

import com.spadeboot.session.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class GameSchedulerConfig {

    @Autowired
    private SessionManager sessionManager;

    /**
     * Clean up inactive game sessions every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void cleanupInactiveSessions() {
        sessionManager.cleanupInactiveSessions();
    }
}