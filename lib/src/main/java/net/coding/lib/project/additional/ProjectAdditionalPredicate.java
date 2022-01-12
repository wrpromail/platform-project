package net.coding.lib.project.additional;

public interface ProjectAdditionalPredicate {
    default boolean withFunction() {
        return false;
    }

    default boolean withAdmin() {
        return false;
    }

    default boolean withMemberCount() {
        return false;
    }

    default boolean withGroup() {
        return false;
    }
}
