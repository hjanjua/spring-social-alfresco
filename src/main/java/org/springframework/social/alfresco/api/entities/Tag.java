/*
 * Copyright 2012 Alfresco Software Limited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * 
 * This file is part of an unsupported extension to Alfresco.
 */

package org.springframework.social.alfresco.api.entities;


import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;


/**
 * Tag
 * 
 * @author jottley
 * 
 */
@JsonSerialize(include = Inclusion.NON_NULL)
public class Tag
{
    private String id;
    private String tag;


    /**
     * @return The unique id of the tag
     */
    public String getId()
    {
        return id;
    }


    /**
     * Set the unique id of the tag
     * 
     * @param id
     */
    public void setId(String id)
    {
        this.id = id;
    }


    /**
     * @return The value of the tag
     */
    public String getTag()
    {
        return tag;
    }


    /**
     * Set the value of the tag
     * 
     * @param tag
     */
    public void setTag(String tag)
    {
        this.tag = tag;
    }


}
