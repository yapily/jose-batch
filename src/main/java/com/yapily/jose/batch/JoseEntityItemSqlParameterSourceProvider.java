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
import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class JoseEntityItemSqlParameterSourceProvider implements ItemSqlParameterSourceProvider<JoseEntity> {

    private JoseBatchConfigurationProperties configuration;

    public JoseEntityItemSqlParameterSourceProvider(JoseBatchConfigurationProperties configuration) {
        this.configuration = configuration;
    }

    @Override
    public SqlParameterSource createSqlParameterSource(JoseEntity item) {
        return new JoseEntitySqlParameterSource(configuration, item);
    }
}
