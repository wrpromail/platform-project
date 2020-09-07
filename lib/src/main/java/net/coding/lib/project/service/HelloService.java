package net.coding.lib.project.service;

import net.coding.lib.project.dao.HelloDao;
import net.coding.lib.project.entity.Hello;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * created by wang007 on 2020/7/25
 */
@Service
public class HelloService {

    @Resource
    private HelloDao helloDao;

    public Hello getById(Integer id) {
        return helloDao.get(id);
    }

    public void updateById(Hello hello) {
        helloDao.update(hello);
    }

    public void deleteById(Hello hello) {
        helloDao.delete(hello);
    }

    public void addHello(Hello hello) {
        helloDao.insert(hello);
    }


}
