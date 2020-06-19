package com.yapily.jose.batch;


import com.yapily.jose.batch.config.JoseBatchConfigurationProperties;
import com.yapily.jose.batch.models.JoseEntity;
import com.yapily.jose.database.JoseDatabaseAttributeConverter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
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
public class BatchConfiguration {

    public final JobBuilderFactory jobBuilderFactory;
    public final StepBuilderFactory stepBuilderFactory;
    private final JoseBatchConfigurationProperties config;

    @Autowired
    public BatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, JoseBatchConfigurationProperties config) {
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
        return new JdbcBatchItemWriterBuilder<JoseEntity>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<JoseEntity>())
                .assertUpdates(true)
                .sql(config.getUpdateSQL())
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<JoseEntity>())
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
    public Job repackageJWTSFieldsAfterKeyRotationJob(JobCompletionNotificationListener listener, Step repackageJWTSFieldsAfterKeyRotationStep) {
        return jobBuilderFactory.get("repackageJWTSFieldsAfterKeyRotationJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(repackageJWTSFieldsAfterKeyRotationStep)
                .end()
                .build();
    }

}