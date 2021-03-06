package top.bitplanet.devops.lowcode.controller;

import top.bitplanet.devops.lowcode.constants.ProjectConfig;
import top.bitplanet.devops.lowcode.dto.query.ProjectMetaDataQuery;
import top.bitplanet.devops.lowcode.helper.Gencode4MybatisplusHelper;
import top.bitplanet.devops.lowcode.entity.DatasourceInfo;
import top.bitplanet.devops.lowcode.entity.PgTableColumn;
import top.bitplanet.devops.lowcode.entity.PgTableInfo;
import top.bitplanet.devops.lowcode.initialconfig.entity.ModuleInfo;
import top.bitplanet.devops.lowcode.initialconfig.entity.ProductInfo;
import top.bitplanet.devops.lowcode.initialconfig.service.IModuleInfoService;
import top.bitplanet.devops.lowcode.initialconfig.service.IProductInfoService;
import top.bitplanet.devops.lowcode.mapper.GenCodeMapper;
import top.bitplanet.devops.lowcode.service.IDatasourceInfoService;
import top.bitplanet.devops.lowcode.service.IGenCodeService;
import top.bitplanet.devops.support.core.helper.*;
import top.bitplanet.devops.uaa.dto.UserResp;
import top.bitplanet.devops.uaa.dto.query.UserQuery;
import top.bitplanet.devops.uaa.feign.UserFeign;
import top.bitplanet.devops.lowcode.dto.query.DataSourceInfoQuery;
import top.bitplanet.devops.lowcode.dto.query.FormUIComponentInfoQuery;
import top.bitplanet.devops.lowcode.dto.query.GenCodeQuery;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/rapid")
public class GenCodeController {

    static final String OSS_ROOT = "oss";

    @Autowired
    private IGenCodeService genCodeService;
    @Autowired
    private IDatasourceInfoService datasourceInfoService;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private ServerProperties serverProperties;
    @Autowired
    private IModuleInfoService moduleInfoService;
    @Autowired
    private IProductInfoService productInfoService;

    @GlobalTransactional(timeoutMills = 300000, name = "spring-cloud-demo-tx")
    @GetMapping("/test")
    public Object test() {
        String xid = RootContext.getXID();
        log.info("??????lowcode....." + xid);
        R<UserResp> detail = userFeign.detail(1471363275779506177L);
        UserQuery userQuery = new UserQuery();
        userQuery.setUsername("?????????");
        userQuery.setEmail("1@a.com");
        userFeign.add(userQuery);
        DatasourceInfo info = new DatasourceInfo();
        info.setDatabase("??????");
        datasourceInfoService.save(info);
        // System.out.println(1/0);
        return detail;
    }

    /**
     * ?????????????????????zip?????????
     * @param query
     * @param request
     * @return
     * @throws SQLException
     */
    @PostMapping("/downloadCode")
    public Object gen(@RequestBody GenCodeQuery query, HttpServletRequest request) throws SQLException {
        DatasourceInfo info = datasourceInfoService.getById(query.getDatasourceInfoId());
        // ???????????????
        DataSourceInfoQuery dataSourceInfoQuery = info.convertToQuery();
        String realPath = request.getServletContext().getRealPath("");
        log.info("??????????????????==>{}",realPath);
        // ????????????
        String relativePath = OSS_ROOT + "/" + UuIdHelper.randomUUID();
        // ??????????????????
        String projectPath = realPath + "/" + relativePath;
        try {
            projectPath = Gencode4MybatisplusHelper.genCode(dataSourceInfoQuery,query,projectPath);
            File zip = ZipHelper.zip(projectPath);
            String zipPath =  OSS_ROOT + "/" + zip.getName();
            return R.success(serverProperties.getServlet().getContextPath() + "/" + zipPath);
        } catch (Exception e) {
            log.error("?????????????????????",e);
            return R.fail("?????????????????????");
        }
    }

