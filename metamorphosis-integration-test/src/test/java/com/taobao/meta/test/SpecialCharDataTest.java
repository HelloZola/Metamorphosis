package com.taobao.meta.test;

import org.junit.Test;

/**
 * meta���ɲ���_�����ַ�����
 * 
 * @author gongyangyu(gongyangyu@taobao.com)
 * 
 */

public class SpecialCharDataTest extends BaseMetaTest {

	private final String topic = "meta-test";

	@Test
	public void sendConsume() throws Exception {
		createProducer();
		producer.publish(this.topic);
		// �����߱���ָ������
		createConsumer("group1");

		try {
			// ������Ϣ
			final int count = 5;
			sendMessage2(count, "~!@#$%", this.topic);

			// ���Ľ�����Ϣ����֤������ȷ
			subscribe(this.topic, 1024 * 1024, count);
		} finally {
			producer.shutdown();
			consumer.shutdown();
		}

	}
}
