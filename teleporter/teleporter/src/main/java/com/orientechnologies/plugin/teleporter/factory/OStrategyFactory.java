/*
 *
 *  *  Copyright 2015 Orient Technologies LTD (info(at)orientechnologies.com)
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *  * For more information: http://www.orientechnologies.com
 *
 */

package com.orientechnologies.plugin.teleporter.factory;

import com.orientechnologies.plugin.teleporter.context.OTeleporterContext;
import com.orientechnologies.plugin.teleporter.strategy.OImportStrategy;
import com.orientechnologies.plugin.teleporter.strategy.ONaiveAggregationImportStrategy;
import com.orientechnologies.plugin.teleporter.strategy.ONaiveImportStrategy;

/**
 * Factory used to instantiate the chosen strategy for the importing phase starting from its name.
 * 
 * @author Gabriele Ponzi
 * @email  <gabriele.ponzi--at--gmail.com>
 * 
 */

public class OStrategyFactory {

	public OStrategyFactory() {}

	public OImportStrategy buildStrategy(String chosenStrategy, OTeleporterContext context) {
		OImportStrategy strategy = null;

		if(chosenStrategy == null)  {
			strategy = new ONaiveAggregationImportStrategy();
		}
		else {
			switch(chosenStrategy) {

			case "naive":   strategy = new ONaiveImportStrategy();
			break;

			case "naive-aggregate":   strategy = new ONaiveAggregationImportStrategy();
			break;

			default :  context.getOutputManager().error("Strategy doesn't exist.\n");
			}
		}
		return strategy;
	}

}