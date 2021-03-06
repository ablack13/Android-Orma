/*
 * Copyright (c) 2015 FUJI Goro (gfx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.gfx.android.orma.internal;

import com.github.gfx.android.orma.BuildConfig;
import com.github.gfx.android.orma.Selector;

import android.database.Cursor;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class OrmaIterator<Model> implements Iterator<Model> {
    static final int BATCH_SIZE = BuildConfig.DEBUG ? 2 : 1000;

    final Selector<Model, ?> selector;

    final long totalCount;

    long totalPos = 0;

    long offset;

    Cursor cursor;

    public OrmaIterator(Selector<Model, ?> selector) {
        this.offset = selector.hasOffset() ? selector.getOffset() : 0L;
        this.totalCount = selector.hasLimit() ? selector.getLimit() : selector.count();
        this.selector = selector.clone().resetLimitClause();

        fill();
    }

    void fill() {
        if (cursor != null) {
            cursor.close();
        }
        cursor = selector
                .limit(BATCH_SIZE)
                .offset(offset)
                .execute();

        cursor.moveToFirst();

        offset += BATCH_SIZE;
    }

    @Override
    public boolean hasNext() {
        return totalPos < totalCount;
    }

    @Override
    public Model next() {
        if (totalPos >= totalCount) {
            throw new NoSuchElementException("OrmaIterator#next()");
        }

        Model model = selector.newModelFromCursor(cursor);

        totalPos++;

        if (!hasNext()) {
            cursor.close();
        } else if (cursor.isLast()) {
            fill();
        } else {
            cursor.moveToNext();
        }

        return model;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Iterator#remove()");
    }
}
