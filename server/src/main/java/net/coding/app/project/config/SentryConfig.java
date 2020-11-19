package net.coding.app.project.config;

import com.getsentry.raven.dsn.Dsn;
import com.getsentry.raven.logback.SentryAppender;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.core.spi.FilterReply;

@Configuration
@ConditionalOnProperty(value = "sentry.enable")
public class SentryConfig {

    @Value("${sentry.dsn}")
    private String dsn;

    @Value("${sentry.appname}")
    private String appname;

    @PostConstruct
    void init() {
        if (StringUtils.isEmpty(dsn)) {
            return;
        }
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (factory instanceof LoggerContext) {
            LoggerContext context = (LoggerContext) factory;
            Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
            if (root.getAppender(appname) == null) {
                SentryAppender sentryAppender = new SentryAppender();
                sentryAppender.setName(appname);
                sentryAppender.setDsn(dsn);
                sentryAppender.setContext(root.getLoggerContext());
                LevelFilter filter = new LevelFilter();
                filter.setLevel(Level.ERROR);
                filter.setOnMatch(FilterReply.ACCEPT);
                filter.setOnMismatch(FilterReply.DENY);
                filter.start();
                sentryAppender.addFilter(filter);
                sentryAppender.start();
                root.addAppender(sentryAppender);
            }
        }
    }
}
