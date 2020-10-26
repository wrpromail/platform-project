package net.coding.lib.project.service;

import net.coding.lib.project.dao.ProfanityWordDao;
import net.coding.lib.project.entity.ProfanityWord;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

@Service
public class ProfanityWordService {

    private static final List<ProfanityWord> list = new ArrayList<>();
    private static long lastUpdate = System.currentTimeMillis();
    private static long timeout = MILLISECONDS.convert(1, MINUTES);

    @PostConstruct
    public void init() {
        update();
    }

    @Autowired
    private ProfanityWordDao profanityWordDao;

    public String checkContent(String content) {
        if (StringUtils.isEmpty(content)) {
            return "";
        }
        checkAndUpdate();
        for (ProfanityWord profanityWord : list) {
            if (content.toLowerCase().contains(profanityWord.getWord().toLowerCase()))
                return profanityWord.getWord();
        }
        return "";
    }

    private void update() {
        lastUpdate = System.currentTimeMillis();
        List<ProfanityWord> words = profanityWordDao.findAll();
        synchronized (list) {
            list.clear();
            list.addAll(words);
        }
    }

    private void checkAndUpdate() {
        if (System.currentTimeMillis() - lastUpdate > timeout) {
            update();
        }
    }
}
