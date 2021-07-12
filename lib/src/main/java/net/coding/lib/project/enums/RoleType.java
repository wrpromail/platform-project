package net.coding.lib.project.enums;

/**
 * @author liwenqiu@coding.net
 */

public enum RoleType {

    // 用户自定义的角色
    UserDefined,

    // 企业所有者
    EnterpriseOwner,
    // 企业管理员
    EnterpriseAdmin,
    // 企业普通成员
    EnterpriseMember,

    // 项目管理员
    ProjectAdmin,
    // 项目成员 -> 新的权限系统里面叫"开发"
    ProjectMember,
    // 项目受限成员 -> 新的权限系统里面叫"测试"
    ProjectGuest,
    // 项目经理
    ProjectManager,
    // 产品经理
    ProductManager,
    // 运维
    ProjectOperation,

    // 项目集负责人
    ProgramOwner,
    // 项目集管理员
    ProgramAdmin,
    // 项目集成员
    ProgramMember,
    // 项目集-项目成员
    ProgramProjectMember

}
