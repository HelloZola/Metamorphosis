package com.taobao.meta.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.taobao.metamorphosis.Message;
import com.taobao.metamorphosis.client.consumer.ConsumerConfig;


/**
 * ��ʵ�����ƫ����λ�ÿ�ʼ������Ϣ
 * 
 * @author �޻�
 * @since 2011-11-14 ����5:03:52
 */

public class ComsumeFromMaxOffsetTest extends BaseMetaTest {
    private final String topic = "meta-test";


    @Test
    public void sendConsume() throws Exception {
        this.createProducer();
        producer.publish(this.topic);

        try {
            // ����֮ǰ�ȷ��ͼ�����Ϣ
            int count = 5;
            this.sendMessage(count, "hello", this.topic);

            Thread.sleep(1000);// �ȴ������ˢ��

            ConsumerConfig consumerConfig = new ConsumerConfig("group1");
            consumerConfig.setConsumeFromMaxOffset();// ������֮ǰ������5��
            this.consumer = this.sessionFactory.createConsumer(consumerConfig);

            this.subscribe(this.topic, 1024 * 1024, 0);

            count = 6;
            this.sendMessage(count, "haha", this.topic);// ����֮����Ϣ

            this.subscribeRepeatable(this.topic, 1024 * 1024, count);

            // ��֤�յ���ȫ�����Ƕ���֮�����Ϣhaha
            assertEquals(count, this.queue.size());
            for (Message msg : this.queue) {
                assertTrue(new String(msg.getData()).contains("haha"));
            }
        }
        finally {
            producer.shutdown();
            consumer.shutdown();
        }
    }
}
