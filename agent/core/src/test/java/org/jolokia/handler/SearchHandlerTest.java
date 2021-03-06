package org.jolokia.handler;

/*
 * Copyright 2009-2011 Roland Huss
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.io.IOException;
import java.util.*;

import javax.management.*;

import org.jolokia.request.JmxRequestBuilder;
import org.jolokia.request.JmxSearchRequest;
import org.jolokia.restrictor.AllowAllRestrictor;
import org.jolokia.util.RequestType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author roland
 * @since 12.09.11
 */
public class SearchHandlerTest {


    private SearchHandler handler;

    private MBeanServerConnection connection;

    @BeforeMethod
    public void setup() {
        handler = new SearchHandler(new AllowAllRestrictor());
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void unsupported() throws InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException, MalformedObjectNameException {
        handler.handleRequest((MBeanServerConnection) null,
                              new JmxRequestBuilder(RequestType.SEARCH,"java.lang:*").<JmxSearchRequest>build());
    }

    @Test
    public void handleAllServersAtOnce() throws MalformedObjectNameException {
        assertTrue(handler.handleAllServersAtOnce(new JmxRequestBuilder(RequestType.SEARCH, "java.lang:*").<JmxSearchRequest>build()));
    }

    @Test
    public void simple() throws MalformedObjectNameException, InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException {
        List<String> res = doSearch("java.lang:*", "java.lang:type=Memory", "java.lang:type=Runtime");
        assertEquals(res.size(),2);
        assertTrue(res.contains("java.lang:type=Memory"));
        assertTrue(res.contains("java.lang:type=Runtime"));
        verify(connection);
    }


    @Test
    public void withEscaping() throws MalformedObjectNameException, InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException {
        String attr = "java.lang:type=\"m:e*m\\\"?o\\\\y\\n\"";
        List<String> res = doSearch("java.lang:*", attr);
        assertEquals(res.size(),1);
        assertTrue(res.contains(attr));
        verify(connection);
    }


    private List<String> doSearch(String pPattern, String ... pFoundNames) throws MalformedObjectNameException, IOException, InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException {
        ObjectName oName = new ObjectName(pPattern);
        JmxSearchRequest request = new JmxRequestBuilder(RequestType.SEARCH,oName).build();

        connection = createMock(MBeanServerConnection.class);
        Set<ObjectName> names = new HashSet<ObjectName>();
        for (String name : pFoundNames) {
            names.add(new ObjectName(name));
        }
        expect(connection.queryNames(oName,null)).andReturn(names);
        replay(connection);
        return (List<String>) handler.handleRequest(new HashSet<MBeanServerConnection>(Arrays.asList(connection)),request);
    }
}
