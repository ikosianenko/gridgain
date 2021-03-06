/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.jdbc.suite;

import junit.framework.*;
import org.gridgain.jdbc.*;

/**
 * JDBC driver test suite.
 */
public class GridJdbcDriverTestSuite extends TestSuite {
    /**
     * @return JDBC Driver Test Suite.
     * @throws Exception In case of error.
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite("Gridgain JDBC Driver Test Suite");

        suite.addTest(new TestSuite(GridJdbcConnectionSelfTest.class));
        suite.addTest(new TestSuite(GridJdbcStatementSelfTest.class));
        suite.addTest(new TestSuite(GridJdbcPreparedStatementSelfTest.class));
        suite.addTest(new TestSuite(GridJdbcResultSetSelfTest.class));
        suite.addTest(new TestSuite(GridJdbcComplexQuerySelfTest.class));
        suite.addTest(new TestSuite(GridJdbcMetadataSelfTest.class));
        suite.addTest(new TestSuite(GridJdbcEmptyCacheSelfTest.class));
        suite.addTest(new TestSuite(GridJdbcLocalCachesSelfTest.class));

        return suite;
    }
}
