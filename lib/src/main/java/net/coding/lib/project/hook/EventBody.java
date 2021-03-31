package net.coding.lib.project.hook;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class EventBody implements Serializable {
    private User member;

    @Builder(toBuilder = true)
    @Data
    public static class User implements Serializable {
        private int id;
        private String login;
        private String avatar_url;
        private String url;
        private String html_url;
        private String name;
        private String name_pinyin;
    }
}
