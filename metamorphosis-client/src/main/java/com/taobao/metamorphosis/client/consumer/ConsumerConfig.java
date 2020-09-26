/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Authors:
 *   wuhua <wq163@163.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.metamorphosis.client.consumer;

import com.taobao.metamorphosis.client.MetaClientConfig;


/**
 * ���������ã���Ҫ����ѡ�����£�
 * <ul>
 * <li>group:�������ƣ����룬��ʾ�����������ڷ��飬ͬһ�������������������²�������ظ���Ϣ����ͬ����ĳһtopic</li>
 * <li>consumerId: ������id������Ψһ��ʶһ�������ߣ��ɲ����ã�ϵͳ����ݷ��������Զ�����</li>
 * <li>commitOffsetPeriodInMills: ����offset��ʱ������Ĭ��5�룬��λ����</li>
 * <li>fetchTimeoutInMills: ͬ����ȡ��Ϣ��Ĭ�ϳ�ʱʱ�䣬Ĭ��10�룬��λ����</li>
 * <li>maxDelayFetchTimeInMills: ����ȡ��Ϣʧ�ܵ�ʱ�򣨰���get
 * miss�����κ��쳣���)���ӳٻ�ȡ����ֵ���������ӳ�ʱ�䣬��λ����</li>
 * <li>fetchRunnerCount: ��ȡ��Ϣ���߳�����Ĭ��cpu����</li>
 * <li>partition:��ʹ��ֱ��ģʽʱ����ֵָ�����ӵķ���������"brokerId-partition"���ַ���</li>
 * <li>offset:ָ����ȡ��offsetƫ����,Ĭ�ϴ�0��ʼ</li>
 * <li>maxFetchRetries:ͬһ����Ϣ�ڴ���ʧ�������������Դ�����Ĭ��5�Σ�����������������Ϣ����¼</li>
 * <li>maxIncreaseFetchDataRetries:��ȡ�������Դ����������ֵ,������ÿ����ȡ��������</li>
 * <li>loadBalanceStrategyType: �����߸��ؾ������</li>
 * </ul>
 * 
 * @author boyan
 * @Date 2011-4-28
 * @author wuhua
 * 
 */
public class ConsumerConfig extends MetaClientConfig {
    static final long serialVersionUID = -1L;
    private int fetchRunnerCount = Runtime.getRuntime().availableProcessors();
    private long maxDelayFetchTimeInMills = 5000;
    @Deprecated
    private long maxDelayFetchTimeWhenExceptionInMills = 10000;
    private long fetchTimeoutInMills = 10000;
    private String consumerId;
    private String partition;
    private long offset = 0;
    private String group;
    private long commitOffsetPeriodInMills = 5000L;
    private int maxFetchRetries = 5;
    private boolean alwaysConsumeFromMaxOffset = false;
    private LoadBalanceStrategy.Type loadBalanceStrategyType = LoadBalanceStrategy.Type.DEFAULT;

    // ����Ϣ����ʧ�����Ը���ȡ���ݲ������Էֿ�,
    // ��Ϊ��ʱ����Ҫ����ʧ������(maxFetchRetries��ΪmaxIntValue),
    // ����Ҫ��������ȡ��������
    private int maxIncreaseFetchDataRetries = 5;


    public int getMaxFetchRetries() {
        return this.maxFetchRetries;
    }


    public boolean isAlwaysConsumeFromMaxOffset() {
        return this.alwaysConsumeFromMaxOffset;
    }


    public void setMaxFetchRetries(final int maxFetchRetries) {
        this.maxFetchRetries = maxFetchRetries;
    }


    /**
     * ��ȡ�������Դ����������ֵ,������ÿ����ȡ��������
     * 
     * @return
     */
    public int getMaxIncreaseFetchDataRetries() {
        return this.maxIncreaseFetchDataRetries;
    }


    /**
     * ������ȡ�������Դ����������ֵ,������ÿ����ȡ��������
     * 
     * @param maxFetchRetriesForDataNotEnough
     */
    public void setMaxIncreaseFetchDataRetries(final int maxFetchRetriesForDataNotEnough) {
        this.maxIncreaseFetchDataRetries = maxFetchRetriesForDataNotEnough;
    }


    /**
     * 
     * @param group
     *            ��������
     */
    public ConsumerConfig(final String group) {
        super();
        this.group = group;
    }


