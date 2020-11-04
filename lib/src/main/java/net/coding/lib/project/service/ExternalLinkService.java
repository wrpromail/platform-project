package net.coding.lib.project.service;

import net.coding.lib.project.dao.ExternalLinkDao;
import net.coding.lib.project.entity.ExternalLink;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExternalLinkService {

    @Autowired
    private ExternalLinkDao externalLinkDao;

    public ExternalLink getById(Integer id) {
        return externalLinkDao.getById(id);
    }
}
