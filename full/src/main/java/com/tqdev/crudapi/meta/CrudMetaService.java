package com.tqdev.crudapi.meta;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.tqdev.crudapi.core.record.DatabaseRecordsException;
import com.tqdev.crudapi.meta.definition.DatabaseDefinition;
import com.tqdev.crudapi.meta.definition.DatabaseDefinitionException;
import com.tqdev.crudapi.meta.openapi.SimpleOpenApiDefinition;
import com.tqdev.crudapi.meta.reflection.DatabaseReflection;

public interface CrudMetaService {

	// meta

	DatabaseReflection getDatabaseReflection();

	DatabaseDefinition getDatabaseDefinition();

	SimpleOpenApiDefinition getOpenApiDefinition();

	// initialization

	void initialize(String columnsFilename, String openApiFilename) throws JsonParseException, JsonMappingException,
			IOException, DatabaseDefinitionException, DatabaseRecordsException;

}
