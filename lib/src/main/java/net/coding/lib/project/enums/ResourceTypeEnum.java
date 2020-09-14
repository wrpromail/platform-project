package net.coding.lib.project.enums;

public enum ResourceTypeEnum {
    Defect("Defect", "缺陷"), // 缺陷
    ExternalLink("ExternalLink", "外部链接"),//外部链接
    Flow("Flow", "工作流"), // 工作流
    Iteration("Iteration", "迭代"), // 迭代
    MergeRequestBean("MergeRequestBean", "MR"), // MR
    Milestone("Milestone", "旧版里程碑"), // 旧版里程碑
    Mission("Mission", "任务"), // 任务
    ProjectFile("ProjectFile", "文件"), // 文件
    Release("Release", "发布"),//发布
    Requirement("Requirement", "需求"), // 需求
    SubTask("SubTask", "子任务"),//子任务
    Task("Task", "旧版任务"), // 旧版任务
    TestingCase("TestingCase", "测试用例"), // 测试用例
    TestingRun("TestingRun", "测试计划"), // 测试计划
    Testing("Testing", "测试"), // 测试
    TestingReport("TestingReport", "测试报告"), // 测试报告
    Wiki("Wiki", "wiki"), // 史诗
    Epic("Epic", "史诗"); // 史诗

    private String type;
    private String name;

    private ResourceTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
