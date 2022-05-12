package net.coding.app.project.utils;

import com.google.common.collect.Lists;

import net.coding.common.util.Result;
import net.coding.common.util.adapter.SpringfoxJsonToGsonAdapter;
import net.coding.lib.project.exception.ExceptionMessage;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.stereotype.Component;

import java.io.Writer;
import java.lang.reflect.Type;
import java.util.List;

import one.util.streamex.StreamEx;
import springfox.documentation.spring.web.json.Json;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.UiConfiguration;

@Component
public class GsonHttpMessageConverter extends org.springframework.http.converter.json.GsonHttpMessageConverter {
    private final static List<Type> TYPE = Lists.newArrayList(
            new ParameterizedTypeReference<List<SwaggerResource>>() {
            }.getType(),
            Result.class,
            Json.class,
            UiConfiguration.class,
            ExceptionMessage.class
    );

    public GsonHttpMessageConverter() {
        this.setGson(
                GsonBuilderUtils.gsonBuilderWithBase64EncodedByteArrays()
                        .registerTypeAdapter(Json.class, new SpringfoxJsonToGsonAdapter())
                        .create()
        );
    }

    private boolean ignoreType(Type o) {
        return StreamEx.of(TYPE).anyMatch(o::equals);
    }

    @Override
    protected void writeInternal(final Object o, final Type type, final Writer writer) throws Exception {
        if (ignoreType(type)) {
            super.writeInternal(o, type, writer);
        } else {
            super.writeInternal(Result.success(o), Result.class, writer);
        }
    }
}
