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

import java.util.Arrays;

import junit.framework.TestCase;
import com.workplacesystems.utilsj.collections.FilterableArrayList;

public class CountTest extends TestCase
{
    public void testCount()
    {
        // some lists to iterate
        FilterableArrayList<String> empty_list = new FilterableArrayList<String>();
        FilterableArrayList<String> list = new FilterableArrayList<String>(
                Arrays.asList(new String[]{"gaga", "lott", "boots"})
        );

        // ensure nothing counted in an empty list
        Integer size = new Count<String>().iterate(empty_list);
        assertEquals(0, size.intValue());
        
        // ensure all counted in a list
        size = new Count<String>().iterate(list);
        assertEquals(3, size.intValue());
    }
}
