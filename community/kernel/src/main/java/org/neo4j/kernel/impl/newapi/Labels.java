/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.newapi;

import org.eclipse.collections.api.set.primitive.IntSet;

import java.util.Arrays;
import java.util.Collection;

import org.neo4j.internal.kernel.api.LabelSet;

import static org.neo4j.collection.PrimitiveIntCollections.asLongArray;

public class Labels implements LabelSet
{
    /**
     * This really only needs to be {@code int[]}, but the underlying implementation uses {@code long[]} for some
     * reason.
     */
    private final long[] labels;

    private Labels( long[] labels )
    {
        this.labels = labels;
    }

    public static Labels from( long[] labels )
    {
        return new Labels( labels );
    }

    static Labels from( Collection<Integer> integers )
    {
        int size = integers.size();
        long[] tokens = new long[size];
        int i = 0;
        for ( Integer token : integers )
        {
            tokens[i++] = token;
        }
        return new Labels( tokens );
    }

    static Labels from( IntSet set )
    {
        return new Labels( asLongArray( set ) );
    }

    @Override
    public int numberOfLabels()
    {
        return labels.length;
    }

    @Override
    public int label( int offset )
    {
        return (int) labels[offset];
    }

    @Override
    public boolean contains( int labelToken )
    {
        //It may look tempting to use binary search
        //however doing a linear search is actually faster for reasonable
        //label sizes (≤100 labels)
        for ( long label : labels )
        {
            if ( label == labelToken )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "Labels" + Arrays.toString( labels );
    }

    public long[] all()
    {
        return labels;
    }
}
