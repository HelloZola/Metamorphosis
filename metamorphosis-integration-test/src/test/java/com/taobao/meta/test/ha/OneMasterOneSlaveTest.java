package com.taobao.meta.test.ha;

import org.junit.After;
import org.junit.Test;

import com.taobao.metamorphosis.EnhancedBroker;


/**
 * 
 * @author �޻�
 * @since 2011-7-12 ����02:31:37
 */

public class OneMasterOneSlaveTest extends HABaseMetaTest {
    private final String topic = "meta-test";
    private EnhancedBroker slaveBroker;


    @Test
    public void sendConsume() throws Exception {
        // ������һ̨master���۲��Ƿ���ȷ���ͺͽ�����Ϣ��Ȼ�����һ̨slave���鿴�����߸��ؾ���仯���Ƿ���ȷ������Ϣ��
        // Ȼ��ر�slave,�ٹ۲�;������slave�۲�

        // start master
        super.startServer("server1");
        super.createProducer();
        this.producer.publish(this.topic);
        super.createConsumer("group1");

        final int count = 5;
        super.sendMessage(count, "hello", this.topic);
        super.subscribe(this.topic, 1024 * 1024, count);

        // start slave
        this.slaveBroker = super.startSlaveServers("slave1-1", false, true);
        this.log.info("------------slave started--------------");
        super.sendMessage(count, "hello", this.topic);
        super.subscribeRepeatable(this.topic, 1024 * 1024, count * 2);

        // stop slave
        this.slaveBroker.stop();
        this.log.info("------------slave stop--------------");
        super.sendMessage(count, "hello", this.topic);
        super.subscribeRepeatable(this.topic, 1024 * 1024, count * 3);

        // start slave again
        this.slaveBroker = super.startSlaveServers("slave1-1", false, false);
        this.log.info("------------slave started--------------");
        super.sendMessage(count, "hello", this.topic);
        super.subscribeRepeatable(this.topic, 1024 * 1024, count * 4);
        Thread.sleep(3000);
    }


    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (this.slaveBroker != null) {
            this.slaveBroker.stop();
        }
    }
}