    /**
     * 
     * @param consumerId
     *            ������id����������ã�ϵͳ���Զ����� "ip_ʱ��"
     * @param group
     */
    public ConsumerConfig(final String consumerId, final String group) {
        super();
        this.consumerId = consumerId;
        this.group = group;
    }


    public ConsumerConfig() {
        super();
    }


    /**
     * �����߳�����Ĭ��cpus��
     * 
     * @return
     */
    public int getFetchRunnerCount() {
        return this.fetchRunnerCount;
    }


    /**
     * ����offset���
     * 
     * @return
     */
    public long getOffset() {
        return this.offset;
    }


    /**
     * ��������offset
     * 
     * @param offset
     */
    public void setOffset(final long offset) {
        this.offset = offset;
    }


    /**
     * �����״ζ����Ƿ������λ�ÿ�ʼ���ѡ�
     * 
     * @param offset
     */
    public void setConsumeFromMaxOffset() {
        this.setConsumeFromMaxOffset(false);
    }


    /**
     * ����ÿ�ζ����Ƿ������λ�ÿ�ʼ���ѡ�
     * 
     * @since 1.4.5
     * @param always
     *            ���Ϊtrue����ʾÿ��������������λ�ÿ�ʼ���ѡ�ͨ���ڲ��Ե�ʱ���������Ϊtrue��
     */
    public void setConsumeFromMaxOffset(boolean always) {
        this.alwaysConsumeFromMaxOffset = always;
        // ��������false ��Ԥ�Ʋ�һ�µ�����
        if (always) {
            this.setOffset(Long.MAX_VALUE);
        } 
    }

    /**
     * �����߷�����
     * 
     * @return
     */
    public String getGroup() {
        return this.group;
    }


    /**
     * ���������߷�����
     * 
     * @param group
     *            ������������Ϊ��
     */
    public void setGroup(final String group) {
        this.group = group;
    }


    /**
     * ����������ֱ�����ӷ�������ʱ����Ч
     * 
     * @return
     */
    public String getPartition() {
        return this.partition;
    }


    /**
     * ���÷���,����ֱ�����ӷ�������ʱ����Ч
     * 
     * @param partition
     *            ����"brokerId-partition"���ַ���
     */
    public void setPartition(final String partition) {
        this.partition = partition;
    }


    /**
     * ������id
     * 
     * @return
     */
    public String getConsumerId() {
        return this.consumerId;
    }


    /**
     * ����������id���ɲ����ã�ϵͳ������"ip_ʱ��"�Ĺ����Զ�����
     * 
     * @param consumerId
     */
    public void setConsumerId(final String consumerId) {
        this.consumerId = consumerId;
    }


    /**
     * ����ʱʱ�䣬����Ϊ��λ��Ĭ��10��
     * 
     * @return
     */
    public long getFetchTimeoutInMills() {
        return this.fetchTimeoutInMills;
    }


    /**
     * ��������ʱʱ�䣬����Ϊ��λ��Ĭ��10��
     * 
     * @param fetchTimeoutInMills
     *            ����
     */
    public void setFetchTimeoutInMills(final long fetchTimeoutInMills) {
        this.fetchTimeoutInMills = fetchTimeoutInMills;
    }


    /**
     * �����������ʱ�䣬��λ���룬Ĭ��5��
     * 
     * @return
     */
    public long getMaxDelayFetchTimeInMills() {
        return this.maxDelayFetchTimeInMills;
    }


    /**
     * ���������������ʱ�䣬��λ���룬Ĭ��5��
     * 
     * @param maxDelayFetchTimeInMills
     */
    public void setMaxDelayFetchTimeInMills(final long maxDelayFetchTimeInMills) {
        this.maxDelayFetchTimeInMills = maxDelayFetchTimeInMills;
    }


    /**
     * ���������쳣ʱ(�����޿������ӵ�),�����������ʱ�䣬��λ���룬Ĭ��10��
     * 
     * @deprecated 1.4��ʼ�ϳ�����ʹ��maxDelayFetchTimeInMills
     * @return
     */
    @Deprecated
    public long getMaxDelayFetchTimeWhenExceptionInMills() {
        return this.maxDelayFetchTimeWhenExceptionInMills;
    }


    /**
     * ���������쳣ʱ(�����޿������ӵ�),���������������ʱ�䣬��λ���룬Ĭ��10��
     * 
     * @deprecated 1.4��ʼ�ϳ�����ʹ��maxDelayFetchTimeInMills
     * @param maxDelayFetchTimeWhenExceptionInMills
     */
    @Deprecated
    public void setMaxDelayFetchTimeWhenExceptionInMills(final long maxDelayFetchTimeWhenExceptionInMills) {
        this.maxDelayFetchTimeWhenExceptionInMills = maxDelayFetchTimeWhenExceptionInMills;
    }


