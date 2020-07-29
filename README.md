# zookeeper-lock-demo
---
## 简介
    演示了基于zookeeper实现的在分布式的高并发场景下，简单实现的分布式锁。
    项目依赖zookeeper，所以需要先在本地安装zookeeper环境。
    在Server项目中需要配置：
      1.zookeeper的地址和超时时间
      2.shop.path，来标识需要消费的库存数量，这个需要现在zk中创建（当然你也可以选择连接Redis进行存储）。
---   
## 调试 
    我选择了jmeter工具进行了并发调试，将server项目修改端口号多服务启动（我启动了3个）。
    可以在每一个启动的实例中，看到当前被消费后剩余的库存量。
    测试后，每个服务不会有重复消费，并且在消费完所有库存后，后序的请求，会提示未抢到。
    实现了多实例运行情况、模仿高并发情况下的基于Zookeeper分布式锁。
