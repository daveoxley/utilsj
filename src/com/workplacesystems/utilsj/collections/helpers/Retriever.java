/*
 * Copyright 2010 Workplace Systems PLC (http://www.workplacesystems.com/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.workplacesystems.utilsj.collections.helpers;

import com.workplacesystems.utilsj.collections.IterativeCallback;
/**
 * Iterator to retrieve the first element visited in an iterated collection.  Clients can retrieve
 * the first element satisfying a property by using this in conjunction with a filter. 
 * @see Collector - collect results of applying a function to all elements visited
 * @see Detector - check if any elements visited
 * @see Retriever - first element visited 
 * @see Selector - all elements visited
 * @see Count - count of all elements visited 
 */
public class Retriever<T> extends IterativeCallback<T,T>
{
    /** Iterator which will return the first object it visits */
    public Retriever()
    {
        super();
    }

    @Override
    protected void nextObject(T obj)
    {
        _return(obj);
    }
}
