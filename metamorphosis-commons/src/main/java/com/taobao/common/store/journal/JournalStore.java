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
 *   dogun (yuexuqiang at gmail.com)
 */
package com.taobao.common.store.journal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import com.taobao.common.store.Store;
import com.taobao.common.store.journal.impl.ConcurrentIndexMap;
import com.taobao.common.store.journal.impl.LRUIndexMap;
import com.taobao.common.store.util.BytesKey;
import com.taobao.common.store.util.Util;


/**
 * <b>һ��ͨ����־�ļ�ʵ�ֵ�key/value�ԵĴ洢</b>
 * 
 * key������16�ֽ� <br />
 * 1�������ļ�����־�ļ���һ�𣬲���¼�����ļ�<br />
 * name.1 name.1.log<br />
 * 2��dataΪ���������ݣ�˳���ţ�ʹ�����ü���<br />
 * 3��logΪ����+key+ƫ����<br />
 * 4����������ʱ��������name.1�����offset��length��Ȼ���¼��־���������ü�����Ȼ����������ڴ�����<br />
 * 5��ɾ������ʱ����¼��־��ɾ���ڴ������������ļ��������жϴ�С�Ƿ������С�ˣ������������ˣ���ɾ�������ļ�����־�ļ�<br />
 * 6����ȡ����ʱ��ֱ�Ӵ��ڴ������������ƫ����<br />
 * 7����������ʱ����������<br />
 * 8������ʱ������ÿһ��log�ļ���ͨ����־�Ĳ����ָ��ڴ�����<br />
 * 
 * @author dogun (yuexuqiang at gmail.com)
 */
public class JournalStore implements Store, JournalStoreMBean {
    private final Log log = LogFactory.getLog(JournalStore.class);

    public static final int FILE_SIZE = 1024 * 1024 * 64; // 20M
    // public static final int ONE_DAY = 1000 * 60 * 60 * 24;
    public static final int HALF_DAY = 1000 * 60 * 60 * 12;
    protected static final int DEFAULT_MAX_BATCH_SIZE = 1024 * 1024 * 4;
    private final String path;
    private final String name;
    private final boolean force;

    protected IndexMap indices;
    private final Map<BytesKey, Long> lastModifiedMap = new ConcurrentHashMap<BytesKey, Long>();
    public Map<Integer, DataFile> dataFiles = new ConcurrentHashMap<Integer, DataFile>();
    protected Map<Integer, LogFile> logFiles = new ConcurrentHashMap<Integer, LogFile>();

    protected DataFile dataFile = null;
    protected LogFile logFile = null;
    private DataFileAppender dataFileAppender = null;
    private final AtomicInteger number = new AtomicInteger(0);
    private long intervalForCompact = HALF_DAY;
    private long intervalForRemove = HALF_DAY * 2 * 7;
    private volatile ScheduledExecutorService scheduledPool;

    private volatile long maxFileCount = Long.MAX_VALUE;

    protected int maxWriteBatchSize = DEFAULT_MAX_BATCH_SIZE;

    public static class InflyWriteData {
        public volatile byte[] data;
        public volatile int count;


        public InflyWriteData(final byte[] data) {
            super();
            this.data = data;
            this.count = 1;
        }

    }


    /**
     * Ĭ�Ϲ��캯��������path��ʹ��name��Ϊ�������������ļ�
     * 
     * @param path
     * @param name
     * @param force
     * @throws IOException
     */
    public JournalStore(final String path, final String name, final boolean force, final boolean enabledIndexLRU)
            throws IOException {
        this(path, name, null, force, enabledIndexLRU, false);
    }


    /**
     * �Լ�ʵ�� ����ά�����
     * 
     * @param path
     * @param name
     * @param indices
     * @param force
     * @param enabledIndexLRU
     * @throws IOException
     */
    public JournalStore(final String path, final String name, final IndexMap indices, final boolean force,
            final boolean enabledIndexLRU) throws IOException {
        this(path, name, indices, force, enabledIndexLRU, false);
    }


