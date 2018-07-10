/**
 * Copyright (c) 2016. Qubole Inc
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
package com.qubole.rubix.tests;

import com.qubole.rubix.core.utils.DataGen;
import com.qubole.rubix.core.utils.DeleteFileVisitor;
import com.qubole.rubix.spi.BlockLocation;
import com.qubole.rubix.spi.BookKeeperFactory;
import com.qubole.rubix.spi.CacheConfig;
import com.qubole.rubix.spi.Location;
import com.qubole.rubix.spi.RetryingBookkeeperClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.thrift.shaded.TException;
import org.apache.thrift.shaded.transport.TTransportException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class TestThriftServerJVM extends Configured
{
  private static final String testDirectoryPrefix = System.getProperty("java.io.tmpdir") + "/TestThriftServerJVM/";
  private static String backendFileName = testDirectoryPrefix + "backendDataFile";
  private static Path backendPath = new Path("file:///" + backendFileName.substring(1));

  private static final String testDirectory = testDirectoryPrefix + "dir0";
  private static final Log log = LogFactory.getLog(TestThriftServerJVM.class.getName());

  private static final String jarsPath = "/usr/lib/hadoop2/share/hadoop/tools/lib/";
  private static final String hadoopDirectory = "/usr/lib/hadoop2/bin/hadoop";
  private static final String bookKeeperClass = "com.qubole.rubix.bookkeeper.BookKeeperServer";
  private static final String localDataTransferServerClass = "com.qubole.rubix.bookkeeper.BookKeeperServer";
  private static final String setDataBlockSize = "-Dhadoop.cache.data.block-size=200";
  private static final String setCacheMaxDisks = "-Dhadoop.cache.data.max.disks=2";

  public BookKeeperFactory bookKeeperFactory = new BookKeeperFactory();
  public RetryingBookkeeperClient client;

  static Process bookKeeperJvm;
  static Process localDataTransferJvm;

  private static Configuration conf = new Configuration();
  private static String cacheDir = CacheConfig.getCacheDirPrefixList(conf) + "0" + CacheConfig.getCacheDataDirSuffix(conf);

  @BeforeClass
  public static void setupClass() throws IOException, InterruptedException
  {
    /*
     * Dynamically, retrieving bookkeeper jar from /usr/lib/hadoop2/share/hadoop/tools/lib/ folder
     * */

    File folder = new File(jarsPath);
    File[] listOfFiles = folder.listFiles();
    String bookKeeperJarPath = null;
    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile() && listOfFiles[i].toString().contains("bookkeeper")) {
        bookKeeperJarPath = listOfFiles[i].toString();
      }
    }
    log.debug(" Located Bookkeeper Jar is : " + bookKeeperJarPath);
    /*
     * Spinning up the separate JVMs for bookKeeper and Local Data Transfer
     * */
    String[] bookKeeperStartCmd = {hadoopDirectory, "jar", bookKeeperJarPath, bookKeeperClass, setDataBlockSize, setCacheMaxDisks};
    String[] localDataTransferStartCmd = {hadoopDirectory, "jar", bookKeeperJarPath, localDataTransferServerClass, setDataBlockSize, setCacheMaxDisks};

    ProcessBuilder pJVMBuilder = new ProcessBuilder();
    pJVMBuilder.redirectErrorStream(true);
    pJVMBuilder.inheritIO();
    pJVMBuilder.command(bookKeeperStartCmd);
    bookKeeperJvm = pJVMBuilder.start();
    pJVMBuilder.command(localDataTransferStartCmd);
    localDataTransferJvm = pJVMBuilder.start();

    log.info("Test Directory : " + testDirectory);
    Files.createDirectories(Paths.get(testDirectory));
    Thread.sleep(3000);
  }

  @AfterClass
  public static void tearDownClass() throws IOException
  {
    /* ****
      Destroying bookKeeper and localDataTransfer JVMs
    **** */
    bookKeeperJvm.destroy();
    localDataTransferJvm.destroy();

    log.info("Deleting files in " + testDirectory);
    Files.walkFileTree(Paths.get(testDirectory), new DeleteFileVisitor());
    Files.deleteIfExists(Paths.get(testDirectory));

    Files.walkFileTree(Paths.get(cacheDir + "/" + testDirectoryPrefix), new DeleteFileVisitor());
    Files.deleteIfExists(Paths.get(cacheDir + "/" + testDirectoryPrefix));
  }

  @BeforeMethod
  public static void setup() throws IOException, InterruptedException, URISyntaxException
  {
    DataGen.populateFile(backendFileName);
    log.info("BackendPath: " + backendPath);

    CacheConfig.setIsStrictMode(conf, true);
    CacheConfig.setCacheDataDirPrefix(conf, testDirectoryPrefix + "dir");
    CacheConfig.setMaxDisks(conf, 2);
    CacheConfig.setIsParallelWarmupEnabled(conf, false);
    CacheConfig.setBlockSize(conf, 200);
  }

  @AfterMethod
  public static void cleanup()
  {
    File file = new File(backendFileName);
    file.delete();
  }

  @Test(enabled = false)
  public void testJVMCommunication() throws IOException, InterruptedException
  {
    log.info("Value of Path " + this.backendFileName);
    log.debug(" backendPath to string : " + this.backendPath.toString());

    String host = "localhost";
    boolean dataDownloaded;
    File file = new File(backendFileName);
    List<BlockLocation> result;
    int lastBlock = 4;
    int readSize = 1000;

    try {
      client = bookKeeperFactory.createBookKeeperClient(host, conf);

      result = client.getCacheStatus("file:///" + backendFileName, file.length(), file.lastModified(), 0, lastBlock, 3);
      assertTrue(result.get(0).getLocation() == Location.LOCAL, "File already cached, before readData call");
      log.info(" Value of Result : " + result);
      log.info("Downloading file from path : " + file.toString());
      dataDownloaded = client.readData("file:///" + backendFileName, 0, readSize, file.length(), file.lastModified(), 3);
      if (!dataDownloaded) {
        log.info("Failed to read Data from the location");
      }
      result = client.getCacheStatus("file:///" + backendFileName, file.length(), file.lastModified(), 0, lastBlock, 3);
      assertTrue(result.get(0).getLocation() == Location.CACHED, "File not cached properly");
      log.info(" Value of Result : " + result);
    }
    catch (TTransportException ex) {
      log.error("Error while creating bookkeeper client");
    }
    catch (TException ex) {
      log.error("Error while invoking getCacheStatus");
    }
  }
}
