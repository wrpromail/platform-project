package net.coding.lib.project.service;

import net.coding.lib.project.dao.ExternalLinkDao;
import net.coding.lib.project.entity.ExternalLink;
import net.coding.lib.project.helper.ProjectResourceServiceHelper;
import net.coding.lib.project.utils.DateUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExternalLinkService {

    @Autowired
    private ExternalLinkDao externalLinkDao;

    @Autowired
    private ProjectResourceServiceHelper projectResourceServiceHelper;

    public ExternalLink getById(Integer id) {
        return externalLinkDao.getById(id);
    }

    public ExternalLink add(Integer userId, String title, String link) {
        ExternalLink externalLink = new ExternalLink();
        externalLink.setCreatorId(userId);
        externalLink.setCreatedAt(DateUtil.getCurrentDate());
        externalLink.setLink(link);
        externalLink.setTitle(title);
        externalLinkDao.insert(externalLink);
        return externalLink;
    }

    public int update(ExternalLink externalLink) {
        return externalLinkDao.update(externalLink);
    }
}
