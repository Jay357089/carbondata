/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.carbondata.query.querystats;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mockit.Mock;
import mockit.MockUp;
import org.carbondata.core.datastorage.store.filesystem.LocalCarbonFile;
import org.carbondata.core.datastorage.store.impl.FileFactory;
import org.carbondata.core.util.CarbonProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BinaryQueryStoreTest {
    private String basePath;

    /**
     * store location path to store query stats
     *
     * @return
     */
    private static String getQueryStatsPath(String metaPath) {

        StringBuffer queryStatsPath = new StringBuffer(metaPath);
        queryStatsPath.append(File.separator).append(Preference.AGGREGATE_STORE_DIR);

        return queryStatsPath.toString();
    }

    @Before
    public void setUpBeforeClass() throws Exception {
        try {
            File file = new File("src/test/resources/schemas/");
            basePath = file.getCanonicalPath() + "/";
            CarbonProperties.getInstance().addProperty("carbon.storelocation", basePath + "store");
            CarbonProperties.getInstance().addProperty(Preference.PERFORMANCE_GOAL_KEY, "3");
            CarbonProperties.getInstance().addProperty(Preference.QUERY_STATS_EXPIRY_DAYS_KEY, "30");
            CarbonProperties.getInstance().addProperty(Preference.BENEFIT_RATIO_KEY, "10");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLogQueryAndReadQuery() {
        new MockUp<FileFactory>() {

            @Mock
            public DataOutputStream getDataOutputStreamUsingAppend(String path,
                    FileFactory.FileType fileType) throws IOException {
                return new DataOutputStream(
                        new BufferedOutputStream(new FileOutputStream(path, true), 1024));
            }

        };

        QueryDetail queryDetail = createQueryDetail();

        QueryStatsCollector queryStatsCollector = QueryStatsCollector.getInstance();
        queryStatsCollector.addQueryStats(queryDetail.getQueryId(), queryDetail);

        queryStatsCollector.logQueryStats(queryDetail);
        //change total execution time
        queryDetail.setTotalExecutionTime(5000);
        queryStatsCollector.logQueryStats(queryDetail);

        //change queryname
        queryDetail = createQueryDetail();
        queryDetail.setDimOrdinals(new int[] { 1, 2, 4, 2 });
        queryDetail.setTotalExecutionTime(5000);
        queryStatsCollector.logQueryStats(queryDetail);

        //same query with less recordsize
        queryDetail = createQueryDetail();
        queryDetail.setDimOrdinals(new int[] { 1, 2, 4, 2 });
        queryDetail.setRecordSize(10);
        queryDetail.setNoOfRowsScanned(1000);
        queryDetail.setTotalExecutionTime(5000);
        queryStatsCollector.logQueryStats(queryDetail);

        //set filter as true
        queryDetail = createQueryDetail();
        queryDetail.setFilterQuery(true);
        queryStatsCollector.logQueryStats(queryDetail);
        //set limit as true
        queryDetail = createQueryDetail();
        queryDetail.setLimitPassed(true);
        queryStatsCollector.logQueryStats(queryDetail);

        //set high record size
        queryDetail = createQueryDetail();
        queryDetail.setRecordSize(100);
        queryStatsCollector.logQueryStats(queryDetail);

        //set expired query
        queryDetail = createQueryDetail();
        queryDetail.setDimOrdinals(new int[] { 4, 5, 6 });
        long queryTime = System.currentTimeMillis() - 3024000000l;
        queryDetail.setQueryStartTime(queryTime);

        queryStatsCollector.logQueryStats(queryDetail);

        //set expired query
        queryDetail = createQueryDetail();
        queryDetail.setDimOrdinals(new int[] { 4, 5, 6 });
        queryDetail.setRecordSize(0);
        queryTime = System.currentTimeMillis() - 3024000000l;
        queryDetail.setQueryStartTime(queryTime);

        queryStatsCollector.logQueryStats(queryDetail);

        Assert.assertNotNull(queryStatsCollector.getQueryStats(queryDetail.getQueryId()));

        String queryStatsPath = getQueryStatsPath(queryDetail.getMetaPath());

        queryStatsCollector.removeQueryStats(queryDetail.getQueryId());
        QueryStore queryStore = new BinaryQueryStore();
        QueryDetail[] queryDetails = queryStore
                .readQueryDetail(queryStatsPath + File.separator + Preference.QUERYSTATS_FILE_NAME);
        Arrays.sort(queryDetails);
        QueryNormalizer queryNormalizer = new QueryNormalizer();
        for (QueryDetail qd : queryDetails) {
            queryNormalizer.addQueryDetail(qd);
        }
        List<QueryDetail> normalizedQueries = queryNormalizer.getNormalizedQueries();
        if (queryNormalizer.isReWriteRequired()) {
            queryStore.writeQueryToFile(normalizedQueries, queryStatsPath);
        }
        Assert.assertTrue(normalizedQueries.size() == 2);
        for (QueryDetail qd : normalizedQueries) {
            if (qd.getBenefitRatio() >= Preference.BENEFIT_RATIO) {
                Assert.assertTrue(true);
            } else {
                Assert.assertTrue(false);
            }
        }

    }

    @Test
    public void testLogQuery_directoryDoesntexist() {
        QueryDetail queryDetail = createQueryDetail();
        String queryStatsPath = getQueryStatsPath(queryDetail.getMetaPath());
        QueryStore queryStore = new BinaryQueryStore();

        new MockUp<FileFactory>() {

            @Mock
            public boolean isFileExist(String filePath, FileFactory.FileType fileType,
                    boolean performcheck) throws IOException {
                return false;
            }

        };

        queryStore.logQuery(queryDetail);
        File file = new File(queryStatsPath + File.separator + Preference.QUERYSTATS_FILE_NAME);
        if (file.exists()) {
            file.delete();
        }
        Assert.assertTrue(!file.exists());

    }

    @Test
    public void testLogQuery_ThrowExceptionReadingQueryStats() {
        QueryDetail queryDetail = createQueryDetail();
        String queryStatsPath = getQueryStatsPath(queryDetail.getMetaPath());
        File file = new File(queryStatsPath + File.separator + Preference.QUERYSTATS_FILE_NAME);
        if (file.exists()) {
            file.delete();
        }

        new MockUp<FileFactory>() {

            @Mock
            public DataOutputStream getDataOutputStreamUsingAppend(String path,
                    FileFactory.FileType fileType) throws IOException {
                throw new IOException();
            }

            @Mock
            public DataOutputStream getDataOutputStream(String path, FileFactory.FileType fileType)
                    throws IOException {
                throw new IOException();
            }

        };

        QueryStore queryStore = new BinaryQueryStore();
        queryStore.logQuery(queryDetail);
        Assert.assertTrue(!file.exists());

    }

    @Test
    public void testPartitionStatsCollector_getPartionDetail() {
        QueryDetail queryDetail = createQueryDetail();
        Assert.assertNotNull(
                PartitionStatsCollector.getInstance().getPartionDetail(queryDetail.getQueryId()));
    }

    @Test
    public void testPartitionStatsCollector_removePartionDetail() {
        QueryDetail queryDetail = createQueryDetail();
        PartitionStatsCollector.getInstance().removePartitionDetail(queryDetail.getQueryId());
        Assert.assertNull(
                PartitionStatsCollector.getInstance().getPartionDetail(queryDetail.getQueryId()));
    }

    @Test
    public void testWriteQueryToFile_ThrowException() {
        BinaryQueryStore qs = new BinaryQueryStore();
        qs.writeQueryToFile(null, "test");
        File f = new File("test");
        f.delete();
        Assert.assertTrue(true);

    }

    @Test
    public void testQueryStatsCollectorgetInitialPartitionAccumulatorValue() {
        Assert.assertNull(QueryStatsCollector.getInstance().getInitialPartitionAccumulatorValue());
    }

    @Test
    public void testQueryStatsCollector() {
        Assert.assertNotNull(QueryStatsCollector.getInstance().getPartitionAccumulatorParam());
    }

    @Test
    public void testLogQueryStats_ThrowException() {
        QueryStatsCollector.getInstance().logQueryStats(null);
        Assert.assertTrue(true);
    }

    @Test
    public void testWriteQueryToFile_failedToRename() {
        try {
            new MockUp<LocalCarbonFile>() {

                @Mock
                boolean renameTo(String changetoName) {
                    return false;
                }
            };

            QueryDetail queryDetail = createQueryDetail();
            BinaryQueryStore store = new BinaryQueryStore();
            List<QueryDetail> list = new ArrayList<QueryDetail>();
            list.add(queryDetail);
            File file = new File("src/test/resources/test");
            file.createNewFile();
            store.writeQueryToFile(list, file.getAbsolutePath());
        } catch (Exception e) {

        }

    }

    @Test
    public void testLogQuery_throwException() {
        try {
            new MockUp<FileFactory>() {

                @Mock
                public boolean isFileExist(String filePath, FileFactory.FileType fileType,
                        boolean performFileCheck) throws IOException {
                    throw new IOException();
                }
            };

            QueryDetail queryDetail = createQueryDetail();
            BinaryQueryStore store = new BinaryQueryStore();
            List<QueryDetail> list = new ArrayList<QueryDetail>();
            list.add(queryDetail);
            File file = new File("src/test/resources/test");
            file.createNewFile();
            store.logQuery(queryDetail);
        } catch (Exception e) {

        }

    }

    @Test
    public void testReadQueryDetail_NoQuery() {
        try {
            new MockUp<java.io.DataInputStream>() {

                @Mock
                public int readInt() throws IOException {
                    return -1;
                }
            };
            File temp = new File("temp");
            temp.createNewFile();
            BinaryQueryStore store = new BinaryQueryStore();
            store.readQueryDetail("temp");
            temp.delete();

        } catch (Exception e) {

        }

    }

    private QueryDetail createQueryDetail() {
        int[] dimOrdinals = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        long queryStartTime = System.currentTimeMillis();
        String queryId = System.nanoTime() + "";
        QueryDetail queryDetail = new QueryDetail(queryId);
        QueryStatsCollector queryStatsCollector = QueryStatsCollector.getInstance();
        queryStatsCollector.addQueryStats(queryId, queryDetail);
        queryDetail.setQueryStartTime(queryStartTime);

        queryDetail.setDimOrdinals(dimOrdinals);
        queryDetail.setFactTableName("carbon");
        queryDetail.setCubeName("carbon");
        queryDetail.setSchemaName("default");
        queryDetail.setRecordSize(5);

        queryDetail.setTotalExecutionTime(queryStartTime + 100);
        PartitionDetail partitionDetail = new PartitionDetail("0");
        partitionDetail.getPartitionId();
        PartitionStatsCollector partitionStatsCollector = PartitionStatsCollector.getInstance();
        partitionStatsCollector.addPartitionDetail(queryId, partitionDetail);

        partitionDetail.addNumberOfNodesScanned(10);
        partitionDetail.addNumberOfRowsScanned(100);
        queryDetail.setPartitionsDetail(null);
        queryDetail.getPartitionsDetail();
        queryDetail.hashCode();
        queryDetail.equals(null);
        queryDetail.setNoOfRowsScanned(partitionDetail.getNoOfRowsScanned());
        queryDetail.setGroupBy(true);
        queryDetail.setLimitPassed(false);
        queryDetail.setFilterQuery(false);
        queryDetail.setMetaPath(basePath + "default/carbon");
        return queryDetail;
    }

}
