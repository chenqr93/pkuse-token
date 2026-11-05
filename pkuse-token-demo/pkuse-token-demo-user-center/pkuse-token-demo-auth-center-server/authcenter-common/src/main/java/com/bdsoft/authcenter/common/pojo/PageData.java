package com.bdsoft.authcenter.common.pojo;

import java.io.Serializable;
import java.util.List;

/**
 * 分页数据（统一响应信封的 data 形态）
 *
 * <p> 所有分页接口的 data 统一为 {records, total, current, size}， </p>
 * <p> 分页信息收进 data，不在响应顶层平铺额外字段。 </p>
 */
public class PageData<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long current;

    /** 每页条数 */
    private long size;

    public PageData() {
    }

    public PageData(List<T> records, long total, long current, long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }

    public static <T> PageData<T> of(List<T> records, long total, long current, long size) {
        return new PageData<>(records, total, current, size);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
