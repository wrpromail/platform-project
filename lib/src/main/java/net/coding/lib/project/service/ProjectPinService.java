package net.coding.lib.project.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.common.base.dao.BaseDao;
import net.coding.common.util.BeanUtils;
import net.coding.common.util.Pager;
import net.coding.common.util.ResultPage;
import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.dao.ProjectPinDao;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectPin;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.grpc.client.UserGrpcClient;

import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.user.UserProto;

import static net.coding.lib.project.exception.CoreException.ExceptionType.RESOURCE_NO_FOUND;
import static net.coding.lib.project.exception.CoreException.ExceptionType.USER_NOT_EXISTS;


@Service
@Slf4j
@AllArgsConstructor
public class ProjectPinService {
    private final UserGrpcClient userGrpcClient;
    private final ProjectPinDao projectPinDao;
    private final ProjectDTOService projectDTOService;
    private final ProjectDao projectDao;
    private final ProjectMemberService projectMemberService;
    private final ProjectHandCacheService projectHandCacheService;

    public Optional<ProjectPin> getByProjectIdAndUserId(int projectId, int userId) {
        return Optional.ofNullable(projectPinDao.selectOne(ProjectPin.builder()
                .projectId(projectId)
                .userId(userId)
                .deletedAt(BeanUtils.getDefaultDeletedAt())
                .build()));
    }

    /**
     * 获取星标项目 支持关键字查看 支持分页
     */
    public ResultPage<ProjectDTO> getProjectPinPages(
            Integer teamId,
            Integer userId,
            String keyword,
            Pager pager
    ) {
        PageInfo<Project> pageInfo = PageHelper.startPage(pager.getPage(), pager.getPageSize())
                .doSelectPageInfo(() -> projectPinDao.getProjectPinPages(
                        teamId,
                        userId,
                        keyword
                ));
        List<ProjectDTO> projectDTOList = pageInfo.getList().stream()
                .map(projectDTOService::toDetailDTO)
                .peek(p -> {
                    p.setPin(getByProjectIdAndUserId(p.getId(), userId).isPresent());
                    p.setUn_read_activities_count(0);
                })
                .collect(Collectors.toList());
        return new ResultPage<>(
                projectDTOList,
                pager.getPage(),
                pager.getPageSize(),
                pageInfo.getTotal()
        );
    }

    public Boolean pinProject(Integer teamId, Integer userId, Integer projectId) throws CoreException {
        Project project = checkProjectMember(teamId, userId, projectId);

        if (getByProjectIdAndUserId(project.getId(), userId).isPresent()) {
            return true;
        }
        ProjectPin projectPin = ProjectPin.builder()
                .projectId(project.getId())
                .userId(userId)
                .sort(projectPinDao.findMaxSort(userId) + 1)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .deletedAt(Timestamp.valueOf(BaseDao.NOT_DELETED))
                .build();
        projectPinDao.insertSelective(projectPin);
        projectHandCacheService.handleProjectPinCache(projectPin);
        return true;
    }

    public Boolean cancelPinProject(Integer teamId, Integer userId, Integer projectId) throws CoreException {
        Project project = checkProjectMember(teamId, userId, projectId);

        getByProjectIdAndUserId(project.getId(), userId)
                .ifPresent(pin -> {
                    pin.setDeletedAt(new Timestamp(System.currentTimeMillis()));
                    projectPinDao.updateByPrimaryKeySelective(pin);
                    projectHandCacheService.handleProjectPinCache(pin);
                });
        return true;
    }

    public void sortPinProject(Integer teamId, Integer userId, Integer projectId, Integer targetId) throws CoreException {
        Project sourceProject = checkProjectMember(teamId, userId, projectId);
        ProjectPin sourcePin = getByProjectIdAndUserId(sourceProject.getId(), userId).orElse(null);
        if (Objects.isNull(sourcePin)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        // 移到最下面
        if (targetId == 0) {
            sourcePin.setSort(projectPinDao.findMaxSort(userId) + 1);
            projectPinDao.updateByPrimaryKeySelective(sourcePin);
            projectHandCacheService.handleProjectPinCache(sourcePin);
            return;
        }
        Project targetProject = checkProjectMember(teamId, userId, targetId);
        ProjectPin targetPin = getByProjectIdAndUserId(targetProject.getId(), userId).orElse(null);
        if (Objects.isNull(targetPin)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        Integer sourceSort = sourcePin.getSort();
        Integer targetSort = targetPin.getSort();
        if (sourceSort.equals(targetSort)) {
            return;
        }
        projectPinDao.batchUpdateSortBlock(userId, sourceSort, targetSort);
        // 将自己的sort更换成目标sort
        sourcePin.setSort(sourceSort > targetSort ? targetSort : targetSort - 1);
        projectPinDao.updateByPrimaryKeySelective(sourcePin);
        projectHandCacheService.handleProjectPinCache(sourcePin);
    }

    public Project checkProjectMember(Integer teamId, Integer userId, Integer projectId) throws CoreException {
        Project project = projectDao.getProjectByIdAndTeamId(projectId, teamId);
        if (Objects.isNull(project)) {
            throw CoreException.of(RESOURCE_NO_FOUND);
        }
        UserProto.User currentUser = userGrpcClient.getUserById(userId);
        if (Objects.isNull(currentUser)) {
            throw CoreException.of(USER_NOT_EXISTS);
        }
        if (!projectMemberService.isMember(currentUser, project.getId())) {
            throw CoreException.of(CoreException.ExceptionType.PERMISSION_DENIED);
        }
        return project;
    }
}
