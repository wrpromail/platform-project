package net.coding.lib.project.service;

import net.coding.lib.project.dao.ReleaseDao;
import net.coding.lib.project.entity.Release;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReleaseService {

    @Autowired
    private ReleaseDao releaseDao;

    public Release getById(Integer id) {
        return releaseDao.getById(id);
    }
}
