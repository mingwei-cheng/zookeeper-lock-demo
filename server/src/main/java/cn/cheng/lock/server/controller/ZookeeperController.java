package cn.cheng.lock.server.controller;

import cn.cheng.lock.server.lock.ZookeeperLock;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author Cheng Mingwei
 * @create 2020-07-28 22:28
 **/
@RestController
public class ZookeeperController {

    @Resource(name = "zkClient")
    ZooKeeper zooKeeper;

    @Value("${shop.path}")
    private String shopPath;

    @GetMapping("/shop")
    public String shopPhone() {
        ZookeeperLock zookeeperLock = new ZookeeperLock(zooKeeper);
        //获取到了，才需要finally去释放锁
        try {
            //加锁
            zookeeperLock.lock();
            //取出phone的剩余数量
            byte[] data = zooKeeper.getData(shopPath, false, new Stat());
            if (data.length == 0) {
                System.out.println("没抢到...");
                return "没抢到...";
            }
            String s = new String(data);
            //取出Zookeeper中存储的phone的数量
            int phone = Integer.parseInt(s);
            //假如还有剩余
            if (phone > 0) {
                //消费一个
                int newNumber = phone - 1;
                //将消费完的phone的数量，重新放到zookeeper中
                zooKeeper.setData(shopPath, Integer.toString(newNumber).getBytes(), -1);
                System.out.println("恭喜抢到啦！" + newNumber);
                return "恭喜抢到啦！";
            }
        } catch (Exception e) {
            System.err.println("没抢到...");
        } finally {
            //释放锁
            zookeeperLock.unlock();
        }
        System.out.println("没抢到...");
        return "没抢到...";
    }
}
