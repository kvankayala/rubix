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

package com.qubole.rubix.health;

import com.qubole.rubix.spi.BookKeeperFactory;
import com.qubole.rubix.spi.RetryingBookkeeperClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

/**
* Created by kvankayala on 16 Dec 2018.
*/

public class BookKeeperHealth extends Configured
{
  RetryingBookkeeperClient client;
  private static Configuration conf = new Configuration();
  private static final Log log = LogFactory.getLog(BookKeeperHealth.class);
  private BookKeeperFactory factory;

  public BookKeeperHealth(Configuration conf)
  {
    this.conf = conf;
    factory = new BookKeeperFactory();
  }

  public static void main(String[]args)
  {
    BookKeeperHealth obj = new BookKeeperHealth(conf);
    boolean isBookKeeperAlive = obj.run();
    if (isBookKeeperAlive == false) {
      Runtime.getRuntime().exit(1);
    }
    else {
      Runtime.getRuntime().exit(0);
    }
    return;
  }

  public boolean run()
  {
    String host = "localhost";
    try {
      client = factory.createBookKeeperClient(host, conf);
      return client.isBookKeeperWorking();
    }
    catch (Exception e) {
      log.error("Failed to create BookKeeper client", e);
    }
    return false;
  }
}