    /**
     * ���������߳�����Ĭ��cpus��
     * 
     * @param fetchRunnerCount
     */
    public void setFetchRunnerCount(final int fetchRunnerCount) {
        this.fetchRunnerCount = fetchRunnerCount;
    }


    /**
     * ����offset�ļ��ʱ�䣬��λ���룬Ĭ��5��
     * 
     * @return
     */
    public long getCommitOffsetPeriodInMills() {
        return this.commitOffsetPeriodInMills;
    }


    /**
     * ���ñ���offset�ļ��ʱ�䣬��λ���룬Ĭ��5��
     * 
     * @param commitOffsetPeriodInMills
     *            ����
     */
    public void setCommitOffsetPeriodInMills(final long commitOffsetPeriodInMills) {
        this.commitOffsetPeriodInMills = commitOffsetPeriodInMills;
    }


    /**
     * ��ȡ���ؾ����������
     * 
     * @return
     */
    public LoadBalanceStrategy.Type getLoadBalanceStrategyType() {
        return this.loadBalanceStrategyType;
    }


    /**
     * ���ø��ؾ����������
     * 
     * @param loadBalanceStrategyType
     */
    public void setLoadBalanceStrategyType(final LoadBalanceStrategy.Type loadBalanceStrategyType) {
        this.loadBalanceStrategyType = loadBalanceStrategyType;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.alwaysConsumeFromMaxOffset ? 1231 : 1237);
        result = prime * result + (int) (this.commitOffsetPeriodInMills ^ this.commitOffsetPeriodInMills >>> 32);
        result = prime * result + (this.consumerId == null ? 0 : this.consumerId.hashCode());
        result = prime * result + this.fetchRunnerCount;
        result = prime * result + (int) (this.fetchTimeoutInMills ^ this.fetchTimeoutInMills >>> 32);
        result = prime * result + (this.group == null ? 0 : this.group.hashCode());
        result = prime * result + (this.loadBalanceStrategyType == null ? 0 : this.loadBalanceStrategyType.hashCode());
        result = prime * result + (int) (this.maxDelayFetchTimeInMills ^ this.maxDelayFetchTimeInMills >>> 32);
        result =
                prime
                * result
                + (int) (this.maxDelayFetchTimeWhenExceptionInMills ^ this.maxDelayFetchTimeWhenExceptionInMills >>> 32);
        result = prime * result + this.maxFetchRetries;
        result = prime * result + this.maxIncreaseFetchDataRetries;
        result = prime * result + (int) (this.offset ^ this.offset >>> 32);
        result = prime * result + (this.partition == null ? 0 : this.partition.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        ConsumerConfig other = (ConsumerConfig) obj;
        if (this.alwaysConsumeFromMaxOffset != other.alwaysConsumeFromMaxOffset) {
            return false;
        }
        if (this.commitOffsetPeriodInMills != other.commitOffsetPeriodInMills) {
            return false;
        }
        if (this.consumerId == null) {
            if (other.consumerId != null) {
                return false;
            }
        }
        else if (!this.consumerId.equals(other.consumerId)) {
            return false;
        }
        if (this.fetchRunnerCount != other.fetchRunnerCount) {
            return false;
        }
        if (this.fetchTimeoutInMills != other.fetchTimeoutInMills) {
            return false;
        }
        if (this.group == null) {
            if (other.group != null) {
                return false;
            }
        }
        else if (!this.group.equals(other.group)) {
            return false;
        }
        if (this.loadBalanceStrategyType != other.loadBalanceStrategyType) {
            return false;
        }
        if (this.maxDelayFetchTimeInMills != other.maxDelayFetchTimeInMills) {
            return false;
        }
        if (this.maxDelayFetchTimeWhenExceptionInMills != other.maxDelayFetchTimeWhenExceptionInMills) {
            return false;
        }
        if (this.maxFetchRetries != other.maxFetchRetries) {
            return false;
        }
        if (this.maxIncreaseFetchDataRetries != other.maxIncreaseFetchDataRetries) {
            return false;
        }
        if (this.offset != other.offset) {
            return false;
        }
        if (this.partition == null) {
            if (other.partition != null) {
                return false;
            }
        }
        else if (!this.partition.equals(other.partition)) {
            return false;
        }
        return true;
    }

}