package mini_cache.computable;

/**
 * 描述：     有一个计算函数computer，用来代表耗时计算，每个计算器都要实现这个接口，这样就可以无侵入实现缓存功能
 */
public interface Computable <String, Integer>{

    Integer compute(String arg) throws Exception;
}
