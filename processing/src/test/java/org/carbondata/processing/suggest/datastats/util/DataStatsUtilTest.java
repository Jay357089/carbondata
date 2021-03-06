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

package org.carbondata.processing.suggest.datastats.util;

import java.io.*;

import junit.framework.Assert;
import mockit.Mock;
import mockit.MockUp;
import org.carbondata.core.datastorage.store.impl.FileFactory;
import org.carbondata.core.carbon.SqlStatement;
import org.carbondata.processing.suggest.datastats.model.Level;
import org.carbondata.query.expression.DataType;
import org.junit.Test;

public class DataStatsUtilTest {
    private String basePath = "src/test/resources";

    @Test
    public void testSerializeObject_ExceptionFromDataOutputStream() {
        File test = new File(basePath);
        Level level = new Level(1, 10);
        level.setName("1");
        try {
            new MockUp<FileFactory>() {

                @Mock
                public DataOutputStream getDataOutputStream(String path,
                        FileFactory.FileType fileType) throws IOException {
                    throw new IOException();
                }

            };

            DataStatsUtil.serializeObject(level, test.getCanonicalPath(), "test");
            Assert.assertTrue(true);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testReadSerializedFile_ExceptionFromDataInputputStream() {
        File test = new File(basePath + "/test");
        Level level = new Level(1, 10);
        level.setName("1");

        new MockUp<FileFactory>() {

            @Mock
            public DataInputStream getDataInputStream(String path, FileFactory.FileType fileType)
                    throws IOException {
                throw new IOException();
            }

        };

        new MockUp<FileFactory>() {

            @Mock
            public boolean isFileExist(String filePath, FileFactory.FileType fileType,
                    boolean performFileCheck) throws IOException {
                return true;
            }

        };

        DataStatsUtil.readSerializedFile(test.getAbsolutePath());
        Assert.assertTrue(true);

    }

    @Test
    public void testSerializeObject_ExceptionFromObjectStream() {
        File test = new File(basePath);
        Level level = new Level(1, 10);
        level.setName("1");
        try {
            new MockUp<ObjectOutputStream>() {

                @Mock
                public final void writeObject(Object obj) throws IOException {
                    throw new IOException();
                }

            };

            DataStatsUtil.serializeObject(level, test.getCanonicalPath(), "test");
            Assert.assertTrue(true);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Test
    public void testCreateDirectory_failedMakeDir() {
        File test = new File(basePath + "/test");
        new MockUp<FileFactory>() {

            @Mock
            public boolean mkdirs(String filePath, FileFactory.FileType fileType)
                    throws IOException {
                return false;
            }

        };
        try {
            if (test.exists()) {
                test.delete();
            }
            Assert.assertTrue(!DataStatsUtil.createDirectory(test.getCanonicalPath()));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateDirectory_failedMakeDir_throwException() {
        File test = new File(basePath + "/test");
        new MockUp<FileFactory>() {

            @Mock
            public boolean mkdirs(String filePath, FileFactory.FileType fileType)
                    throws IOException {
                throw new IOException();
            }

        };
        try {
            if (test.exists()) {
                test.delete();
            }
            Assert.assertTrue(!DataStatsUtil.createDirectory(test.getCanonicalPath()));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testReadSerializedFile_FileExistThrowException() {
        new MockUp<FileFactory>() {

            @Mock
            public boolean isFileExist(String filePath, FileFactory.FileType fileType,
                    boolean performFileCheck) throws IOException {
                throw new IOException();
            }

        };
        DataStatsUtil.readSerializedFile("test/test");
        Assert.assertTrue(true);
    }

    @Test
    public void testGetDataType() {
        Assert.assertEquals(DataType.DoubleType,
                DataStatsUtil.getDataType(SqlStatement.Type.DOUBLE));
        Assert.assertEquals(DataType.LongType, DataStatsUtil.getDataType(SqlStatement.Type.LONG));
        Assert.assertEquals(DataType.BooleanType,
                DataStatsUtil.getDataType(SqlStatement.Type.BOOLEAN));
    }
}
