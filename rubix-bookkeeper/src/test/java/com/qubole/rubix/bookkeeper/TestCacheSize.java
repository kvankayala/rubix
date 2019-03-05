/**
 * Copyright (c) 2018. Qubole Inc
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. See accompanying LICENSE file.
 */
package com.qubole.rubix.bookkeeper;

import com.codahale.metrics.MetricRegistry;
import com.qubole.rubix.bookkeeper.BookKeeper;
import com.qubole.rubix.bookkeeper.utils.DiskUtils;
import com.qubole.rubix.common.utils.DataGen;
import com.qubole.rubix.common.utils.DeleteFileVisitor;
import com.qubole.rubix.core.LocalFSInputStream;
import com.qubole.rubix.core.ReadRequest;
import com.qubole.rubix.core.RemoteReadRequestChain;
import com.qubole.rubix.spi.CacheUtil;
import com.qubole.rubix.spi.ClusterType;
import com.qubole.rubix.spi.CacheConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.thrift.shaded.TException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

//import javax.security.auth.login.Configuration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import org.apache.hadoop.fs.FileSystem;

import static org.testng.Assert.assertTrue;

/**
 * Created by kvankayala on 21/1/19.
 */
public class TestCacheSize
{
  private FSDataInputStream fsDataInputStream;
  private String testDirectory = System.getProperty("java.io.tmpdir") + "/TestCacheSize/";
  private String backendFileName = testDirectory + "testBackendFile";
  private File backendFile = new File(backendFileName);
  private String localFileName = testDirectory + "testLocalFile";
  private RemoteReadRequestChain remoteReadRequestChain;
  private final Configuration conf = new Configuration();
  private MetricRegistry metrics;
  private static final Log log = LogFactory.getLog(TestCacheSize.class);
  private BookKeeper bookKeeper;
  private static final long TEST_LAST_MODIFIED = 1514764800; // 2018-01-01T00:00:00
  private FileSystem fs = null;

  @BeforeMethod
  public void setup() throws IOException
  {
    System.setProperty("hadoop.home.dir", "/");
    CacheConfig.setMaxDisks(conf,1);
    // Populate File
    metrics = new MetricRegistry();
    bookKeeper = new CoordinatorBookKeeper(conf, metrics);
    log.info("value of testDirectory : " + CacheUtil.getLocalPath(backendFileName, conf));
    Files.createDirectories(Paths.get(testDirectory));
    DataGen.populateFile(backendFileName, 1, 10000000);
    //LocalFSInputStream localFSInputStream = new LocalFSInputStream(backendFileName);
    //log.info("value of localpath : " + CacheUtil.getLocalPath(backendFileName, conf));
    //Files.createDirectories(Paths.get(CacheUtil.getLocalPath(backendFileName, conf)));
    //log.info("value of mdfilepath : " + CacheUtil.getMetadataFilePath(backendFileName, conf));
    //Files.createDirectories(Paths.get(CacheUtil.getMetadataFilePath(backendFileName, conf)));
    //fsDataInputStream = new FSDataInputStream(localFSInputStream);
    //fsDataInputStream = new FSDataInputStream(fis);
    //remoteReadRequestChain = new RemoteReadRequestChain(fsDataInputStream, localFileName);
  }

  @Test
  public void testGetCacheDirSizeinMBs() throws IOException
  {
    //byte[] buffer = new byte[2500000];
    //ReadRequest[] readRequests = {
    //    new ReadRequest(100000000, 102500000, 100000000, 102500000, buffer, 0, backendFile.length())};
    String generatedTestData = DataGen.getExpectedOutput(2500000, 10000000);

    //testCacheSizeAfterRead(readRequests, generatedTestData);
    testCacheSizeAfterRead(generatedTestData);
  }

  //private void testCacheSizeAfterRead(ReadRequest[] readRequests, String expectedBufferOutput)
  private void testCacheSizeAfterRead(String expectedBufferOutput)
          throws IOException
  {
    //for (ReadRequest rr : readRequests) {
    //  remoteReadRequestChain.addReadRequest(rr);
    //}
    //remoteReadRequestChain.lock();
    // 2. Execute and verify that buffer has right data
    //int readSize = remoteReadRequestChain.call();
    final String remotePathWithScheme = "file://" + backendFileName;
    try {
      bookKeeper.readData(remotePathWithScheme, (long)100000000, 2500000, 102500000, TEST_LAST_MODIFIED, ClusterType.TEST_CLUSTER_MANAGER.ordinal());
    }
    catch (TException e) {
      log.info("IN Exception");
    }
    //assertTrue(readSize == expectedBufferOutput.length(), "Wrong amount of data read " + readSize + " was expecting " + expectedBufferOutput.length());

    // 3. read from randomAccessFile and verify that it has the right data
    //         0 - 100000000 : should be filled with '0'
    // 100000000 - 102500000 : should be filled with 'k'
    byte[] buffer = new byte[260000000];
    FileInputStream localFileInputStream = new FileInputStream(new File("/media/ephemeral0/fcache" + backendFileName));
    localFileInputStream.read(buffer, 0, 100000000);
    localFileInputStream.read(buffer, 100000001, 102500000);
    for (int i = 0; i <= 90000000; i++) {
      assertTrue(buffer[i] == 0, "Got data instead of hole: " + buffer[i]);
    }
    for (int i = 100000001; i <= 102500000; i++) {
      assertTrue(buffer[i] == 'k', "Got " + buffer[i] + " at " + i + "instead of 'k'");
    }
    localFileInputStream.close();
    //long fileSize = DiskUtils.getDirSizeMB(new File(localFileName));
    long fileSize = DiskUtils.getDirSizeMB(new File("/media/ephemeral0/fcache" + backendFileName));
    log.info("file size : " + fileSize);
    assertTrue(fileSize == 3, "getDirSizeMB is reporting wrong file Size");
  }

  //@AfterMethod
  public void cleanup()
          throws IOException
  {
    fsDataInputStream.close();
    backendFile.delete();
    File localFile = new File(localFileName);
    localFile.delete();
    Files.walkFileTree(Paths.get(testDirectory), new DeleteFileVisitor());
    Files.deleteIfExists(Paths.get(testDirectory));
  }
}
