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
package com.qubole.rubix.core;

import com.qubole.rubix.common.utils.DeleteFileVisitor;
import com.qubole.rubix.spi.CacheConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;

public class TestUpdateConfInCachingFileSystem
{
  private static final Log log = LogFactory.getLog(TestUpdateConfInCachingFileSystem.class);
  private static final String testDirectory = System.getProperty("java.io.tmpdir") + "TestUpdateConfInCachingFileSystem/";
  private static String rubixSiteXmlName = testDirectory + "rubix-site.xml";
  private static final String TEST_BACKEND_FILE = testDirectory + "backendFile";
  private static Configuration conf = new Configuration();

  Path backendFilePath = new Path("file:///" + TEST_BACKEND_FILE.substring(1));

  @BeforeClass
  public static void setupClass() throws IOException, InterruptedException
  {
    log.info("test Directory : " + testDirectory);
    log.info("Xml File : " + rubixSiteXmlName);
    Files.createDirectories(Paths.get(testDirectory));
    CacheConfig.setKeyRubixSiteLocation(conf, rubixSiteXmlName);
    Document dom;
    DocumentBuilderFactory bdf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = bdf.newDocumentBuilder();
      dom = db.newDocument();
      Element confElement = dom.createElement("configuration");
      Element propertyElement = dom.createElement("property");
      Element nameElement = dom.createElement("name");
      Text item = dom.createTextNode("rubix.qubole.team");
      nameElement.appendChild(item);
      Element valueElement = dom.createElement("value");
      item = dom.createTextNode("set");
      valueElement.appendChild(item);
      propertyElement.appendChild(nameElement);
      propertyElement.appendChild(valueElement);
      confElement.appendChild(propertyElement);
      dom.appendChild(confElement);
      try {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(dom), new StreamResult(new FileOutputStream(rubixSiteXmlName)));
      }
      catch (Exception e) {
        log.error("Error in Transforming dom to xml file : " + e.getMessage());
      }
    }
    catch (Exception e) {
      log.error("Error in filling the dom : " + e.getMessage());
    }
  }

  @AfterClass
  public static void tearDownClass() throws IOException, InterruptedException
  {
    log.info("Deleting files in " + testDirectory);
    Files.walkFileTree(Paths.get(testDirectory), new DeleteFileVisitor());
    Files.deleteIfExists(Paths.get(testDirectory));
  }

  @Test
  public void testUpdateConf_pickHadoopOverrideConf() throws IOException
  {
    MockCachingFileSystem mFs = new MockCachingFileSystem();
    conf.set("rubix.qubole.team", "unset");
    mFs.singletonCounter = 0;
    // Start FS with conf.set("rubix.qubole.team", "unset") with is similar conf getting set in hadoop overrides
    mFs.initialize(backendFilePath.toUri(), conf);
    mFs.updateConf(conf);
    Configuration lconf = CachingFileSystem.localConf;
    assertEquals(lconf.get("rubix.qubole.team"), "unset", "Configuration is returning wrong value");
    assertEquals(lconf.get("qubole.team"), null, "Configuration is returning wrong value");
  }

  @Test
  public void testUpdateConf_pickConfFromRubixSiteXml() throws IOException
  {
    MockCachingFileSystem mFs = new MockCachingFileSystem();
    mFs.singletonCounter = 0;
    mFs.initialize(backendFilePath.toUri(), conf);
    mFs.updateConf(conf);
    Configuration lconf = CachingFileSystem.localConf;
    assertEquals(lconf.get("rubix.qubole.team"), "set", "Configuration is returning wrong value");
    assertEquals(lconf.get("qubole.team"), null, "Configuration is returning wrong value");
  }
}
