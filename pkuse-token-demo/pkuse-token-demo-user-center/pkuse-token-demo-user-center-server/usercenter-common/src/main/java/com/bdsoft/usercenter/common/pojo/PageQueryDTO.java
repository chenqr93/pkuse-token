package com.bdsoft.usercenter.common.pojo;

/**
 * 分页查询参数（数据传输对象，前端 GET 请求查询参数）
 *
 * <p> 按规约 POJO 属性使用包装类型且不设默认值；取值规范化见 {@link #currentOrDefault()} / {@link #sizeOrDefault()}。 </p>
 */
public class PageQueryDTO {

    /** 页码（从 1 开始；为空或小于 1 时按 1 处理） */
    private Long current;

    /** 每页条数（为空或小于 1 时按 10 处理） */
    private Long size;

    /** 页码规范化（空或非法值回落到 1） */
    public long currentOrDefault() {
        return current == null || current < 1 ? 1 : current;
    }

    /** 每页条数规范化（空或非法值回落到 10） */
    public long sizeOrDefault() {
        return size == null || size < 1 ? 10 : size;
    }

    public Long getCurrent() {
        return current;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "PageQueryDTO{" +
                "current=" + current +
                ", size=" + size +
                '}';
    }
}
