package net.coding.lib.project.service;

import net.coding.lib.project.dao.RamTransformTeamDao;
import net.coding.lib.project.entity.RamTransformTeam;

import org.springframework.stereotype.Service;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static net.coding.lib.project.entity.RamTransformTeam.Status.Read_ReadWrite;

@Service
@Slf4j
@AllArgsConstructor
public class RamTransformTeamService {
    private final RamTransformTeamDao ramTransformTeamDao;

    public Boolean ramOnline(Integer teamId) {
        return Optional.ofNullable(ramTransformTeamDao.selectOne(RamTransformTeam.builder()
                .teamId(teamId.longValue())
                .status(Read_ReadWrite.name())
                .build())).isPresent();
    }
}
