package top.bitplanet.devops.lowcode.initialconfig.feign;

import top.bitplanet.devops.support.core.framework.page.TYPageQuery;
import top.bitplanet.devops.support.core.framework.page.TYPageResp;
import top.bitplanet.devops.support.core.helper.R;
import top.bitplanet.devops.lowcode.initialconfig.dto.ProductInfoResp;
import top.bitplanet.devops.lowcode.initialconfig.dto.query.ProductInfoQuery;
import org.springframework.cloud.openfeign.FeignClient;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 初始化配置-产品配置信息 feign 对象
 * </p>
 *
 * @author Le
 * @since 2022-02-07
 */
@FeignClient(contextId = "lowcodeProductInfoFeign",name = "lowcode",path = "/initialconfig/productInfo")
public interface ProductInfoFeign {

    /**
    * 新增 初始化配置-产品配置信息
    * @param query
    * @return
    */
    @PostMapping("")
    public R<String> add(@RequestBody @Valid ProductInfoQuery query);

    /**
     * 根据 id 删除 初始化配置-产品配置信息
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public R<Long> delete(@PathVariable("id") Long id);

    /**
     * 根据id 修改 初始化配置-产品配置信息
     * @param id
     * @param query
     * @return
     */
    @PutMapping("/{id}")
    public R<String> update(@PathVariable("id") Long id, @RequestBody ProductInfoQuery query);

    /**
     * 根据id 查询初始化配置-产品配置信息详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<ProductInfoResp> detail(@PathVariable("id") Long id);

    /**
     * 通用分页查询
     * @param tyPageQuery
     * @return
     */
    @GetMapping("/page")
    public R<TYPageResp<ProductInfoResp>> page(TYPageQuery<ProductInfoQuery> tyPageQuery);

}