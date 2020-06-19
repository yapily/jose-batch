package com.yapily.jose.batch;


import com.yapily.jose.batch.models.JoseEntity;
import com.yapily.jose.database.JoseDatabaseAttributeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.util.stream.Collectors;

@Slf4j
public class JoseEntityItemProcessor implements ItemProcessor<JoseEntity, JoseEntity> {

    private JoseDatabaseAttributeConverter attributeEncryptor;

    public JoseEntityItemProcessor(JoseDatabaseAttributeConverter attributeEncryptor) throws Exception {
        this.attributeEncryptor = attributeEncryptor;
    }

    @Override
    public JoseEntity process(final JoseEntity joseEntity) throws Exception {

        joseEntity.setJwtFields(
                joseEntity.getJwtFields()
                        .entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey(),
                                e -> attributeEncryptor.convertToDatabaseColumn(
                                        attributeEncryptor.convertToEntityAttribute(e.getValue()))
                        ))
        );

        return joseEntity;
    }
}