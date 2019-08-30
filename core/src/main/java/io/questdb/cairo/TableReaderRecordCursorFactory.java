/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2019 Appsicle
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package io.questdb.cairo;

import io.questdb.cairo.sql.RecordCursor;
import io.questdb.cairo.sql.RecordMetadata;
import io.questdb.griffin.SqlExecutionContext;

public class TableReaderRecordCursorFactory extends AbstractRecordCursorFactory {
    private final TableReaderRecordCursor cursor = new TableReaderRecordCursor();
    private final CairoEngine engine;
    private final String tableName;
    private final long tableVersion;

    public TableReaderRecordCursorFactory(RecordMetadata metadata, CairoEngine engine, String tableName, long tableVersion) {
        super(metadata);
        this.engine = engine;
        this.tableName = tableName;
        this.tableVersion = tableVersion;
    }

    @Override
    public RecordCursor getCursor(SqlExecutionContext executionContext) {
        cursor.of(engine.getReader(executionContext.getCairoSecurityContext(), tableName, tableVersion));
        return cursor;
    }

    @Override
    public boolean isRandomAccessCursor() {
        return true;
    }
}