package com.nxg.mall.analytics.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nxg.mall.common.entity.log.OperationLog;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

public class OperationLogDeserializer implements DeserializationSchema<OperationLog> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OperationLog deserialize(byte[] message) throws IOException {
        return objectMapper.readValue(message, OperationLog.class);
    }

    @Override
    public boolean isEndOfStream(OperationLog nextElement) {
        return false;
    }

    @Override
    public TypeInformation<OperationLog> getProducedType() {
        return TypeInformation.of(OperationLog.class);
    }
}
