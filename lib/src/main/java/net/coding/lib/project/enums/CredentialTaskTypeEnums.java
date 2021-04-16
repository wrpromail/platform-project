package net.coding.lib.project.enums;

public enum CredentialTaskTypeEnums {

    JobTask(1), CdTask(2);

    private int value;

    public int value() {
        return this.value;
    }

    CredentialTaskTypeEnums(int val) {
        this.value = val;
    }
}
