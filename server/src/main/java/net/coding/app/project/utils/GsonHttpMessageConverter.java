package net.coding.app.project.utils;

import net.coding.common.util.adapter.SpringfoxJsonToGsonAdapter;
import net.coding.framework.pojo.RestfulApiResponse;

import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.stereotype.Component;

import java.io.Writer;
import java.lang.reflect.Type;

import springfox.documentation.spring.web.json.Json;

@Component
public class GsonHttpMessageConverter extends org.springframework.http.converter.json.GsonHttpMessageConverter {

    public GsonHttpMessageConverter() {
        this.setGson(
                GsonBuilderUtils.gsonBuilderWithBase64EncodedByteArrays()
                        .registerTypeAdapter(Json.class, new SpringfoxJsonToGsonAdapter())
                        .create()
        );
    }

    @Override
    protected void writeInternal(final Object o, final Type type, final Writer writer) throws Exception {
        if (RestfulApiResponse.class.isAssignableFrom(o.getClass())) {
            super.writeInternal(o, RestfulApiResponse.class, writer);
        } else {
            super.writeInternal(o, type, writer);
        }
    }
}
