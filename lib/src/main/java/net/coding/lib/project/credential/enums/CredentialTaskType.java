package net.coding.lib.project.credential.enums;

public enum CredentialTaskType {

    JobTask(1), CdTask(2), QCITask(3);

    private final int value;

    public int value() {
        return this.value;
    }

    CredentialTaskType(int val) {
        this.value = val;
    }
}
