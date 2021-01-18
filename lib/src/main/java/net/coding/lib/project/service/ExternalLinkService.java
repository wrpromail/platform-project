package net.coding.lib.project.service;

import net.coding.lib.project.dao.ExternalLinkDao;
import net.coding.lib.project.entity.ExternalLink;
import net.coding.lib.project.utils.DateUtil;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class ExternalLinkService {

    private final ExternalLinkDao externalLinkDao;


    public ExternalLink getById(Integer id) {
        return externalLinkDao.getById(id);
    }

    public ExternalLink add(Integer userId, String title, String link) {

        ExternalLink externalLink = ExternalLink.builder()
                .creatorId(userId)
                .createdAt(DateUtil.getCurrentDate())
                .updatedAt(DateUtil.getCurrentDate())
                .link(link).title(title)
                .projectId(0)
                .build();
        if (externalLinkDao.insert(externalLink) > 0) {

            return externalLink;
        } else {
            return null;
        }

    }

    public int update(ExternalLink externalLink) {
        return externalLinkDao.update(externalLink);
    }

}
