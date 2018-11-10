/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * MySQLTable base
 */
public abstract class SQLServerTableBase extends JDBCTable<SQLServerDataSource, SQLServerSchema>
    implements SQLServerObject, DBPNamedObject2,DBPRefreshableObject
{
    private static final Log log = Log.getLog(SQLServerTableBase.class);

    private long objectId;

    protected SQLServerTableBase(SQLServerSchema schema)
    {
        super(schema, false);
    }

    // Copy constructor
    protected SQLServerTableBase(DBRProgressMonitor monitor, SQLServerSchema catalog, DBSEntity source) throws DBException {
        super(catalog, source, false);
    }

    protected SQLServerTableBase(
        SQLServerSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, JDBCUtils.safeGetString(dbResult, "name"), true);

        this.objectId = JDBCUtils.safeGetLong(dbResult, "object_id");
    }

    public SQLServerDatabase getDatabase() {
        return getSchema().getDatabase();
    }

    public SQLServerSchema getSchema() {
        return getContainer();
    }

    @Override
    public JDBCStructCache<SQLServerSchema, ? extends JDBCTable, ? extends JDBCTableColumn> getCache()
    {
        return getContainer().getTableCache();
    }

    @Override
    @Property(viewable = false, editable = false, order = 5)
    public long getObjectId() {
        return objectId;
    }

    @Override
    public Collection<SQLServerTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        List<SQLServerTableColumn> childColumns = getContainer().getTableCache().getChildren(monitor, getContainer(), this);
        if (childColumns == null) {
            return Collections.emptyList();
        }
        List<SQLServerTableColumn> columns = new ArrayList<>(childColumns);
        columns.sort(DBUtils.orderComparator());
        return columns;
    }

    @Override
    public SQLServerTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    public SQLServerTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull long columnId)
        throws DBException
    {
        for (SQLServerTableColumn col : getAttributes(monitor)) {
            if (col.getObjectId() == columnId) {
                return col;
            }
        }
        log.error("Column '" + columnId + "' not found in table '" + getFullyQualifiedName(DBPEvaluationContext.DML) + "'");
        return null;
    }

    @Override
    @Association
    public synchronized Collection<SQLServerTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return this.getContainer().getIndexCache().getObjects(monitor, getSchema(), this);
    }

    public SQLServerTableIndex getIndex(DBRProgressMonitor monitor, long indexId) throws DBException {
        for (SQLServerTableIndex index : getIndexes(monitor)) {
            if (index.getObjectId() == indexId) {
                return index;
            }
        }
        log.error("Index '" + indexId + "' not found in table '" + getFullyQualifiedName(DBPEvaluationContext.DML) + "'");
        return null;
    }

    public SQLServerTableIndex getIndex(DBRProgressMonitor monitor, String name) throws DBException {
        for (SQLServerTableIndex index : getIndexes(monitor)) {
            if (CommonUtils.equalObjects(name, index.getName())) {
                return index;
            }
        }
        log.error("Index '" + name + "' not found in table '" + getFullyQualifiedName(DBPEvaluationContext.DML) + "'");
        return null;
    }


    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getDatabase(),
            getSchema(),
            this);
    }

}
