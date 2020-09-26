package com.taobao.meta.test;

import org.junit.Test;


/**
 * ���Ա�����������Ϣ
 * 
 * @author boyan(boyan@taobao.com)
 * @date 2011-8-31
 * 
 */
public class LocalTxTenProducerTenConsumerTenGroupTest extends BaseMetaTest {

    private final String topic = "meta-test";


    @Test
    public void sendConsume() throws Exception {

        this.create_nProducer(10);

        try {
            // ������Ϣ
            final int count = 5;
            this.localTxSendMessage_nProducer(count, "hello", this.topic, 10);
            // ���Ľ�����Ϣ����֤������ȷ
            this.subscribe_nConsumer(this.topic, 1024 * 1024, count, 10, 10);
        }
        catch (final Throwable e) {
            e.printStackTrace();
        }
        finally {
            for (int i = 0; i < 10; i++) {
                this.producerList.get(i).shutdown();
                this.consumerList.get(i).shutdown();
            }
        }
    }
}
