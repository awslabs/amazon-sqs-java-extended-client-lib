/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.sqs.javamessaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;

class JsonDataConverter {

	protected final ObjectMapper objectMapper;

	public JsonDataConverter() {
		this(new ObjectMapper());
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.enableDefaultTyping(DefaultTyping.NON_FINAL);
	}

	public JsonDataConverter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String serializeToJson(Object obj) throws JsonProcessingException {
		ObjectWriter objectWriter = objectMapper.writer();
		return objectWriter.writeValueAsString(obj);
	}

	public <T> T deserializeFromJson(String jsonText, Class<T> objectType) throws Exception {
		return objectMapper.readValue(jsonText, objectType);
	}

}
