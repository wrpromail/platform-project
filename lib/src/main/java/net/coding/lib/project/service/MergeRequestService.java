package net.coding.lib.project.service;

import net.coding.lib.project.dao.MergeRequestDao;
import net.coding.lib.project.entity.MergeRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MergeRequestService {

    @Autowired
    private MergeRequestDao mergeRequestDao;

    public MergeRequest getById(Integer id) {
        return mergeRequestDao.getById(id);
    }
}
