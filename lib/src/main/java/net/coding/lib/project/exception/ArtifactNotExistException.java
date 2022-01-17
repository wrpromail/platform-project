package net.coding.lib.project.exception;

public class ArtifactNotExistException  extends AppException{
    @Override
    public int getCode() {
        return 2312;
    }

    @Override
    public String getKey() {
        return "artifact_not_exist";
    }
}
