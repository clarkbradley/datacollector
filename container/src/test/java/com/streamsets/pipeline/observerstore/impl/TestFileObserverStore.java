/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.observerstore.impl;

import com.google.common.collect.ImmutableList;
import com.streamsets.pipeline.main.RuntimeInfo;
import com.streamsets.pipeline.util.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;

public class TestFileObserverStore {

  private static final String PIPELINE_NAME = "myPipeline";
  private static final String PIPELINE_REV = "0";

  private FileSamplingStore observerStore = null;

  @BeforeClass
  public static void beforeClass() {
    System.setProperty(RuntimeInfo.DATA_DIR, "./target/var");
  }

  @AfterClass
  public static void afterClass() {
    System.getProperties().remove(RuntimeInfo.DATA_DIR);
  }

  @Before
  public void setUp() throws IOException {
    File f = new File(System.getProperty(RuntimeInfo.DATA_DIR));
    FileUtils.deleteDirectory(f);
    RuntimeInfo info = new RuntimeInfo(ImmutableList.of(getClass().getClassLoader()));
    observerStore = new FileSamplingStore(info, new Configuration());
  }

  @After
  public void tearDown() {

  }



}
