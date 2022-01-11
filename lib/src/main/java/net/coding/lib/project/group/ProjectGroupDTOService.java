package net.coding.lib.project.group;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectGroupDTOService {

    public ProjectGroupDTO toDTO(ProjectGroup group, Function<ProjectGroup, Long> numberFunc) {
        return Optional.ofNullable(group)
                .map(g ->
                        ProjectGroupDTO.builder()
                                .id(g.getId())
                                .name(g.getName())
                                .type(g.getType())
                                .ownerId(g.getOwnerId())
                                .sort(g.getSort())
                                .projectNum(Optional.ofNullable(numberFunc).map(f -> f.apply(g)).orElse(0L))
                                .build()
                ).orElse(null);
    }

    public List<ProjectGroupDTO> toDTO(List<ProjectGroup> groups, Function<ProjectGroup, Long> numberFunc) {
        return Optional.ofNullable(groups)
                .orElseGet(ArrayList::new)
                .stream()
                .map(g -> this.toDTO(g, numberFunc))
                .collect(Collectors.toList());
    }
}
