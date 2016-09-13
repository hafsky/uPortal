/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apereo.portal.groups.pags.testers;

import org.apereo.portal.groups.pags.dao.IPersonAttributesGroupTestDefinition;
import org.apereo.portal.security.IPerson;

/**
 * Base class for testers that determine membership based on the number of
 * values a user has for an attribute.
 *
 * @author drewwills
 * @since 5.0
 */
public abstract class AbstractNbValuesTester extends BaseAttributeTester {

    private final int testInteger;

    /**
     * @since 4.3
     */
    public AbstractNbValuesTester(IPersonAttributesGroupTestDefinition definition) {
        super(definition);
        this.testInteger = Integer.parseInt(definition.getTestValue());
    }

    /**
     * @deprecated use {@link EntityPersonAttributesGroupStore}, which leverages
     * the single-argument constructor.
     */
    @Deprecated
    public AbstractNbValuesTester(String attribute, String test) {
        super(attribute, test);
        testInteger = Integer.parseInt(test);
    }

    public int getTestInteger() {
        return testInteger;
    }

    @Override
    public final boolean test(IPerson person) {
        boolean result = false;
        final Object[] atts = person.getAttributeValues(getAttributeName());
        if (atts != null) {
            result = test(atts.length);
        }
        return result;
    }

    /**
     * Subclasses provide a concrete implementation of this method to perform
     * their testing.
     */
    protected abstract boolean test(int numValues);

}
