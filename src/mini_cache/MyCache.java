package mini_cache;

import mini_cache.computable.Computable;
import mini_cache.computable.ExpensiveFunction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * 描述：  用ConcurrentHashMap保证线程安全，但是细细想来还有一个重复计算的问题，而且应该频率不低
 */
public class MyCache<A, V> implements Computable<A, V> {

    private final Map<A, Future<V>> cache = new ConcurrentHashMap<>();

    private final Computable<A, V> c;

    public MyCache(Computable<A, V> c) {
        this.c = c;
    }

    @Override
    public V compute(A arg) throws Exception {
        Future<V> f = cache.get(arg);
        if (f == null) {   //虽然概率小 但是这儿其实还是拦不住多线程并发访问的
            FutureTask<V> ft = new FutureTask<>(()->c.compute(arg));//lambda表达式就是一个实现了callable接口的任务实例
            f = ft;
            cache.put(arg, ft);   //先put后run
            System.out.println("正在执行FutureTask中的callable任务...");
            ft.run();
        }
        return f.get();
    }

    public static void main(String[] args) throws Exception {
        MyCache<String, Integer> expensiveComputer = new MyCache<>(new ExpensiveFunction());

        new Thread(() -> {
            try {
                Integer result = expensiveComputer.compute("666");
                System.out.println("第一次的计算结果："+result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(100);
        new Thread(() -> {
            try {
                Integer result = expensiveComputer.compute("667");
                System.out.println("第二次的计算结果："+result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(100);
        new Thread(() -> {
            try {
                Integer result = expensiveComputer.compute("666");
                System.out.println("第三次的计算结果："+result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}