    /**
     * 
     * @param path
     * @param name
     * @param force
     * @param enableIndexLRU
     * @param enabledDataFileCheck
     * @throws IOException
     */
    public JournalStore(final String path, final String name, final boolean force, final boolean enableIndexLRU,
            final boolean enabledDataFileCheck) throws IOException {
        this(path, name, null, force, enableIndexLRU, false);
    }


    /**
     * ���������ļ������Ĺ��캯��
     * 
     * @param path
     * @param name
     * @param force
     * @throws IOException
     */
    public JournalStore(final String path, final String name, final IndexMap indices, final boolean force,
            final boolean enableIndexLRU, final boolean enabledDataFileCheck) throws IOException {
        Util.registMBean(this, name);
        this.path = path;
        this.name = name;
        this.force = force;
        if (indices == null) {
            if (enableIndexLRU) {
                final long maxMemory = Runtime.getRuntime().maxMemory();
                // Ĭ��ʹ������ڴ��1/40���洢������Ŀǰֻ�ǹ���ֵ����Ҫ����
                final int capacity = (int) (maxMemory / 40 / 60);
                this.indices =
                        new LRUIndexMap(capacity, this.getPath() + File.separator + name + "_indexCache",
                            enableIndexLRU);
            }
            else {
                this.indices = new ConcurrentIndexMap();
            }
        }
        else {
            this.indices = indices;
        }
        this.dataFileAppender = new DataFileAppender(this);

        this.initLoad();
        // �����ǰû�п����ļ�������
        if (null == this.dataFile || null == this.logFile) {
            this.newDataFile();
        }

        // ����һ����ʱ�̣߳���Store4j�������ļ����ڽ�������.
        if (enabledDataFileCheck) {
            this.scheduledPool = Executors.newSingleThreadScheduledExecutor();
            this.scheduledPool.scheduleAtFixedRate(new DataFileCheckThread(), this.calcDelay(), HALF_DAY,
                TimeUnit.MILLISECONDS);
            log.warn("���������ļ���ʱ�����߳�");
        }

        // ��Ӧ�ñ��رյ�ʱ��,���û�йر��ļ�,�ر�֮.��ĳЩ����ϵͳ����
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    JournalStore.this.close();
                }
                catch (final IOException e) {
                    log.error("close error", e);
                }
            }
        });
    }


    /**
     * Ĭ�Ϲ��캯��������path��ʹ��name��Ϊ�������������ļ�
     * 
     * @param path
     * @param name
     * @throws IOException
     */
    public JournalStore(final String path, final String name) throws IOException {
        this(path, name, false, false);
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.Store#add(byte[], byte[])
     */
    @Override
    public void add(final byte[] key, final byte[] data) throws IOException {
        this.add(key, data, false);
    }


    @Override
    public void add(final byte[] key, final byte[] data, final boolean force) throws IOException {
        // �ȼ���Ƿ��Ѿ����ڣ�����Ѿ������׳��쳣 �ж��ļ��Ƿ����ˣ�����name.1�����offset����¼��־���������ü��������������ڴ�����
        this.checkParam(key, data);
        this.innerAdd(key, data, -1, force);

    }


    @Override
    public boolean remove(final byte[] key, final boolean force) throws IOException {
        return this.innerRemove(key, force);
    }


    /**
     * �������������ļ����ܹ��������ļ�����.
     * 
     * @param key
     * @throws IOException
     */
    private void reuse(final byte[] key, final boolean sync) throws IOException {
        final byte[] value = this.get(key);
        final long oldLastTime = this.lastModifiedMap.get(new BytesKey(key));
        if (value != null && this.remove(key)) {
            this.innerAdd(key, value, oldLastTime, sync);
        }
    }


    /**
     * �����¸�ִ�����ڵ�delayʱ��.
     * 
     * @return
     */
    private long calcDelay() {
        final Calendar date = new GregorianCalendar();
        date.setTime(new Date());
        final long currentTime = date.getTime().getTime();

        date.set(Calendar.HOUR_OF_DAY, 6);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);

        long delay = date.getTime().getTime() - currentTime;
        // ��������6�㣬���������6��ʱ��
        if (delay < 0) {
            date.set(Calendar.HOUR_OF_DAY, 18);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            delay = date.getTime().getTime() - currentTime;
            // ��������6��
            if (delay < 0) {
                delay += HALF_DAY;
            }
        }
        return delay;
    }


    /**
     * �ڲ���������
     * 
     * @param key
     * @param data
     * @throws IOException
     */
    private OpItem innerAdd(final byte[] key, final byte[] data, final long oldLastTime, final boolean sync)
            throws IOException {
        final BytesKey k = new BytesKey(key);
        final OpItem op = new OpItem();
        op.op = OpItem.OP_ADD;
        this.dataFileAppender.store(op, k, data, sync);
        this.indices.put(k, op);
        if (oldLastTime == -1) {
            this.lastModifiedMap.put(k, System.currentTimeMillis());
        }
        else {
            this.lastModifiedMap.put(k, oldLastTime);
        }
        return op;
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.Store#get(byte[])
     */
    @Override
    public byte[] get(final byte[] key) throws IOException {
        byte[] data = null;
        final BytesKey bytesKey = new BytesKey(key);
        data = this.dataFileAppender.getDataFromInFlyWrites(bytesKey);
        if (data != null) {
            return data;
        }
        final OpItem op = this.indices.get(bytesKey);
        if (null != op) {

            final DataFile df = this.dataFiles.get(Integer.valueOf(op.number));
            if (null != df) {
                final ByteBuffer bf = ByteBuffer.wrap(new byte[op.length]);
                df.read(bf, op.offset);
                data = bf.array();
            }
            else {
                log.warn("�����ļ���ʧ��" + op);
                this.indices.remove(bytesKey);
                this.lastModifiedMap.remove(bytesKey);
            }
        }

        return data;
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.Store#iterator()
     */
    @Override
    public Iterator<byte[]> iterator() throws IOException {
        final Iterator<BytesKey> it = this.indices.keyIterator();
        return new Iterator<byte[]>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }


            @Override
            public byte[] next() {
                final BytesKey bk = it.next();
                if (null != bk) {
                    return bk.getData();
                }
                return null;
            }


            @Override
            public void remove() {
                throw new UnsupportedOperationException("��֧��ɾ������ֱ�ӵ���store.remove����");
            }
        };
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.Store#remove(byte[])
     */
    @Override
    public boolean remove(final byte[] key) throws IOException {
        return this.remove(key, false);
    }


    /**
     * ��ü�¼���Ǹ��ļ�����¼��־��ɾ���ڴ������������ļ��������жϴ�С�Ƿ������С�ˣ������������ˣ���ɾ�������ļ�����־�ļ�
     * 
     * @param key
     * @return �Ƿ�ɾ��������
     * @throws IOException
     */
    private boolean innerRemove(final byte[] key, final boolean sync) throws IOException {
        boolean ret = false;
        final BytesKey k = new BytesKey(key);
        final OpItem op = this.indices.get(k);
        if (null != op) {
            ret = this.innerRemove(op, k, sync);
            if (ret) {
                this.indices.remove(k);
                this.lastModifiedMap.remove(k);
            }
        }
        return ret;
    }


    /**
     * ����OpItem��������־�ļ��м�¼ɾ���Ĳ�����־�������޸Ķ�Ӧ�����ļ������ü���.
     * 
     * @param op
     * @return
     * @throws IOException
     */
    private boolean innerRemove(final OpItem op, final BytesKey bytesKey, final boolean sync) throws IOException {
        final DataFile df = this.dataFiles.get(Integer.valueOf(op.number));
        final LogFile lf = this.logFiles.get(Integer.valueOf(op.number));
        if (null != df && null != lf) {
            final OpItem o = new OpItem();
            o.key = op.key;
            o.length = op.length;
            o.number = op.number;
            o.offset = op.offset;
            o.op = OpItem.OP_DEL;
            this.dataFileAppender.remove(o, bytesKey, sync);
            return true;
        }
        return false;
    }


    /**
     * �������Ƿ�Ϸ�
     * 
     * @param key
     * @param data
     */
    private void checkParam(final byte[] key, final byte[] data) {
        if (null == key || null == data) {
            throw new NullPointerException("key/data can't be null");
        }
        if (key.length != 16) {
            throw new IllegalArgumentException("key.length must be 16");
        }
    }


    /**
     * ����һ���µ������ļ�
     * 
     * @throws FileNotFoundException
     */
    protected DataFile newDataFile() throws IOException {
        if (this.dataFiles.size() > this.maxFileCount) {
            throw new RuntimeException("���ֻ�ܴ洢" + this.maxFileCount + "�������ļ�");
        }
        final int n = this.number.incrementAndGet();
        this.dataFile = new DataFile(new File(this.path + File.separator + this.name + "." + n), n, this.force);
        this.logFile = new LogFile(new File(this.path + File.separator + this.name + "." + n + ".log"), n, this.force);
        this.dataFiles.put(Integer.valueOf(n), this.dataFile);
        this.logFiles.put(Integer.valueOf(n), this.logFile);
        log.info("�������ļ���" + this.dataFile);
        return this.dataFile;
    }


    /**
     * Create the parent directory if it doesn't exist.
     */
    private void checkParentDir(final File parent) {
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Can't make dir " + this.path);
        }
    }


    /**
     * ���ʼ����ʱ����Ҫ�������е���־�ļ����ָ��ڴ������
     * 
     * @throws IOException
     */
    private void initLoad() throws IOException {
        log.warn("��ʼ�ָ�����");
        final String nm = this.name + ".";
        final File dir = new File(this.path);
        this.checkParentDir(dir);
        final File[] fs = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String n) {
                return n.startsWith(nm) && !n.endsWith(".log");
            }
        });
        if (fs == null || fs.length == 0) {
            return;
        }
        log.warn("����ÿ�������ļ�");
        final List<Integer> indexList = new LinkedList<Integer>();
        for (final File f : fs) {
            try {
                final String fn = f.getName();
                final int n = Integer.parseInt(fn.substring(nm.length()));
                indexList.add(Integer.valueOf(n));
            }
            catch (final Exception e) {
                log.error("parse file index error" + f, e);
            }
        }

        Integer[] indices = indexList.toArray(new Integer[indexList.size()]);

        // ���ļ�˳���������
        Arrays.sort(indices);

        for (final Integer n : indices) {
            log.warn("����indexΪ" + n + "���ļ�");
            // ���汾�����ļ���������Ϣ
            final Map<BytesKey, OpItem> idx = new HashMap<BytesKey, OpItem>();
            // ����dataFile��logFile
            final File f = new File(dir, this.name + "." + n);
            final DataFile df = new DataFile(f, n, this.force);
            final LogFile lf = new LogFile(new File(f.getAbsolutePath() + ".log"), n, this.force);
            final long size = lf.getLength() / OpItem.LENGTH;

            for (int i = 0; i < size; ++i) { // ѭ��ÿһ������
                final ByteBuffer bf = ByteBuffer.wrap(new byte[OpItem.LENGTH]);
                lf.read(bf, i * OpItem.LENGTH);
                if (bf.hasRemaining()) {
                    log.warn("log file error:" + lf + ", index:" + i);
                    continue;
                }
                final OpItem op = new OpItem();
                op.parse(bf.array());
                final BytesKey key = new BytesKey(op.key);
                switch (op.op) {
                case OpItem.OP_ADD: // ��������ӵĲ����������������������ü���
                    final OpItem o = this.indices.get(key);
                    if (null != o) {
                        // �Ѿ���֮ǰ���ӹ�����ô��Ȼ��Update��ʱ��Remove�Ĳ�����־û��д�롣

                        // д��Remove��־
                        this.innerRemove(o, key, true);

                        // ��map��ɾ��
                        this.indices.remove(key);
                        this.lastModifiedMap.remove(key);
                    }
                    boolean addRefCount = true;
                    if (idx.get(key) != null) {
                        // ��ͬһ���ļ���add����update������ôֻ�Ǹ������ݣ������������ü�����
                        addRefCount = false;
                    }

                    idx.put(key, op);

                    if (addRefCount) {
                        df.increment();
                    }
                    break;

                case OpItem.OP_DEL: // �����ɾ���Ĳ���������ȥ�����������ü���
                    idx.remove(key);
                    df.decrement();
                    break;

                default:
                    log.warn("unknow op:" + (int) op.op);
                    break;
                }
            }
            if (df.getLength() >= FILE_SIZE && df.isUnUsed()) { // �����������ļ��Ѿ��ﵽָ����С�����Ҳ���ʹ�ã�ɾ��
                df.delete();
                lf.delete();
                log.warn("�����ˣ�Ҳ�����˴�С��ɾ��");
            }
            else { // �������map
                this.dataFiles.put(n, df);
                this.logFiles.put(n, lf);
                if (!df.isUnUsed()) { // ���������������������
                    this.indices.putAll(idx);
                    // ��������������־�ļ�������޸�ʱ��,����û�б�Ҫ�ǳ���ȷ.
                    final long lastModified = lf.lastModified();
                    for (final BytesKey key : idx.keySet()) {
                        this.lastModifiedMap.put(key, lastModified);
                    }
                    log.warn("����ʹ�ã�����������referenceCount:" + df.getReferenceCount() + ", index:" + idx.size());
                }
            }
        }
        // У����ص��ļ��������õ�ǰ�ļ�
        if (this.dataFiles.size() > 0) {
            indices = this.dataFiles.keySet().toArray(new Integer[this.dataFiles.keySet().size()]);
            Arrays.sort(indices);
            for (int i = 0; i < indices.length - 1; i++) {
                final DataFile df = this.dataFiles.get(indices[i]);
                if (df.isUnUsed() || df.getLength() < FILE_SIZE) {
                    throw new IllegalStateException("�ǵ�ǰ�ļ���״̬�Ǵ��ڵ����ļ��鳤�ȣ�������used״̬");
                }
            }
            final Integer n = indices[indices.length - 1];
            this.number.set(n.intValue());
            this.dataFile = this.dataFiles.get(n);
            this.logFile = this.logFiles.get(n);
        }
        log.warn("�ָ����ݣ�" + this.size());
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.Store#size()
     */
    @Override
    public int size() {
        return this.indices.size();
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.Store#update(byte[], byte[])
     */
    @Override
    public boolean update(final byte[] key, final byte[] data) throws IOException {
        // ����Update����Ϣ������д��OpCodeΪUpdate����־��
        final BytesKey k = new BytesKey(key);
        final OpItem op = this.indices.get(k);
        if (null != op) {
            this.indices.remove(k);
            final OpItem o = this.innerAdd(key, data, -1, false);
            if (o.number != op.number) {
                // ����ͬһ���ļ��ϸ��£��Ž���ɾ����
                this.innerRemove(op, k, false);
            }
            else {
                // ͬһ���ļ��ϸ��£�����DataFile���ã���Ϊadd��ʱ�������
                final DataFile df = this.dataFiles.get(Integer.valueOf(op.number));
                df.decrement();
            }
            return true;
        }
        return false;
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.journal.JournalStoreMBean#getDataFilesInfo()
     */
    @Override
    public String getDataFilesInfo() {
        return this.dataFiles.toString();
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.journal.JournalStoreMBean#getLogFilesInfo()
     */
    @Override
    public String getLogFilesInfo() {
        return this.logFiles.toString();
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.journal.JournalStoreMBean#getNumber()
     */
    @Override
    public int getNumber() {
        return this.number.get();
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.journal.JournalStoreMBean#getPath()
     */
    @Override
    public String getPath() {
        return this.path;
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.journal.JournalStoreMBean#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.journal.JournalStoreMBean#getDataFileInfo()
     */
    @Override
    public String getDataFileInfo() {
        return this.dataFile.toString();
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.journal.JournalStoreMBean#getLogFileInfo()
     */
    @Override
    public String getLogFileInfo() {
        return this.logFile.toString();
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.journal.JournalStoreMBean#viewIndexMap()
     */
    @Override
    public String viewIndexMap() {
        return this.indices.toString();
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.Store#close()
     */
    @Override
    public void close() throws IOException {
        this.sync();
        for (final DataFile df : this.dataFiles.values()) {
            try {
                df.close();
            }
            catch (final Exception e) {
                log.warn("close error:" + df, e);
            }
        }
        this.dataFiles.clear();
        for (final LogFile lf : this.logFiles.values()) {
            try {
                lf.close();
            }
            catch (final Exception e) {
                log.warn("close error:" + lf, e);
            }
        }
        this.logFiles.clear();
        this.indices.close();
        this.lastModifiedMap.clear();
        this.dataFile = null;
        this.logFile = null;
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.common.store.journal.JournalStoreMBean#getSize()
     */
    @Override
    public long getSize() throws IOException {
        return this.size();
    }


    @Override
    public long getIntervalForCompact() {
        return this.intervalForCompact;
    }


    @Override
    public void setIntervalForCompact(final long intervalForCompact) {
        this.intervalForCompact = intervalForCompact;
    }


    @Override
    public long getIntervalForRemove() {
        return this.intervalForRemove;
    }


    @Override
    public void setIntervalForRemove(final long intervalForRemove) {
        this.intervalForRemove = intervalForRemove;
    }


    @Override
    public long getMaxFileCount() {
        return this.maxFileCount;
    }


    @Override
    public void setMaxFileCount(final long maxFileCount) {
        this.maxFileCount = maxFileCount;
    }


    public void sync() {
        this.dataFileAppender.sync();
    }


    /**
     * �������ļ����м�飬��������Ӧ�Ĵ�����
     * 
     * 1.���ݳ���ָ����Removeʱ��,����ֱ��ɾ�� 2.���ݳ���ָ����Compactʱ�䣬��Remove��Add
     * 
     * @throws IOException
     */
    @Override
    public void check() throws IOException {
        final Iterator<byte[]> keys = this.iterator();
        BytesKey key = null;
        final long now = System.currentTimeMillis();
        long time;
        log.warn("Store4j�����ļ�������ʼ...");
        while (keys.hasNext()) {
            key = new BytesKey(keys.next());
            time = this.lastModifiedMap.get(key);
            if (this.intervalForRemove != -1 && now - time > this.intervalForRemove) {
                this.innerRemove(key.getData(), true);
            }
            else if (now - time > this.intervalForCompact) {
                this.reuse(key.getData(), true);
            }
        }
        log.warn("Store4j�����ļ��������...");
    }

    /**
     * �����ļ����ĺ�̨�̣߳���ҪĿ����Ϊ��Store4j�����ļ����������Ĺ������£�
     */
    class DataFileCheckThread implements Runnable {

        @Override
        public void run() {
            try {
                JournalStore.this.check();
            }
            catch (final Exception ex) {
                log.warn("check error:", ex);
            }
        }
    }
}