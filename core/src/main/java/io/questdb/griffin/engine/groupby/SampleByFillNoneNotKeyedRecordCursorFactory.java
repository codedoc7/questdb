/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2020 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.griffin.engine.groupby;

import io.questdb.cairo.sql.Function;
import io.questdb.cairo.sql.RecordCursorFactory;
import io.questdb.cairo.sql.RecordMetadata;
import io.questdb.griffin.engine.functions.GroupByFunction;
import io.questdb.std.Misc;
import io.questdb.std.ObjList;
import org.jetbrains.annotations.NotNull;

public class SampleByFillNoneNotKeyedRecordCursorFactory extends AbstractSampleByNotKeyedRecordCursorFactory {
    private final SampleByFillNoneNotKeyedRecordCursor cursor;

    public SampleByFillNoneNotKeyedRecordCursorFactory(
            RecordCursorFactory base,
            @NotNull TimestampSampler timestampSampler,
            RecordMetadata groupByMetadata,
            ObjList<GroupByFunction> groupByFunctions,
            ObjList<Function> recordFunctions,
            int valueCount,
            int timestampIndex,
            Function timezoneNameFunc,
            Function offsetFunc
    ) {
        super(base, groupByMetadata, recordFunctions, timezoneNameFunc, offsetFunc);
        final SimpleMapValue simpleMapValue = new SimpleMapValue(valueCount);
        try {
            this.cursor = new SampleByFillNoneNotKeyedRecordCursor(
                    simpleMapValue,
                    groupByFunctions,
                    recordFunctions,
                    timestampIndex,
                    timestampSampler
            );
        } catch (Throwable e) {
            Misc.freeObjList(recordFunctions);
            throw e;
        }
    }

    @Override
    protected AbstractNoRecordSampleByCursor getRawCursor() {
        return cursor;
    }
}
