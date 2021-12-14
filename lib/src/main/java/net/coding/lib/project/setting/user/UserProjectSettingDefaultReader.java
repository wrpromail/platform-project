package net.coding.lib.project.setting.user;


import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@Component
@AllArgsConstructor
public class UserProjectSettingDefaultReader {

    private final UserProjectSettingDefaultProperties userProjectSettingDefaultProperties;

    public Collection<UserProjectSettingDefault> read() {
        UserProjectSettingDefaultProperties read = userProjectSettingDefaultProperties;
        return Optional.ofNullable(read.getMails())
                .orElse(Collections.emptyList());
    }

    public Optional<UserProjectSettingDefault> read(String code) {
        UserProjectSettingDefaultProperties read = userProjectSettingDefaultProperties;
        return StreamEx.of(read.getMails())
                .filterBy(UserProjectSettingDefault::getCode, code)
                .findFirst();
    }
}
