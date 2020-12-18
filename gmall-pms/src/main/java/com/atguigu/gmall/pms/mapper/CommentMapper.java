package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.CommentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品评价
 * 
 * @author jinge
 * @email jinge@atguigu.com
 * @date 2020-12-14 22:13:27
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentEntity> {
	
}
