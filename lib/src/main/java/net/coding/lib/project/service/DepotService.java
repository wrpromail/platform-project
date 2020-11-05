package net.coding.lib.project.service;

import net.coding.lib.project.dao.DepotDao;
import net.coding.lib.project.entity.Depot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DepotService {

    @Autowired
    private DepotDao depotDao;

    public Depot getById(Integer id) {
        return depotDao.getById(id);
    }
}
