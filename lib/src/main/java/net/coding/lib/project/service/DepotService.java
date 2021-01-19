package net.coding.lib.project.service;

import net.coding.lib.project.dao.DepotDao;
import net.coding.lib.project.entity.Depot;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DepotService {

    private final DepotDao depotDao;


    public Depot getById(Integer id) {
        return depotDao.getById(id);
    }
}
