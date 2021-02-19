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


import com.nimbusds.jwt.JWTParser;
import com.yapily.jose.batch.models.JoseEntity;
import com.yapily.jose.database.JoseDatabaseAttributeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.text.ParseException;
import java.util.stream.Collectors;

@Slf4j
public class JoseEntityItemProcessor implements ItemProcessor<JoseEntity, JoseEntity> {

    private JoseDatabaseAttributeConverter attributeEncryptor;

    public JoseEntityItemProcessor(JoseDatabaseAttributeConverter attributeEncryptor) throws Exception {
        this.attributeEncryptor = attributeEncryptor;
    }

    @Override
    public JoseEntity process(final JoseEntity joseEntity) throws Exception {
        log.info("Start processing entity {}", joseEntity.getId());
        joseEntity.setJwtFields(
                joseEntity.getJwtFields()
                        .entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey(),
                                e -> {
                                    if (e.getValue() == null) {
                                        return null;
                                    }
                                    try {
                                        JWTParser.parse(e.getValue());
                                    } catch (Exception exception) {
                                        log.warn("Entity '{}' wasn't encrypted/signed.", joseEntity.getId(), exception);
                                        return attributeEncryptor.convertToDatabaseColumn(e.getValue());
                                    }
                                    return attributeEncryptor.convertToDatabaseColumn(
                                            attributeEncryptor.convertToEntityAttribute(e.getValue()));
                                }
                        ))
        );
        log.info("Done processing entity {}", joseEntity.getId());
        return joseEntity;
    }
}