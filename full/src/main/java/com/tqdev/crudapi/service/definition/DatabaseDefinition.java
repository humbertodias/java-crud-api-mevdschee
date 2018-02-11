package com.tqdev.crudapi.service.definition;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.jooq.AlterTableUsingIndexStep;
import org.jooq.Constraint;
import org.jooq.CreateTableConstraintStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DatabaseDefinition extends HashMap<String, TableDefinition> {

	public static final Logger logger = LoggerFactory.getLogger(DatabaseDefinition.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static String findTablePrefix(DSLContext dsl) {
		Connection connection = dsl.configuration().connectionProvider().acquire();
		String catalog = null, schema = null;
		try {
			catalog = connection.getCatalog();
			schema = connection.getSchema();
		} catch (SQLException e) {
			// error on table prefix
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				// ignore
			}
		}
		String prefix = "";
		if (catalog != null) {
			prefix += "\"" + catalog + "\".";
		}
		if (schema != null) {
			prefix += "\"" + schema + "\".";
		}
		return prefix;
	}

	public void create(DSLContext dsl) throws DatabaseDefinitionException {
		ArrayList<String> created = new ArrayList<>();
		for (String tableName : keySet()) {
			TableDefinition table = get(tableName);
			ArrayList<Field<?>> fields = table.getFields(dsl);
			ArrayList<Constraint> constraints = table.getPkConstraints(dsl, tableName);
			CreateTableConstraintStep query = dsl.createTable(DSL.name(tableName)).columns(fields)
					.constraints(constraints);
			logger.info("Executing SQL: " + query.getSQL());
			int result = query.execute();
			if (result > 0) {
				created.add(tableName);
			}
		}
		for (String tableName : created) {
			TableDefinition table = get(tableName);
			for (Constraint constraint : table.getFkConstraints(dsl, tableName, this)) {
				AlterTableUsingIndexStep query = dsl.alterTable(DSL.name(tableName)).add(constraint);
				logger.info("Executing SQL: " + query.getSQL());
				query.execute();
			}
		}
	}

	public static DatabaseDefinition fromValue(DSLContext dsl) {
		DatabaseDefinition definition = new DatabaseDefinition();
		String prefix = findTablePrefix(dsl);
		for (Table<?> table : dsl.meta().getTables()) {
			if (!(table.toString().startsWith(prefix))) {
				// table not in current catalog or schema
				continue;
			}
			definition.put(table.getName(), TableDefinition.fromValue(table));
		}
		return definition;
	}

	public static DatabaseDefinition fromFile(String filename)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ClassPathResource resource = new ClassPathResource(filename);
		DatabaseDefinition result;
		try {
			result = mapper.readValue(resource.getInputStream(), DatabaseDefinition.class);
		} catch (FileNotFoundException e) {
			result = new DatabaseDefinition();
		}
		return result;
	}

}
