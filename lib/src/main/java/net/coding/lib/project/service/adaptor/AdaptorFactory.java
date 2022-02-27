package net.coding.lib.project.service.adaptor;

public interface AdaptorFactory<T> {
    T create(Integer type);
}

