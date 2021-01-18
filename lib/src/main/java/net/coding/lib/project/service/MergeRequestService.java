package net.coding.lib.project.service;

import net.coding.lib.project.dao.MergeRequestDao;
import net.coding.lib.project.entity.MergeRequest;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class MergeRequestService {


    private final MergeRequestDao mergeRequestDao;


    public MergeRequest getById(Integer id) {
        return mergeRequestDao.getById(id);
    }
}
