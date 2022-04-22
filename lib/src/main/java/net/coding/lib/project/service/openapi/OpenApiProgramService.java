package net.coding.lib.project.service.openapi;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import net.coding.common.util.BeanUtils;
import net.coding.common.util.ResultPage;
import net.coding.lib.project.dao.ProgramDao;
import net.coding.lib.project.dto.ProgramDTO;
import net.coding.lib.project.enums.PmTypeEnums;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.parameter.ProgramPageQueryParameter;
import net.coding.lib.project.parameter.ProjectMemberPrincipalQueryParameter;
import net.coding.lib.project.service.member.ProjectMemberInspectService;
import net.coding.lib.project.service.project.ProjectAdaptorFactory;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static net.coding.lib.project.enums.ProgramProjectEventEnums.ACTION.ACTION_VIEW;

/**
 * @Description: open api  项目逻辑
 */
@Service
@Slf4j
@AllArgsConstructor
public class OpenApiProgramService {

    private ProgramDao programDao;

    private final ProjectAdaptorFactory projectAdaptorFactory;

    private final ProjectMemberInspectService projectMemberInspectService;

    public ResultPage<ProgramDTO> findProgramPages(ProgramPageQueryParameter parameter)
            throws CoreException {
        //是否有查询全部项目集权限，如无 则查询参与的项目集
        boolean hasEnterprisePermission = projectAdaptorFactory
                .create(PmTypeEnums.PROGRAM.getType())
                .hasEnterprisePermission(
                        parameter.getTeamId(),
                        parameter.getUserId(),
                        PmTypeEnums.PROGRAM.getType(),
                        ACTION_VIEW
                );
        if (!hasEnterprisePermission) {
            Set<Integer> joinedProgramIds = projectMemberInspectService.getJoinedProjectIds(
                    ProjectMemberPrincipalQueryParameter.builder()
                            .teamId(parameter.getTeamId())
                            .userId(parameter.getUserId())
                            .pmType(PmTypeEnums.PROGRAM.getType())
                            .deletedAt(BeanUtils.getDefaultDeletedAt())
                            .build());
            if (CollectionUtils.isEmpty(joinedProgramIds)) {
                List<ProgramDTO> programDTOList = new ArrayList<>();
                return new ResultPage<>(programDTOList, parameter.getPage(), parameter.getPageSize(), programDTOList.size());
            }
            parameter.setJoinedProjectIds(joinedProgramIds);
        }

        PageInfo<ProgramDTO> pageInfo = PageHelper.startPage(parameter.getPage(), parameter.getPageSize())
                .doSelectPageInfo(() -> programDao.selectProgramPages(parameter));
        return new ResultPage<>(pageInfo.getList(), parameter.getPage(), parameter.getPageSize(), pageInfo.getTotal());
    }
}
