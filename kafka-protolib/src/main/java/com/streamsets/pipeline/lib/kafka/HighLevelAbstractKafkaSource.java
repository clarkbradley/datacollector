/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.lib.kafka;

import com.streamsets.pipeline.api.BatchMaker;
import com.streamsets.pipeline.api.ChooserMode;
import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.Label;
import com.streamsets.pipeline.api.OffsetCommitter;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.ValueChooser;
import com.streamsets.pipeline.api.base.BaseSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

@ConfigGroups(HighLevelAbstractKafkaSource.HighLevelKafkaSourceConfigGroups.class)
public abstract class HighLevelAbstractKafkaSource extends BaseSource implements OffsetCommitter {

  private static final Logger LOG = LoggerFactory.getLogger(HighLevelAbstractKafkaSource.class);
  private static final String DOT = ".";

  @ConfigDef(required = true,
    type = ConfigDef.Type.STRING,
    description = "The Consumer Group columnName",
    label = "Consumer Group",
    defaultValue = "streamsetsDataCollector")
  public String consumerGroup;

  @ConfigDef(required = true,
    type = ConfigDef.Type.STRING,
    description = "The Kafka topic from which the messages must be read",
    label = "Topic",
    defaultValue = "topicName")
  public String topic;

  @ConfigDef(required = true,
    type = ConfigDef.Type.STRING,
    description = "Comma separated zookeeper connect strings.",
    label = "Zookeeper Connect String",
    defaultValue = "localhost:2181")
  public String zookeeperConnect;

  @ConfigDef(required = true,
    type = ConfigDef.Type.INTEGER,
    description = "The maximum number of messages to be read from Kafka for one batch.",
    label = "Max Batch Size",
    defaultValue = "1000")
  public int maxBatchSize;

  @ConfigDef(required = true,
    type = ConfigDef.Type.INTEGER,
    description = "The maximum time in milli seconds for which the Kafka consumer waits to fill a batch.",
    label = "Batch Duration Time",
    defaultValue = "1000")
  public int maxWaitTime;

  @ConfigDef(required = true,
    type = ConfigDef.Type.MODEL,
    description = "Type of data sent as kafka message payload",
    label = "Payload Type",
    defaultValue = "LOG")
  @ValueChooser(type = ChooserMode.PROVIDED, chooserValues = ConsumerPayloadTypeChooserValues.class)
  public ConsumerPayloadType consumerPayloadType;

  @ConfigDef(required = false,
    type = ConfigDef.Type.MAP,
    description = "Additional configuration properties which will be used by the underlying Kafka consumer.",
    defaultValue = "",
    group = "ADVANCED",
    label = "Advanced Configuration")
  public Map<String, String> kafkaConsumerConfigs;


  public enum HighLevelKafkaSourceConfigGroups implements Label {
    ADVANCED("Advanced Properties");

    private final String label;

    private HighLevelKafkaSourceConfigGroups(String label) {
      this.label = label;
    }

    public String getLabel() {
      return this.label;
    }
  }

  private HighLevelKafkaConsumer kafkaConsumer;

  @Override
  public void init() throws StageException {
    kafkaConsumer = new HighLevelKafkaConsumer(zookeeperConnect, topic, consumerGroup, maxBatchSize, maxWaitTime,
      kafkaConsumerConfigs, getContext());
    kafkaConsumer.init();
    LOG.info("Successfully initialized Kafka Consumer");
  }

  @Override
  public String produce(String lastSourceOffset, int maxBatchSize, BatchMaker batchMaker) throws StageException {
    LOG.debug("Reading messages from kafka.");
    List<MessageAndOffset> messages = kafkaConsumer.read(maxBatchSize);
    int recordCounter = 0;
    for(MessageAndOffset message : messages) {
      recordCounter++;
      Record record = getContext().createRecord(topic + DOT + message.getPartition() + DOT + System.currentTimeMillis()
        + DOT + recordCounter);
      record.set(createField(message.getPayload()));
      batchMaker.addRecord(record);
    }
    LOG.info("Produced {} records in this batch.", recordCounter);
    return lastSourceOffset;
  }

  @Override
  public void destroy() {
    LOG.info("Destroying Kafka Consumer");
    kafkaConsumer.destroy();
    super.destroy();
  }

  @Override
  public void commit(String offset) throws StageException {
    LOG.info("Committing offset for topic.");
    kafkaConsumer.commit();
  }

  protected abstract Field createField(byte[] bytes) throws StageException;

}
