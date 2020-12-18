package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallPmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {


    @Autowired
    private SpuDescMapper descMapper;

    @Autowired
    private SpuAttrValueService attrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private SpuDescService spuDescService;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySupByCidAndPage(Long cid, PageParamVo pageParamVo) {

        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();

        //如果用户选择了分类，并且查询本类
        if (cid != 0){
            wrapper.eq("category_id" , cid);
        }

        String key = pageParamVo.getKey();
        //判断关键字是否为空
        if (StringUtils.isNotBlank(key)){
            //调用wrapper的and方法可以把两个sql语句进行()连接
            //t表示为wrapper对象
            wrapper.and(t -> t.eq("id" , key).or().like("name" , key));
        }
        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    @Transactional(rollbackFor = Exception.class , readOnly = true)
    @Override
    public void bigSave(SpuVo spu) throws FileNotFoundException {
        //1.保存spu相关信息
        //1.1   保存spu基本信息：pms_spu
        Long spuId = saveSpu(spu);

        //1.2   保存spu的描述信息: pms_spu_desc
//        saveSpuDesc(spu, spuId);
        this.spuDescService.saveSpuDesc(spu , spuId);

//        try {
//            TimeUnit.SECONDS.sleep(4);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

//        new FileInputStream("xxxx");

        ///1.3  保存spu的基本属性信息: pms_spu_attr_value
        saveSpuBaseAttr(spu, spuId);
        //2.保存sku相关信息
        saveSkuInto(spu, spuId);
    }

    private void saveSpuBaseAttr(SpuVo spu, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){

            this.attrValueService.saveBatch(baseAttrs.stream().map(spuAttrValueVo ->{
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();

                BeanUtils.copyProperties(spuAttrValueVo , spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList()));
        }
    }

    private void saveSkuInto(SpuVo spu, Long spuId) {
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return;
        }
        //2.1   保存sku的基本信息: pms_sku
        skus.forEach(sku -> {
            sku.setSpuId(spuId);
            sku.setBrandId(spu.getBrandId());
            sku.setCatagoryId(spu.getCategoryId());
            //设置默认图片
            List<String> images = sku.getImages();
            //判断是否有默认图片，如果有默认图片使用默认图片，没有使用第一张图片
            if (!CollectionUtils.isEmpty(images)){
                sku.setDefaultImage(StringUtils.isNotBlank(sku.getDefaultImage()) ? sku.getDefaultImage() : images.get(0));
            }

            this.skuMapper.insert(sku);
            Long skuId = sku.getId();

            //2.2   保存sku的图片信息: pms_sku_images
            if (!CollectionUtils.isEmpty(images)){

                skuImagesService.saveBatch(images.stream().map(image ->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(sku.getDefaultImage() , image) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }

            //2.3   保存sku的基本属性信息: pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)){
                saleAttrs.forEach(skuAttrValueEntity ->
                    skuAttrValueEntity.setSkuId(skuId)
                );
                this.skuAttrValueService.saveBatch(saleAttrs);
            }

            //3.保存sku营销信息
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(sku , skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallPmsClient.saveSales(skuSaleVo);

        });
    }



    private Long saveSpu(SpuVo spu) {
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        return spu.getId();
    }

}