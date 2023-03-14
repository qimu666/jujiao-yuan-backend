package com.qimu.jujiao.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年03月13日 09:41
 * @Version: 1.0
 * @Description: 分页公用类
 */
@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = -7310366548235104148L;
    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 当前是第几页
     */
    private int pageNum = 1;
}
