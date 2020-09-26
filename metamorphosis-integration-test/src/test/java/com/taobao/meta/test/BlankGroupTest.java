package com.taobao.meta.test;

import org.junit.Assert;

import org.junit.Test;

import com.taobao.metamorphosis.exception.InvalidConsumerConfigException;

/**
 * meta���ɲ���_groupΪ��
 * 
 * @author gongyangyu(gongyangyu@taobao.com)
 * 
 */

public class BlankGroupTest extends BaseMetaTest {

	private final String topic = "meta-test";

	@Test
	public void sendConsume() throws Exception {
		createProducer();
		// ��Ҫ����topic
		producer.publish(this.topic);
		try {
			// ������δָ������
			createConsumer2();
			Assert.fail();

			// ������Ϣ
			final int count = 5;
			sendMessage(count, "hello", this.topic);

			// ���Ľ�����Ϣ����֤������ȷ
			subscribe(this.topic, 1024 * 1024, count);

		} catch (Exception e) {
			Assert.assertTrue(e instanceof InvalidConsumerConfigException);
			System.out.println("fuck");
			Assert.assertTrue(e.getMessage().indexOf("Blank group") != -1);
		} finally {
			producer.shutdown();
			if (null != consumer) {
				consumer.shutdown();
			}
		}

	}

}
