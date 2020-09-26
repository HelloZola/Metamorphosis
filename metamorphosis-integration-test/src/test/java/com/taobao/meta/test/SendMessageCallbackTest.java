package com.taobao.meta.test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.taobao.metamorphosis.Message;
import com.taobao.metamorphosis.client.producer.SendMessageCallback;
import com.taobao.metamorphosis.client.producer.SendResult;


/**
 * meta���ɲ���_OneProducerOneConsumer
 * 
 * @author gongyangyu(gongyangyu@taobao.com)
 * 
 */
public class SendMessageCallbackTest extends BaseMetaTest {

    private final String topic = "meta-test";


    @Override
    public void sendMessage(final int count, final String strdata, final String topic) throws Exception {
        this.messages = new ArrayList<Message>();
        final CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            final byte[] data = (strdata + i).getBytes();
            final Message msg = new Message(topic, data);
            this.producer.sendMessage(msg, new SendMessageCallback() {
                public void onMessageSent(final SendResult result) {
                    latch.countDown();
                    if (!result.isSuccess()) {
                        throw new RuntimeException("Send message failed:" + result.getErrorMessage());
                    }
                }


                public void onException(final Throwable e) {
                    e.printStackTrace();
                    latch.countDown();
                }
            });
            this.messages.add(msg);

        }
        latch.await();
    }


    @Test
    public void sendConsume() throws Exception {
        this.createProducer();
        this.producer.publish(this.topic);
        // �����߱���ָ������
        this.createConsumer("group1");
        try {
            // ������Ϣ
            final int count = 5;
            // ���͵������ڵ�topic
            this.sendMessage(count, "hello", this.topic);

            // ���Ľ�����Ϣ����֤������ȷ
            this.subscribe(this.topic, 1024 * 1024, count);
        }
        finally {
            this.producer.shutdown();
            this.consumer.shutdown();
        }

    }
}
