package com.taobao.meta.test.ha;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;

import com.taobao.meta.test.Utils;
import com.taobao.metamorphosis.EnhancedBroker;


/**
 * 
 * @author �޻�
 * @since 2011-7-12 ����04:28:29
 */
// note:һ̨serverͻȻ�رպ���������쳣��������,ʵ�������и��ؾ���֮�����ͻ���ʧ��
public class OneMasterTwoSlaveTest extends HABaseMetaTest {

    private final String topic = "meta-test";
    private EnhancedBroker slaveBroker1;
    private EnhancedBroker slaveBroker2;


    @Test
    public void sendConsume() throws Exception {
        // ������һ̨master���۲��Ƿ���ȷ���ͺͽ�����Ϣ��Ȼ�����һ̨slave���鿴�����߸��ؾ���仯���Ƿ���ȷ������Ϣ��
        // �ٹ���һ̨slave
        // Ȼ��ر�slave,�ٹ۲�;������slave�۲�

        // start master
        super.startServer("server1");
        super.createProducer();
        this.producer.publish(this.topic);
        super.createConsumer("group1");

        final int count = 5;
        super.sendMessage(count, "hello", this.topic);
        super.subscribe(this.topic, 1024 * 1024, count);

        // start slave1
        this.slaveBroker1 = super.startSlaveServers("slave1-1", false, true);
        this.log.info("------------slave1 started--------------");
        super.sendMessage(count, "hello", this.topic);
        super.subscribeRepeatable(this.topic, 1024 * 1024, count * 2);

        // start slave2
        this.slaveBroker2 = super.startSlaveServers("slave1-2", false, true);
        this.log.info("------------slave2 started--------------");
        super.sendMessage(count, "hello", this.topic);
        super.subscribeRepeatable(this.topic, 1024 * 1024, count * 3);

        // stop slave1
        this.slaveBroker1.stop();
        this.log.info("------------slave1 stop--------------");
        super.sendMessage(count, "hello", this.topic);
        super.subscribeRepeatable(this.topic, 1024 * 1024, count * 4);

        // start slave1 again
        this.slaveBroker1 = super.startSlaveServers("slave1-1", false, false);
        this.log.info("------------slave1 started again--------------");
        super.sendMessage(count, "hello", this.topic);
        super.subscribeRepeatable(this.topic, 1024 * 1024, count * 5);

        Thread.sleep(3000);

    }


    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Utils.stopWrapperServers(Arrays.asList(this.slaveBroker1, this.slaveBroker2));
    }
}
