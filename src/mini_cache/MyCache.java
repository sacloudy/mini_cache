package mini_cache;

import mini_cache.computable.Computable;
import mini_cache.computable.ExpensiveFunction;
import mini_cache.computable.MayFail;
import sun.font.TrueTypeFont;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 描述： 出于安全性考虑，缓存需要设置有效期，到期自动失效，否则如果缓存一直不失效，那么带来缓存不一致等问题
 */
public class MyCache<A, V> implements Computable<A, V> {

    private final Map<A, Future<V>> cache = new ConcurrentHashMap<>();

    private final Computable<A, V> c;

    public MyCache(Computable<A, V> c) {
        this.c = c;
    }

    @Override
    public V compute(A arg) throws InterruptedException {
        while(true){
            Future<V> f = cache.get(arg);
            if (f == null) {
                FutureTask<V> ft = new FutureTask<>(() -> c.compute(arg));
                f = cache.putIfAbsent(arg, ft); //使用原子操作保证原子性
                if (f == null) {   //再来一层过滤
                    f = ft;
                    System.out.println("正在执行FutureTask中的callable任务...");
                    ft.run();
                }
            }
            try {
                return f.get();            //异常要处理的
            } catch (CancellationException e) {
                System.out.println("被取消了");
                cache.remove(arg);
                throw e;
            } catch (InterruptedException e) {
                cache.remove(arg);
                throw e;
            } catch (ExecutionException e) {
                System.out.println("计算错误，需要重试");
                cache.remove(arg); //如果没有的话一定要移除这个空的FutureTask否则以后的计算也都会抛异常
            }
        }
    }

    //要增加缓存过期功能了，重载了compute方法
    public final static ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);

    public V compute(A arg, long expiretime) throws ExecutionException, InterruptedException {
        if (expiretime>0)   //如果超时时间大于0，用线程池帮我们做延迟的工作
            scheduledThreadPool.schedule(() -> expire(arg), expiretime, TimeUnit.MILLISECONDS);//给线程池传入任务和延迟时间
        return compute(arg);
    }

    public synchronized void expire(A key) {
        Future<V> future = cache.get(key);
        if (future != null) {
            if (!future.isDone()) {    //缓存有效时间到了任务还没有执行结束
                System.out.println("Future任务因过期被取消");
                future.cancel(true);
            }
            System.out.println("过期时间到，缓存被清除");
            cache.remove(key);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        MyCache<String, Integer> expensiveComputer = new MyCache<>(new MayFail());

        new Thread(() -> {
            try {
                Integer result = expensiveComputer.compute("666",5000L);
                System.out.println("第一次的计算结果："+result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                Integer result = expensiveComputer.compute("667");
                System.out.println("第二次的计算结果："+result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                Integer result = expensiveComputer.compute("666");
                System.out.println("第三次的计算结果："+result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(6000);
        Integer result = expensiveComputer.compute("666");
        System.out.println("主线程的计算结果：" + result);
    }
}


