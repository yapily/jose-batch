package com.yapily.jose.batch;

import com.yapily.jose.batch.config.JoseBatchConfigurationProperties;
import com.yapily.jose.batch.models.JoseEntity;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JoseEntityRowMapper implements RowMapper<JoseEntity> {

    private final JoseBatchConfigurationProperties config;

    public JoseEntityRowMapper(JoseBatchConfigurationProperties config) {
        this.config = config;
    }

    @Override
    public JoseEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        JoseEntity joseEntity = new JoseEntity();

        joseEntity.setId(rs.getLong(config.getId()));
        for(String field: config.getFields()) {
            joseEntity.getJwtFields().put(field, rs.getString(field));
        }

        return joseEntity;
    }
}