    /**
     * ?????????????????????coder????????????
     * @param query
     * @param request
     * @return
     * @throws SQLException
     */
    @PostMapping("/installCode")
    public Object installCode(@RequestBody GenCodeQuery query, HttpServletRequest request) {
        DatasourceInfo info = datasourceInfoService.getById(query.getDatasourceInfoId());
        // ???????????????
        DataSourceInfoQuery dataSourceInfoQuery = info.convertToQuery();
        String realPath = request.getServletContext().getRealPath("");
        // ????????????
        String projectPath = realPath + "/" + OSS_ROOT + "/" + UuIdHelper.randomUUID();
        try {
            // ?????????id??????????????????????????????
            ModuleInfo moduleInfo = moduleInfoService.getById(query.getModuleInfoId());
            ProjectMetaDataQuery metaDataQuery = query.getMetaDataQuery();
            metaDataQuery.setDescription(moduleInfo.getDescription());
            metaDataQuery.setModuleName(moduleInfo.getModuleName());
            // ????????????
            ProductInfo productInfo = productInfoService.getById(moduleInfo.getProductId());
            metaDataQuery.setCompanyName(productInfo.getCompanyName());
            metaDataQuery.setProductName(productInfo.getProductName());
            // ????????????
            projectPath = Gencode4MybatisplusHelper.genCode(dataSourceInfoQuery,query,projectPath);
            // ???????????????????????????
            String backCodePath = projectPath + "/" + query.getMetaDataQuery().getProductName() + "-" + query.getMetaDataQuery().getModuleHyphen();
            // ???????????????????????? ?????????git??????
            String backGitDir = moduleInfo.getBackGitDir();
            String gitDest = Gencode4MybatisplusHelper.pullAndCopy(backGitDir, ProjectConfig.GIT_COPY_DIR);
            // ????????????
            String moduleDir = gitDest + moduleInfo.getBackModuleDir();
            FileHelper.copyContent(backCodePath ,moduleDir,query.isOverride());
            // ?????????????????? & ??????
            String frontGitSrc = moduleInfo.getFrontGitDir();
            String frontGitDest = Gencode4MybatisplusHelper.pullAndCopy(frontGitSrc,ProjectConfig.GIT_COPY_DIR);
            String viewDir = frontGitDest + moduleInfo.getFrontModuleDir();
            String webPath = projectPath + "/web";
            FileHelper.copyContent(webPath,viewDir,query.isOverride());
            return R.success(new String[]{gitDest,frontGitDest});
        } catch (Exception e) {
            log.error("?????????????????????",e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * ??????????????????sql
     * @return
     */
    @PostMapping("/genSql")
    public Object genSqlByForm(@RequestBody List<FormUIComponentInfoQuery> querys,String tableName) {
        log.info(querys.toString());
        String sql = genCodeService.genSqlByForm(querys,tableName);
        return R.success(sql);
    }

    /**
     *
     * @return
     */
    @PostMapping("/database/{id}/executeSql")
    public Object executeSql(@RequestBody String sql,@PathVariable("id") Long databaseId) {
        Connection connection = datasourceInfoService.getConnectionById(databaseId);
        if (connection == null) {
            return  R.fail("????????????????????????????????????????????????????????????");
        }
        boolean result;
        try {
            result = DBHelper.executeSql(connection, sql);
        } catch (SQLException e) {
            log.error("??????????????????sql??????:",e);
            return R.fail("????????????????????????sql??????");
        }
        return R.success("???????????????" + result);
    }

    /**
     * ????????????????????????????????????
     * @param id
     * @return
     */
    @GetMapping("/database/{id}")
    public R<List<PgTableInfo>> showTables(@PathVariable("id") Long id) {
        Connection connection = datasourceInfoService.getConnectionById(id);
        if (connection == null) {
            return  R.fail("????????????????????????????????????????????????????????????");
        }
        List<PgTableInfo> allTables = genCodeService.findAllTables(connection, GenCodeMapper.SQL_SELECT_ALL_TABLES);
        return R.success("success",allTables);
    }

    /**
     * ?????????????????????
     * @param id
     * @return
     */
    @GetMapping("/database/{id}/{tableName}")
    public Object showTables(@PathVariable("id") Long id,@PathVariable("tableName")String tableName) {
        DatasourceInfo info = datasourceInfoService.getById(id);
        DataSourceInfoQuery query = info.convertToQuery();
        // ????????????
        Connection connection = DBHelper.getConnection(query.getJdbcUrl(), query.getUsername(), query.getPassword());
        List<PgTableColumn> allTableColumns = genCodeService.findAllColumns(connection, GenCodeMapper.SQL_SELECT_ALL_COLUMNS,tableName);
        return R.success("success",allTableColumns);
    }


}
