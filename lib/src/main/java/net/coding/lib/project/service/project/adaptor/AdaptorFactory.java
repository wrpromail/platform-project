package net.coding.lib.project.service.project.adaptor;

public interface AdaptorFactory<T> {
    T create(Integer type);
}

