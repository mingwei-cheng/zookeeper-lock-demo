package cn.cheng.lock.server.config;

import jdk.nashorn.internal.runtime.logging.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CountDownLatch;

/**
 * @author Cheng Mingwei
 * @create 2020-07-28 20:57
 **/
@Logger
@Configuration
public class ZookeeperConfig {

    @Value("${zookeeper.address}")
    private String connectString;

    @Value("${zookeeper.timeout}")
    private int timeout;


    @Bean(name = "zkClient")
    public ZooKeeper zkClient() {
        ZooKeeper zooKeeper = null;
        try {
            //创建等待连接
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            zooKeeper = new ZooKeeper(connectString, timeout, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    countDownLatch.countDown();
                }
            });
            //zookeeper登录方式：1.world 2.auth 3.digest 4.ip
            //zooKeeper.addAuthInfo("digest", "password".getBytes());
            //等待连接成功
            countDownLatch.await();
            System.out.println("Zookeeper连接成功：" + zooKeeper.getState());
            Stat exists = zooKeeper.exists("/LOCKS", false);
            if (exists == null) {
                zooKeeper.create("/LOCKS", new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (Exception e) {
            System.err.println("Zookeeper连接失败：" + e);
        }
        return zooKeeper;
    }

}
