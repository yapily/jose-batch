package com.yapily.jose.batch;

import com.yapily.jose.batch.models.JoseEntity;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JoseEntityItemPreparedStatementSetter implements ItemPreparedStatementSetter<JoseEntity> {

    @Override
    public void setValues(JoseEntity item, PreparedStatement preparedStatement) throws SQLException {
        int i;
        for(i = 0; i < item.getJwtFields().size(); i++) {
            preparedStatement.setString(i, item.getJwtFields().get(i));
        }
        preparedStatement.setLong(i++, item.getId());
    }
}
