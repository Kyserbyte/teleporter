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

package com.orientechnologies.orient.importer.nameresolver;

import com.orientechnologies.orient.importer.mapper.OER2GraphMapper;
import com.orientechnologies.orient.importer.model.dbschema.ORelationship;

/**
 * @author Gabriele Ponzi
 * @email  gabriele.ponzi-at-gmaildotcom
 *
 */

public interface ONameResolver {
  
  public String resolveVertexName(String candidatename);
  
  public  String resolveVertexProperty(String candidateName);
  
  public String resolveEdgeName(ORelationship relationship, OER2GraphMapper mapper);
  
  public String reverseTransformation(String transformedName);


}