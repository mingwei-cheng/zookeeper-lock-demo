package cn.cheng.lock.server.lock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 实现Lock接口实现我们需要用的lock、tryLock、unLock三个方法
 *
 * @author Cheng Mingwei
 * @create 2020-07-28 21:55
 **/
public class ZookeeperLock implements Lock {

    private final ZooKeeper zooKeeper;

    /**
     * 不托管给Spring，让这个类可以new
     */
    public ZookeeperLock(ZooKeeper zookeeper) {
        this.zooKeeper = zookeeper;
    }

    /**
     * 锁的根节点
     */
    private final String ROOT_LOCK_DIR = "/LOCKS";
    /**
     * 获得锁时创建的临时节点
     */
    private String currentLock = null;
    /**
     * 未获得锁时，需要等待哪个需要释放的节点
     */
    private String waitLock = null;
    /**
     * 用于处理等待节点的通知
     */
    private CountDownLatch latch;

    /**
     * 加锁
     */
    @Override
    public void lock() {
        //tryLock返回true则表示获得锁
        if (this.tryLock()) {
            System.out.println(Thread.currentThread().getName() + "==>" + currentLock + " 获得锁...");
        } else {
            //反之则需要等待waitLock节点释放
            try {
                //注册waitLock的Watcher，当该节点删除时，唤醒latch
                Stat stat = zooKeeper.exists(waitLock, event -> {
                    if (latch != null) {
                        latch.countDown();
                    }
                });
                if (stat != null) {
                    System.out.println(Thread.currentThread().getName() + "==>等待锁==>" + waitLock + " 释放...");
                    //等待waitLock删除后被唤醒
                    latch = new CountDownLatch(1);
                    latch.await();
                    //唤醒即表示成功获得锁
                    System.out.println(Thread.currentThread().getName() + "==>" + currentLock + " 获得锁...");
                }
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 尝试获取锁
     *
     * @return 是否获取成功
     */
    @Override
    public boolean tryLock() {
        try {
            //尝试获取锁,使用自增的临时节点
            currentLock = zooKeeper.create(ROOT_LOCK_DIR + "/", new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            System.out.println(Thread.currentThread().getName() + "==>" + currentLock + " 尝试获得锁...");
            //获取ROOT节点下的所有子节点
            List<String> nodes = zooKeeper.getChildren(ROOT_LOCK_DIR, false);
            //判断是否获得锁，即判断自己的节点是不是最小的
            SortedSet<String> set = new TreeSet<String>();
            nodes.forEach((node) -> {
                set.add(ROOT_LOCK_DIR + "/" + node);
            });
            String first = set.first();
            //如果最小序号的节点就是自己创建的节点，则表示自己获得了锁
            if (currentLock.equals(first)) {
                return true;
            }
            //否则获得当前所有节点中序号比自己小的节点
            SortedSet<String> less = set.headSet(currentLock);
            if (!less.isEmpty()) {
                //获得所有比自己小的节点中最大的一个，设置为等待锁，即这个节点删除时，自己就可以获得锁
                waitLock = less.last();
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 解锁
     */
    @Override
    public void unlock() {
        try {
            System.out.println(Thread.currentThread().getName() + " ==> " + currentLock + " 释放锁...");
            //删除临时节点
            zooKeeper.delete(currentLock, -1);
            currentLock = null;
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return tryLock();
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
