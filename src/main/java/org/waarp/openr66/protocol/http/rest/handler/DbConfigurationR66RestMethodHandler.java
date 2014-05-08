/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.protocol.http.rest.handler;

import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.data.DbValue;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.exception.HttpNotFoundRequestException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.openr66.database.data.DbConfiguration;
import org.waarp.openr66.database.data.DbConfiguration.Columns;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * DBConfiguration Rest Handler
 * @author "Frederic Bregier"
 *
 */
public class DbConfigurationR66RestMethodHandler extends DataModelRestMethodHandler<DbConfiguration> {
	public static enum FILTER_ARGS {
		HOSTID("host name"),
		BANDWIDTH("<0 for no filter, =0 for no bandwidth, >0 for a limit greater than value");
		
		public String type;
		FILTER_ARGS(String type) {
			this.type = type;
		}
	}
	/**
	 * @param name
	 * @param allowDelete
	 */
	public DbConfigurationR66RestMethodHandler(String name, boolean allowDelete) {
		super(name, allowDelete);
	}

	protected DbConfiguration getItem(HttpRestHandler handler, RestArgument arguments,
			RestArgument result, Object body) throws HttpIncorrectRequestException,
			HttpInvalidAuthenticationException, HttpNotFoundRequestException {
		ObjectNode arg = arguments.getUriArgs().deepCopy();
		arg.putAll(arguments.getBody());
		try {
			JsonNode node = RestArgument.getId(arg);
			String id;
			if (node.isMissingNode()) {
				// shall not be but continue however
				id = arg.path(DbConfiguration.Columns.HOSTID.name()).asText();
			} else {
				id = node.asText();
			}
			return new DbConfiguration(handler.getDbSession(), id);
		} catch (WaarpDatabaseException e) {
			throw new HttpNotFoundRequestException("Issue while reading from database "+arg, e);
		}
	}

	@Override
	protected DbConfiguration createItem(HttpRestHandler handler, RestArgument arguments,
			RestArgument result, Object body) throws HttpIncorrectRequestException,
			HttpInvalidAuthenticationException {
		ObjectNode arg = arguments.getUriArgs().deepCopy();
		arg.putAll(arguments.getBody());
		try {
			return new DbConfiguration(handler.getDbSession(), arg);
		} catch (WaarpDatabaseException e) {
			throw new HttpIncorrectRequestException("Issue while inserting into database", e);
		}
	}

	@Override
	protected DbPreparedStatement getPreparedStatement(HttpRestHandler handler,
			RestArgument arguments, RestArgument result, Object body)
			throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
		ObjectNode arg = arguments.getUriArgs().deepCopy();
		arg.putAll(arguments.getBody());
		String host = arg.path(FILTER_ARGS.HOSTID.name()).asText();
		if (host == null || host.isEmpty()) {
			host = null;
		}
		int limit = arg.path(FILTER_ARGS.BANDWIDTH.name()).asInt(-1);
		try {
			return DbConfiguration.getFilterPrepareStament(handler.getDbSession(), 
					host, limit);
		} catch (WaarpDatabaseNoConnectionException e) {
			throw new HttpIncorrectRequestException("Issue while reading from database", e);
		} catch (WaarpDatabaseSqlException e) {
			throw new HttpIncorrectRequestException("Issue while reading from database", e);
		}
	}

	@Override
	protected DbConfiguration getItemPreparedStatement(DbPreparedStatement statement)
			throws HttpIncorrectRequestException, HttpNotFoundRequestException {
		try {
			return DbConfiguration.getFromStatement(statement);
		} catch (WaarpDatabaseNoConnectionException e) {
			throw new HttpIncorrectRequestException("Issue while selecting from database", e);
		} catch (WaarpDatabaseSqlException e) {
			throw new HttpNotFoundRequestException("Issue while selecting from database", e);
		}
	}

	@Override
	protected ArrayNode getDetailedAllow() {
		ArrayNode node = JsonHandler.createArrayNode();
		
		ObjectNode node2;
		node2 = RestArgument.fillDetailedAllow(METHOD.GET, this.path+"/id", COMMAND_TYPE.GET.name(), 
				JsonHandler.createObjectNode().put(DbConfiguration.Columns.HOSTID.name(), "HostId as VARCHAR in URI as "+this.path+"/id"));
		node.add(node2);

		ObjectNode node3 = JsonHandler.createObjectNode();
		for (FILTER_ARGS arg : FILTER_ARGS.values()) {
			node3.put(arg.name(), arg.type);
		}
		node2 = RestArgument.fillDetailedAllow(METHOD.GET, this.path, COMMAND_TYPE.MULTIGET.name(), 
				node3);
		node.add(node2);

		node3 = JsonHandler.createObjectNode();
		node3.put(DbConfiguration.Columns.HOSTID.name(), "HostId as VARCHAR in URI as "+this.path+"/id"); 
		DbValue []values = DbConfiguration.getAllType();
		for (DbValue dbValue : values) {
			if (dbValue.column.equalsIgnoreCase(DbConfiguration.Columns.HOSTID.name())) {
				continue;
			}
			node3.put(dbValue.column, dbValue.getType());
		}
		node2 = RestArgument.fillDetailedAllow(METHOD.PUT, this.path+"/id", COMMAND_TYPE.UPDATE.name(), 
				node3);
		node.add(node2);
		
		node3 = JsonHandler.createObjectNode();
		node3.put(DbConfiguration.Columns.HOSTID.name(), "HostId as VARCHAR in URI as "+this.path+"/id"); 
		node2 = RestArgument.fillDetailedAllow(METHOD.DELETE, this.path+"/id", COMMAND_TYPE.DELETE.name(), 
				node3);
		node.add(node2);

		node3 = JsonHandler.createObjectNode();
		for (DbValue dbValue : values) {
			node3.put(dbValue.column, dbValue.getType());
		}
		node2 = RestArgument.fillDetailedAllow(METHOD.POST, this.path, COMMAND_TYPE.CREATE.name(), 
				node3);
		node.add(node2);
		
		node2 = RestArgument.fillDetailedAllow(METHOD.OPTIONS, this.path, COMMAND_TYPE.OPTIONS.name(), null);
		node.add(node2);

		return node;
	}

	@Override
	public String getPrimaryPropertyName() {
		return Columns.HOSTID.name();
	}

}
