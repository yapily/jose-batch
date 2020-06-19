package com.yapily.jose.batch.models;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoseEntity {

    private Long id;

    private Map<String, String> jwtFields = new HashMap<>();
}