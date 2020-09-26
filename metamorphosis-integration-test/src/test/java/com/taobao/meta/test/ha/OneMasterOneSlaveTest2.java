package com.taobao.meta.test.ha;

import org.junit.After;
import org.junit.Test;

import com.taobao.meta.test.Utils;
import com.taobao.metamorphosis.EnhancedBroker;


/**
 * 
 * @author �޻�
 * @since 2011-7-12 ����04:29:02
 */

public class OneMasterOneSlaveTest2 extends HABaseMetaTest {
    private final String topic = "meta-test";
    private EnhancedBroker slaveBroker;


    @Test
    public void sendConsume() throws Exception {
        // ������һ̨master���۲��Ƿ���ȷ���ͺͽ�����Ϣ��Ȼ�����һ̨slave���鿴�����߸��ؾ���仯���Ƿ���ȷ������Ϣ��
        // ���ŷ�����Ϣ֮��ȴ�����ͬ����slave�ٹر�master,�ٹ۲���Ϣ�������;������master

        // start master
        super.startServer("server1");
        super.createProducer();
        this.producer.publish(this.topic);
        super.createConsumer("group1");

        final int count = 5;
        super.sendMessage(count, "hello", this.topic);
        super.subscribe(this.topic, 1024 * 1024, count);

        // start slave
        this.log.info("------------start slave...--------------");
        this.slaveBroker = super.startSlaveServers("slave1-1", false, true);
        super.sendMessage(count, "hello", this.topic);
        // stop master
        Thread.sleep(6000);
        this.log.info("------------stop master...--------------");
        Utils.stopServers(super.brokers);
        super.subscribeRepeatable(this.topic, 1024 * 1024, count * 2);

        // start master again
        this.log.info("------------start master again...--------------");
        super.startServer("server1", false, false);
        Thread.sleep(2000);
        super.sendMessage(count, "hello", this.topic);
        super.subscribeRepeatable(this.topic, 1024 * 1024, count * 3);
        this.log.info("------------end--------------");
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
