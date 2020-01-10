/**
 * Copyright (c) 2019. Qubole Inc
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
import com.qubole.rubix.bookkeeper.exception.BookKeeperInitializationException;
import com.qubole.rubix.bookkeeper.exception.CoordinatorInitializationException;
import com.qubole.rubix.common.metrics.BookKeeperMetrics;
import com.qubole.rubix.common.utils.TestUtil;
import com.qubole.rubix.core.utils.DummyClusterManager;
import com.qubole.rubix.hadoop2.Hadoop2ClusterManager;
import com.qubole.rubix.presto.PrestoClusterManager;
import com.qubole.rubix.spi.CacheConfig;
import com.qubole.rubix.spi.ClusterManager;
import com.qubole.rubix.spi.ClusterType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertTrue;

public class TestClusterManager
{
  private static final Log log = LogFactory.getLog(TestBookKeeper.class);

  private static final String TEST_CACHE_DIR_PREFIX = TestUtil.getTestCacheDirPrefix("TestBookKeeper");
  private static final String TEST_DNE_CLUSTER_MANAGER = "com.qubole.rubix.core.DoesNotExistClusterManager";
  private static final int TEST_MAX_DISKS = 1;
  private static final int TEST_BLOCK_SIZE = 100;

  private final Configuration conf = new Configuration();
  private MetricRegistry metrics;

  private CoordinatorBookKeeper bookKeeper;
  BookKeeperMetrics bookKeeperMetrics;

  @BeforeMethod
  public void setUp() throws IOException, BookKeeperInitializationException
  {
    CacheConfig.setCacheDataDirPrefix(conf, TEST_CACHE_DIR_PREFIX);
    CacheConfig.setBlockSize(conf, TEST_BLOCK_SIZE);

    TestUtil.createCacheParentDirectories(conf, TEST_MAX_DISKS);

    metrics = new MetricRegistry();
    bookKeeperMetrics = new BookKeeperMetrics(conf, metrics);
    bookKeeper = new CoordinatorBookKeeper(conf, bookKeeperMetrics);
  }

  @AfterMethod
  public void tearDown() throws Exception
  {
    TestUtil.removeCacheParentDirectories(conf, TEST_MAX_DISKS);
    bookKeeperMetrics.close();
    conf.clear();
  }

  @Test
  public void testGetDummyClusterManagerValidInstance() throws Exception
  {
    ClusterType type = ClusterType.TEST_CLUSTER_MANAGER;
    ClusterManager manager = bookKeeper.getClusterManagerInstance(type, conf);

    assertTrue(manager instanceof DummyClusterManager, " Didn't initialize the correct cluster manager class." +
        " Expected : " + DummyClusterManager.class + " Got : " + manager.getClass());
  }

  @Test(expectedExceptions = CoordinatorInitializationException.class)
  public void testGetDummyClusterManagerInValidInstance() throws Exception
  {
    ClusterType type = ClusterType.TEST_CLUSTER_MANAGER;
    CacheConfig.setDummyClusterManager(conf, TEST_DNE_CLUSTER_MANAGER);

    ClusterManager manager = bookKeeper.getClusterManagerInstance(type, conf);
  }

  @Test
  public void testGetHadoop2ClusterManagerValidInstance() throws Exception
  {
    ClusterType type = ClusterType.HADOOP2_CLUSTER_MANAGER;
    ClusterManager manager = bookKeeper.getClusterManagerInstance(type, conf);

    assertTrue(manager instanceof Hadoop2ClusterManager, " Didn't initialize the correct cluster manager class." +
        " Expected : " + Hadoop2ClusterManager.class + " Got : " + manager.getClass());
  }

  @Test(expectedExceptions = CoordinatorInitializationException.class)
  public void testGetHadoop2ClusterManagerInValidInstance() throws Exception
  {
    ClusterType type = ClusterType.HADOOP2_CLUSTER_MANAGER;
    CacheConfig.setHadoopClusterManager(conf, TEST_DNE_CLUSTER_MANAGER);

    ClusterManager manager = bookKeeper.getClusterManagerInstance(type, conf);
  }

  @Test
  public void testGetPrestoClusterManagerValidInstance() throws Exception
  {
    ClusterType type = ClusterType.PRESTO_CLUSTER_MANAGER;
    ClusterManager manager = bookKeeper.getClusterManagerInstance(type, conf);

    assertTrue(manager instanceof PrestoClusterManager, " Didn't initialize the correct cluster manager class." +
        " Expected : " + PrestoClusterManager.class + " Got : " + manager.getClass());
  }

  @Test(expectedExceptions = CoordinatorInitializationException.class)
  public void testGetPrestoClusterManagerInValidInstance() throws Exception
  {
    ClusterType type = ClusterType.PRESTO_CLUSTER_MANAGER;
    CacheConfig.setPrestoClusterManager(conf, TEST_DNE_CLUSTER_MANAGER);

    ClusterManager manager = bookKeeper.getClusterManagerInstance(type, conf);
  }
}
