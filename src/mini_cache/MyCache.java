package mini_cache;

import mini_cache.computable.Computable;
import mini_cache.computable.MayFail;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 描述： 出于安全性考虑，缓存需要设置有效期，到期自动失效，否则如果缓存一直不失效，那么带来缓存不一致等问题
 */
public class MyCache<String, Integer> implements Computable<String, Integer> {

    private final Map<String, Future<Integer>> cache = new ConcurrentHashMap<>();

    private final Computable<String, Integer> c;

    public MyCache(Computable<String, Integer> c) {
        this.c = c;
    }

    @Override
    public Integer compute(String key) throws InterruptedException {
        while(true){
            Future<Integer> f = cache.get(key);
            if (f == null) {
                FutureTask<Integer> ft = new FutureTask<>(() -> c.compute(key));
                f = cache.putIfAbsent(key, ft); //使用原子操作保证原子性
                if (f == null) {   //这就是双重检查的意思了
                    f = ft;
                    System.out.println("正在执行FutureTask中的callable任务...");
                    ft.run();
                }
            }
            try {
                return f.get();      //如果ft里面的任务执行成功就直接返回，其中任务抛异常则ft.get也会抛出来
            } catch (CancellationException e) {
                System.out.println("被取消了");
                cache.remove(key);
                throw e;
            } catch (InterruptedException e) {
                cache.remove(key);
                throw e;
            } catch (ExecutionException e) {
                System.out.println("计算错误，需要重试");
                cache.remove(key); //如果没有的话一定要移除这个空的FutureTask否则以后的计算也都会抛异常
            }
        }
    }

    //增加LRU缓存淘汰策略..............
    //public String get(String key){
    //        Node node = cache.get(key);
    //        if(node==null){
    //            return null;
    //        }
    //        //删除原节点
    //        //将更新后的节点插入链表头部
    //    }
    //
    //    public void put(String key,String value){
    //        Node node = catch.get(key);
    //        if(node==null){
    //            if (hashMap.size()> limit){
    //                //移除链表尾节点
    //                hashMap.remove(key);
    //            }
    //            node = new Node(key,value);
    //            //插入链表头部
    //            hashMap.put(key,node);
    //        }else{
    //            node.value=value;//更新node
    //            //移除该节点
    //            //插入更新后的链表插入链表头部
    //        }
    //    }


    //要增加缓存过期功能了，重载了compute方法,这是装饰者模式吗哈哈(调用人家原来的compute了还)
    public final static ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);

    public Integer compute(String arg, long expireTime) throws InterruptedException {
        //时间一到，这个线程池立马可以执行传给它的任务
        scheduledThreadPool.schedule(() -> expire(arg), expireTime, TimeUnit.MILLISECONDS);//给线程池传入任务和延迟执行时间

        return compute(arg);
    }

    public synchronized void expire(String key) {
        Future<Integer> future = cache.get(key);
        if (future != null) {
            if (!future.isDone()) {    //缓存有效时间到了任务还没有执行结束
                System.out.println("Future任务因过期被取消");
                future.cancel(true);
            }
            System.out.println("过期时间到，缓存被清除");
            cache.remove(key);
        }
    }


    public static void main(java.lang.String[] args) throws InterruptedException {

        MyCache<java.lang.String, java.lang.Integer> expensiveComputer = new MyCache<>(new MayFail());

        new Thread(() -> {
            try { //进去就交给scheduledThreadPool开始倒计时了，然后调用正常的compute方法正常计算就行，有点残酷哈
                java.lang.Integer result = expensiveComputer.compute("666",5000L);
                System.out.println("第一次的计算结果："+result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                java.lang.Integer result = expensiveComputer.compute("667");
                System.out.println("第二次的计算结果："+result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                java.lang.Integer result = expensiveComputer.compute("666");
                System.out.println("第三次的计算结果："+result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(6000);
        java.lang.Integer result = expensiveComputer.compute("666");
        System.out.println("主线程的计算结果：" + result);
    }
}


