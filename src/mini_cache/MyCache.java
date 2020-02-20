package mini_cache;

import mini_cache.computable.Computable;
import mini_cache.computable.ExpensiveFunction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：  用ConcurrentHashMap保证线程安全，但是细细想来还有一个重复计算的问题，而且应该频率不低
 */
public class MyCache<A, V> implements Computable<A, V> {

    private final Map<A, V> cache = new ConcurrentHashMap<>();

    private final Computable<A, V> c;

    public MyCache(Computable<A, V> c) {
        this.c = c;
    }

    @Override
    public V compute(A arg) throws Exception {
        System.out.println("进入缓存机制");
        V result = cache.get(arg);
        if (result == null) {
            result = c.compute(arg);
            cache.put(arg, result);
        }
        return result;
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


