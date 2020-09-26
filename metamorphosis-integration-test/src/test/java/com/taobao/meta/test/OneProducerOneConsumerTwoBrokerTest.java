package com.taobao.meta.test;

import org.junit.Test;

/**
 * meta���ɲ���_OneProducerOneConsumerTwoBroker
 * 
 * @author gongyangyu(gongyangyu@taobao.com)
 * 
 */

public class OneProducerOneConsumerTwoBrokerTest extends BaseMetaTest {

	private final String topic = "meta-test";

	@Test
	public void sendConsume() throws Exception {
		this.startServer("server2");
		createProducer();
		producer.publish(this.topic);
		// �����߱���ָ������
		createConsumer("group1");

		try {
			// ������Ϣ
			final int count = 5;
			sendMessage(count, "hello", this.topic);

			// ���Ľ�����Ϣ����֤������ȷ
			subscribe(this.topic, 1024 * 1024, count);			
		} finally {
			producer.shutdown();
			consumer.shutdown();
		}
	}
}
