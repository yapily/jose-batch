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
import com.yapily.jose.database.JoseDatabaseAttributeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableBatchProcessing
@ComponentScan(basePackages = {"com.yapily"})
@Slf4j
public class JoseBatchConfiguration {

    public final JobBuilderFactory jobBuilderFactory;
    public final StepBuilderFactory stepBuilderFactory;
    private final JoseBatchConfigurationProperties config;

    @Autowired
    public JoseBatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, JoseBatchConfigurationProperties config) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.config = config;
    }


    @Bean
    /**
     * The reader provides JoseEntity for processing
     */
    public JdbcPagingItemReader<JoseEntity> reader(DataSource dataSource,
                                                   PostgresPagingQueryProvider pagingQueryProvider) throws Exception {
        JdbcPagingItemReader<JoseEntity> reader = new JdbcPagingItemReader<JoseEntity>();

        reader.setDataSource(dataSource);
        reader.setQueryProvider(pagingQueryProvider);
        reader.setPageSize(config.getPageSize());

        reader.setRowMapper(new JoseEntityRowMapper(config));
        reader.afterPropertiesSet();

        return reader;
    }

    @Bean
    /**
     * The page query provider is used by the reader to fetch the row
     */
    public PostgresPagingQueryProvider pagingQueryProvider() {
        PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
        queryProvider.setSelectClause(config.getId() + ", " + config.getFields().stream().collect(Collectors.joining(", ")));
        queryProvider.setFromClause("from \"" + config.getTable() + "\"");

        Map<String, Order> sortKeys = new HashMap<>(1);
        sortKeys.put(config.getId(), Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);

        return queryProvider;
    }

    @Bean
    /**
     * The processor is the one processing each row. In our case, we are going to unpack the JWT with the old key and re-pack with the new key
     */
    public JoseEntityItemProcessor processor(@Autowired JoseDatabaseAttributeConverter attributeEncryptor) throws Exception {
        return new JoseEntityItemProcessor(attributeEncryptor);
    }

    @Bean
    /**
     * The write is the one saying how to update the row back to the database
     */
    public JdbcBatchItemWriter<JoseEntity> writer(DataSource dataSource) {
        log.debug("Update SQL we will used to update a row: {}", config.getUpdateSQL());
        return new JdbcBatchItemWriterBuilder<JoseEntity>()
                .itemSqlParameterSourceProvider(new JoseEntityItemSqlParameterSourceProvider(config))
                .assertUpdates(true)
                .sql(config.getUpdateSQL())
                .itemPreparedStatementSetter(new JoseEntityItemPreparedStatementSetter())
                .dataSource(dataSource)
                .build();
    }

    @Bean
    /**
     * Job step that bundle all the previous reader, processor and writer.
     * The goal is to repackage every row fields of the database with the new keys
     */
    public Step repackageJWTSFieldsAfterKeyRotationStep(JdbcBatchItemWriter<JoseEntity> writer,
                      DataSource dataSource,
                      PostgresPagingQueryProvider pagingQueryProvider,
                      JdbcPagingItemReader<JoseEntity> reader,
                      JoseEntityItemProcessor processor) {
        return stepBuilderFactory.get("repackageJWTSFieldsAfterKeyRotationStep")
                .<JoseEntity, JoseEntity> chunk(config.getChunkSize())
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    /**
     * Bundle the step into a job
     */
    public Job repackageJWTSFieldsAfterKeyRotationJob(JoseBatchJobCompletionNotificationListener listener, Step repackageJWTSFieldsAfterKeyRotationStep) {
        return jobBuilderFactory.get("repackageJWTSFieldsAfterKeyRotationJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(repackageJWTSFieldsAfterKeyRotationStep)
                .end()
                .build();
    }

}