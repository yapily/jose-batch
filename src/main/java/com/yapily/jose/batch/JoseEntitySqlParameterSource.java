/**
 * Copyright 2020 Yapily
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.yapily.jose.batch;

import com.yapily.jose.batch.config.JoseBatchConfigurationProperties;
import com.yapily.jose.batch.models.JoseEntity;
import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JoseEntitySqlParameterSource extends AbstractSqlParameterSource {

    private JoseBatchConfigurationProperties configuration;
    private JoseEntity joseEntity;
    private String[] parameterNames;

    public JoseEntitySqlParameterSource(JoseBatchConfigurationProperties configuration, JoseEntity joseEntity) {
        this.configuration = configuration;
        this.joseEntity = joseEntity;
        this.parameterNames = Stream.concat(
                joseEntity.getJwtFields().keySet().stream(),
                Stream.of(configuration.getId())
        ).collect(Collectors.toList()).toArray(new String[0]);
    }

    @Override
    public boolean hasValue(String paramName) {
        return configuration.getId().equals(paramName) || configuration.getFields().contains(paramName);
    }

    @Override
    public Object getValue(String paramName) throws IllegalArgumentException {
        if (configuration.getId().equals(paramName)) {
            return joseEntity.getId();
        }
        if (configuration.getFields().contains(paramName)) {
            return joseEntity.getJwtFields().get(paramName);
        }
        return null;
    }

    @Override
    public String[] getParameterNames() {
        return parameterNames;
    }
}
