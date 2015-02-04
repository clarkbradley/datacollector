/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.runner.production;

import com.streamsets.pipeline.prodmanager.ShutdownObject;
import com.streamsets.pipeline.runner.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesConfigLoaderRunnable implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(RulesConfigLoaderRunnable.class);

  private volatile Thread runningThread;
  private final ShutdownObject shutdownObject;
  private final RulesConfigLoader rulesConfigLoader;
  private final Observer observer;

  public RulesConfigLoaderRunnable(ShutdownObject shutdownObject, RulesConfigLoader rulesConfigLoader,
                                   Observer observer) {
    this.shutdownObject = shutdownObject;
    this.rulesConfigLoader = rulesConfigLoader;
    this.observer = observer;
  }

  @Override
  public void run() {
    runningThread = Thread.currentThread();
    while (!shutdownObject.isStop()) {
      try {
        rulesConfigLoader.load(observer);
      } catch(InterruptedException e) {
        LOG.error("Stopping the Rules Config Loader, Reason: {}", e.getMessage());
        runningThread = null;
        return;
      } catch (Exception e) {
        LOG.error("Stopping the Rules Config Loader, Reason: {}", e.getMessage());
        e.printStackTrace();
        return;
      }
    }
  }

  public void stop() {
    Thread thread = runningThread;
    if (thread != null) {
      thread.interrupt();
      LOG.debug("Pipeline stopped, interrupting the Rules Config Loader Thread.");
    }
  }

}
