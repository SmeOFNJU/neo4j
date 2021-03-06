/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.runtime.vectorized.operators

import org.neo4j.cypher.internal.compatibility.v3_5.runtime.SlotConfiguration
import org.neo4j.cypher.internal.runtime.QueryContext
import org.neo4j.cypher.internal.runtime.interpreted.pipes.{QueryState => OldQueryState}
import org.neo4j.cypher.internal.runtime.vectorized._


/*
Responsible for aggregating the data coming from a single morsel. This is equivalent to the map
step of map-reduce. Each thread performs it its local aggregation on the data local to it. In
the subsequent reduce steps these local aggregations are merged into a single global aggregate.
 */
class AggregationMapperOperatorNoGrouping(slots: SlotConfiguration, aggregations: Array[AggregationOffsets]) extends MiddleOperator {


  override def operate(iterationState: Iteration, data: Morsel, context: QueryContext, state: QueryState): Unit = {
    val aggregationMappers = aggregations.map(_.aggregation.createAggregationMapper)
    val longCount = slots.numberOfLongs
    val refCount = slots.numberOfReferences
    val currentRow = new MorselExecutionContext(data, longCount, refCount, currentRow = 0)
    val queryState = new OldQueryState(context, resources = null, params = state.params)

    //loop over the entire morsel and apply the aggregation
    while (currentRow.currentRow < data.validRows) {
      var accCount = 0
      while (accCount < aggregations.length) {
        aggregationMappers(accCount).map(currentRow, queryState)
        accCount += 1
      }
      currentRow.currentRow += 1
    }

    //Write the local aggregation value to the morsel in order for the
    //reducer to pick it up later
    var i = 0
    while (i < aggregations.length) {
      val aggregation = aggregations(i)
      data.refs(aggregation.incoming) = aggregationMappers(i).result
      i += 1
    }

    //we have written a single row
    data.validRows = 1
  }
}
