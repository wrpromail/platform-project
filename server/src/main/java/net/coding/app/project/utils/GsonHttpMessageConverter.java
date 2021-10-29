package net.coding.app.project.utils;

import com.google.common.collect.Lists;

import net.coding.common.util.Result;
import net.coding.common.util.adapter.SpringfoxJsonToGsonAdapter;
import net.coding.lib.project.exception.ExceptionMessage;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.stereotype.Component;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import one.util.streamex.StreamEx;
import springfox.documentation.spring.web.json.Json;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.UiConfiguration;

@Component
public class GsonHttpMessageConverter extends org.springframework.http.converter.json.GsonHttpMessageConverter {
    private final static List<Type> TYPE = Lists.newArrayList(
            ParameterizedTypeImpl.make(List.class, new Type[]{SwaggerResource.class}, null),
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
    protected void writeInternal(final Object o, final Type type, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        if (ignoreType(type)) {
            super.writeInternal(o, type, outputMessage);
        } else {
            super.writeInternal(Result.success(o), Result.class, outputMessage);
        }
    }
}
