package net.coding.lib.project.listener;

import com.google.common.eventbus.Subscribe;

import net.coding.common.base.event.ProgramEvent;
import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.entity.ProjectMember;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramEventListener {

    private final ProjectMemberDao projectMemberDao;

    @Subscribe
    public void handle(ProgramEvent event) {
        if (!(ProgramEvent.Function.ARCHIVE.equals(event.getFunction()) ||
                ProgramEvent.Function.UNARCHIVE.equals(event.getFunction()))) {
            return;
        }
        List<ProjectMember> members = projectMemberDao.findListByProjectId(
                event.getProjectId(),
                ProgramEvent.Function.ARCHIVE.equals(event.getFunction()) ?
                        BeanUtils.getDefaultDeletedAt() : BeanUtils.getDefaultArchivedAt());
        if (CollectionUtils.isEmpty(members)) {
            return;
        }
        if (ProgramEvent.Function.ARCHIVE.equals(event.getFunction())) {
            projectMemberDao.batchUpdate(members, BeanUtils.getDefaultArchivedAt());
        }
        if (ProgramEvent.Function.UNARCHIVE.equals(event.getFunction())) {
            projectMemberDao.batchUpdate(members, BeanUtils.getDefaultDeletedAt());
        }
    }
}
