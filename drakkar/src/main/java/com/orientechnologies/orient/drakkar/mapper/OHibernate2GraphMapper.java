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

package com.orientechnologies.orient.drakkar.mapper;

import java.io.File;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.orientechnologies.orient.drakkar.context.ODrakkarContext;
import com.orientechnologies.orient.drakkar.model.dbschema.OAttribute;
import com.orientechnologies.orient.drakkar.model.dbschema.OEntity;
import com.orientechnologies.orient.drakkar.model.dbschema.OPrimaryKey;

/**
 * Extends OER2GraphMapper thus manages the source DB schema and the destination graph model with their correspondences.
 * Unlike the superclass, this class builds the source DB schema starting from Hibernate's XML configuration file.
 * 
 * @author Gabriele Ponzi
 * @email  <gabriele.ponzi--at--gmail.com>
 *
 */

public class OHibernate2GraphMapper extends OER2GraphMapper {

  private String xmlPath;

  public OHibernate2GraphMapper(String driver, String uri, String username, String password, String xmlPath) {
    super(driver, uri, username, password);
    this.xmlPath = xmlPath;
  }

  @Override
  public void buildSourceSchema(ODrakkarContext context) {

    try {

      /*
       * Building Info from DB Schema
       */

      super.buildSourceSchema(context);


      /*
       * XML Checking and Inheritance
       */

      // XML parsing and DOM building

      File xmlFile = new File(this.xmlPath);
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document dom = dBuilder.parse(xmlFile);


      NodeList entities = dom.getElementsByTagName("class");
      Element currentEntityElement;
      OEntity currentEntity = null;

      for(int i=0; i<entities.getLength(); i++) {
        currentEntityElement = (Element) entities.item(i);

        if(currentEntityElement.hasAttribute("table"))
          currentEntity = super.dataBaseSchema.getEntityByName(currentEntityElement.getAttribute("table"));
        else {
          context.getOutputManager().error("XML Format ERROR: problem in class definition, table attribute missing on class node.");
          System.exit(0);
        }

        // check primary key
        if(currentEntity.getPrimaryKey().getInvolvedAttributes().size() == 0) {
          this.detectPrimaryKey(dom, currentEntityElement, currentEntity, context);
        }

        // check foreign key
        if(currentEntity.getForeignKeys().size() == 0) {
          this.detectForeignKeys(dom, currentEntityElement, currentEntity, context);
        }

        // inheritance
        this.detectInheritanceAndUpdateSchema(dom, currentEntity, currentEntityElement, context);
      }

      // sorting tables for inheritance level and then for name
      Collections.sort(super.dataBaseSchema.getEntities());

    }catch(Exception e) {
      e.printStackTrace();
    }

  }

  private void detectPrimaryKey(Document dom, Element currentEntityElement, OEntity currentEntity, ODrakkarContext context) {

    OPrimaryKey pKey = currentEntity.getPrimaryKey();

    // adding primary key or composite primary key
    NodeList pKeyElements = currentEntityElement.getElementsByTagName("id");
    NodeList compositePKeyElements = currentEntityElement.getElementsByTagName("composite-id");

    if(pKeyElements.getLength() == compositePKeyElements.getLength()) {
      context.getOutputManager().error("XML Format ERROR: problem on the primary key inference of the entity '" + currentEntity.getName()  + "', primary key neither present in Db Schema nor in XMl mapping file.");
      System.exit(0);
    }

    if(pKeyElements.getLength()==1) {

      Element pKeyElement = (Element) pKeyElements.item(0);
      OAttribute currentAttribute;
      if(pKeyElement.hasAttribute("column")) {
        currentAttribute = currentEntity.getAttributeByName(pKeyElement.getAttribute("column"));
      }
      else {
        Element column = (Element) pKeyElement.getElementsByTagName("column").item(0);
        currentAttribute = currentEntity.getAttributeByName(column.getAttribute("name"));
      }
      pKey.addAttribute(currentAttribute);
    }

    else if (compositePKeyElements.getLength() == 1) {

      Element compositePKeyElement = (Element) compositePKeyElements.item(0);
      NodeList compositePKeyAttributes = compositePKeyElement.getElementsByTagName("key-property");

      OAttribute currentAttribute;
      for(int i=0; i<compositePKeyAttributes.getLength(); i++) {
        currentAttribute = currentEntity.getAttributeByName(((Element)compositePKeyAttributes.item(i)).getAttribute("column"));
        pKey.addAttribute(currentAttribute);
      }
    }

    else if(pKeyElements.getLength()>1 || compositePKeyElements.getLength()>1) {
      context.getOutputManager().error("XML Format ERROR: problem on the primary key inference of the entity '" + currentEntity.getName()  + "'.");
      System.exit(0);
    }
  }


  private void detectInheritanceAndUpdateSchema(Document dom, OEntity parentEntity, Element parentEntityElement, ODrakkarContext context) {

//    NodeList classElements = dom.getElementsByTagName("class");
//    Element currentEntityElement;
//    OEntity currentEntity;
//    for(int i=0; i<classElements.getLength(); i++) {

      NodeList subclassElements = parentEntityElement.getElementsByTagName("subclass");
      NodeList joinedSubclassElements = parentEntityElement.getElementsByTagName("joined-subclass");

      if(subclassElements.getLength() > 0) {
        this.performSubclassInheritance(dom, parentEntity, subclassElements, context);
      }

      if(joinedSubclassElements.getLength() > 0) {
        this.performJoinedSubclassInheritance(dom, parentEntity, joinedSubclassElements, context);
      }
//    }

  }


  private void performSubclassInheritance(Document dom, OEntity parentEntity, NodeList subclassElements, ODrakkarContext context) {
    // TODO Auto-generated method stub

  }

  private void performJoinedSubclassInheritance(Document dom, OEntity parentEntity, NodeList joinedSubclassElements, ODrakkarContext context) {

    Element currentChildElement;
    OEntity currentChildEntity;
    String currentChildEntityName = null;

    for(int i=0; i<joinedSubclassElements.getLength(); i++) {
      currentChildElement = (Element) joinedSubclassElements.item(i);
      if(currentChildElement.hasAttribute("table"))
        currentChildEntityName = currentChildElement.getAttribute("table");
      else {
        context.getOutputManager().error("XML Format ERROR: problem in subclass definition, table attribute missing on joined-subclass node.");
        System.exit(0);
      }
      currentChildEntity = super.dataBaseSchema.getEntityByName(currentChildEntityName);
      currentChildEntity.setParentEntity(parentEntity);
      currentChildEntity.setInheritanceLevel(parentEntity.getInheritanceLevel()+1);

      // recursive call on the node
      this.detectInheritanceAndUpdateSchema(dom, currentChildEntity, currentChildElement, context);
    }

  }


  private void detectForeignKeys(Document dom, Element currentEntityElement, OEntity currentEntity, ODrakkarContext context) {

    NodeList one2OneElements = currentEntityElement.getElementsByTagName("one-to-one");
    NodeList many2OneElements = currentEntityElement.getElementsByTagName("many-to-one");
    NodeList one2ManyElements = currentEntityElement.getElementsByTagName("one-to-many");

    // adding relationships one-to-one if present
    //TODO

    // adding relationships many-to-one if present
    //TODO


    // adding relationships one-to-many if present
    //TODO


  }



}
