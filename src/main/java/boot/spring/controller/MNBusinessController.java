package boot.spring.controller;

import boot.spring.constant.SdkConstant;
import boot.spring.pagemodel.AjaxResult;
import boot.spring.po.*;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@Api(tags = "jdbcTemplate接口")
@RestController
public class MNBusinessController {

    @Autowired
    @Qualifier("mysqlTemplate")
    JdbcTemplate mysqlTemplate;

    @Autowired
    @Qualifier("pgTemplate")
    JdbcTemplate pgTemplate;

    @Autowired
    @Qualifier("oracleTemplate")
    JdbcTemplate oracleTemplate;

    @Autowired
    @Qualifier("meinianTemplate")
    JdbcTemplate meinianTemplate;

    @Autowired
    ThreadPoolExecutor executor;

    private static final Logger LOGGER = LoggerFactory.getLogger(MNBusinessController.class);
    public static int COUNT = 10000;

    @ApiOperation("查询mysql")
    @RequestMapping(value = "/jdbc2/actors/{id}/{name}", method = RequestMethod.GET)
    @ResponseBody
    public List<Actor> getactorlist(@PathVariable("id") Short id, @PathVariable("name") String name) {
        List<Actor> list = mysqlTemplate.query("select * from actor where actor_id = ? or first_name = ?", new Object[]{id, name}, new BeanPropertyRowMapper<>(Actor.class));
        return list;
    }

    @ApiOperation("查询potgresql")
    @RequestMapping(value = "/jdbc2/hotwords/{word}", method = RequestMethod.GET)
    public List<MarketDataRecord> gethotwords(@PathVariable("word") String word) {
        Long wordL = Long.valueOf(word);
        List<MarketDataRecord> list = pgTemplate.query("select * from market_data_record2 where id = ?", new Object[]{wordL}, new BeanPropertyRowMapper<>(MarketDataRecord.class));
        return list;
    }

    @ApiOperation("查询oracle")
    @RequestMapping(value = "/jdbc2/dw", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> gethotwords() {
        List<Map<String, Object>> list = oracleTemplate.queryForList("select * from HBHZK.dwb ");
        return list;
    }

    @ApiOperation("数据推送")
    @RequestMapping(value = "/meinian2/easyPush", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult easyPush(@RequestBody DataParam dataParam) {
        if (dataParam == null) {
            return AjaxResult.error("请输入正常源数据id");
        }
        String[] strings = dataParam.getIds().split("[,，;；]");

        if (!NumberUtil.isNumber(strings[0])) {
            return AjaxResult.error("数据源应该是数据类型");
        }
        List<String> stringList = Arrays.asList(strings);
        String listStr = stringList.stream().collect(Collectors.joining(","));
        String sql = "select * from market_data_record2 where id in(" + listStr + ")";
        List<MarketDataRecord> list = pgTemplate.query(sql, new Object[]{}, new BeanPropertyRowMapper<>(MarketDataRecord.class));
        if (list.isEmpty()) {
            return AjaxResult.error("未找到源数据，参数id为" + dataParam.toString());
        }
        for (MarketDataRecord marketDataRecord : list) {

            String market_data = marketDataRecord.getMarket_data();
            MarketData ord = JSON.parseObject(market_data, MarketData.class);
            int nextInt = new Random().nextInt(9999);
            ord.setOrderNo(System.currentTimeMillis() + String.valueOf(nextInt));
            int nextInt2 = new Random().nextInt(9999);
            ord.setNonceStr(System.currentTimeMillis() + String.valueOf(nextInt2));
            ord.setUsername(SdkConstant.USER_NAME);
            ord.setPackageId(SdkConstant.PACKAGE_ID);
            ord.setOrderStatus(2);
            //加密处理
            StringBuffer sb = new StringBuffer();
            sb.append("orderNo=" + (ord.getOrderNo() == null ? SdkConstant.NULL_STR : ord.getOrderNo()) + SdkConstant.SPLIT_OTHER);
            sb.append("vid=" + (ord.getCustomer().getVid() == null ? SdkConstant.NULL_STR : ord.getCustomer().getVid()) + SdkConstant.SPLIT_OTHER);
            sb.append("custName=" + (ord.getCustomer().getCustName() == null ? SdkConstant.NULL_STR : ord.getCustomer().getCustName()) + SdkConstant.SPLIT_OTHER);
            sb.append("custSex=" + (ord.getCustomer().getCustSex() == null ? SdkConstant.NULL_STR : ord.getCustomer().getCustSex()) + SdkConstant.SPLIT_OTHER);
            sb.append("shopNo=" + (ord.getCustomer().getShopNo() == null ? SdkConstant.NULL_STR : ord.getCustomer().getShopNo()) + SdkConstant.SPLIT_OTHER);
            sb.append("custSfzh=" + (ord.getCustomer().getCustSfzh() == null ? SdkConstant.NULL_STR : ord.getCustomer().getCustSfzh()) + SdkConstant.SPLIT_OTHER);
            sb.append("agentMobile=" + (ord.getCustomer().getAgentMobile() == null ? SdkConstant.NULL_STR : ord.getCustomer().getAgentMobile()) + SdkConstant.SPLIT_OTHER);
            sb.append("checkNum=" + ((ord.getCheckData() == null || ord.getCheckData().size() == 0) ? 0 : ord.getCheckData().size()) + SdkConstant.SPLIT_OTHER);
            sb.append("testNum=" + ((ord.getTestData() == null || ord.getTestData().size() == 0) ? 0 : ord.getTestData().size()) + SdkConstant.SPLIT_OTHER);
            sb.append("nonceStr=" + (ord.getNonceStr() == null ? SdkConstant.NULL_STR : ord.getNonceStr()) + SdkConstant.SPLIT_OTHER);
            sb.append("username=" + (ord.getUsername() == null ? SdkConstant.NULL_STR : ord.getUsername()));
            AES aes = SecureUtil.aes(HexUtil.decodeHex(SdkConstant.DES_KEY));
            String signStr = aes.encryptHex(sb.toString());
            //数据组装
            ord.setSignStr(signStr);
            try {
                executor.submit(() -> {
                    LOGGER.info("数据推送-》外部订单号=》[{}] 体检编号[{}]", ord.getOrderNo(), ord.getCustomer().getVid());
                    String body = HttpRequest.post(SdkConstant.URL)
                            .header("Content-Type", "application/json")
                            .body(JSONUtil.parse(ord))
                            .execute()
                            .body();
                    LOGGER.info("响应结果[{}]", body);
                });
            } catch (Exception e) {
                LOGGER.warn("数据推送异常{}", JSON.toJSONString(ord), e);
                return AjaxResult.error("推送异常");
            }
        }
        return AjaxResult.success("推送成功");
    }

    @ApiOperation("获取美年使用项目")
    @RequestMapping(value = "/meinian/tj/{words}", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult mnDataOnline(@PathVariable("words") String words) {
        if (StrUtil.isBlank(words)) {
            words = "1";
        }
        //获取用户数据
        String[] splits = words.split("[,，]");
        for (String wordL : splits) {
            HashMap<String, Object> getParamMaps = new HashMap<>();
            getParamMaps.put("vid", wordL);
            HttpResponse response = HttpRequest.get("https://service.health-100.cn/v1/etl/findByVid")
                    .header("Content-Type", "application/json")
                    .form(getParamMaps).execute();
            String responseBody = response.body();
            StandardCheckData onlineInfo = JSONUtil.toBean(responseBody, StandardCheckData.class);
            StandardResult result1 = onlineInfo.getResult();
            List<MNCheckData> test = result1.getStandardTestResult();
            List<MNCheckData> diagnosis = result1.getStandardDiagnosisResults();
            List<MNCheckData> check = result1.getPacsCheckResult();
            String vid = result1.getCustomerRecordId();
            for (MNCheckData testData : test) {
                if ("0".equals(testData.getCleanStatus())) {
                    String itemName = testData.getItemName();
                    String itemNameComm = testData.getItemNameComm();
                    String itemFt = testData.getTestItemFt();
                    String discrete = testData.getResultsDiscrete();
                    String results = testData.getItemResults();
                    String sql = "INSERT INTO \"standard_tj\" (\"vid\", \"item_name\", \"item_name_comm\", \"item_ft\", \"results\", \"results_discrete\", \"type\")" +
                            " VALUES ('" + vid + "', '" + itemName + "', '" + itemNameComm + "', '" + itemFt + "', '" + results + "', '" + discrete + "', 1);";
                    pgTemplate.execute(sql);
                }
            }
            for (MNCheckData diagnosisData : diagnosis) {
                String itemName = diagnosisData.getItemName();
                String discrete = diagnosisData.getResultsDiscrete();
                String result = diagnosisData.getResult();
                String sql = "INSERT INTO \"standard_tj\" (\"vid\", \"item_name\", \"results\", \"type\")" +
                        " VALUES ('" + vid + "', '" + itemName + "', '" + result + "', 3);";
                pgTemplate.execute(sql);
            }
        }
        return AjaxResult.success("推送成功");
    }

    @ApiOperation("获取甲状腺数据")
    @RequestMapping(value = "/meinian/jzx", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult mnDataOnline() {
        String words = "999";
        //获取用户数据
        String sql = "select * from jzx_data ";
        List<Map<String, Object>> list = pgTemplate.queryForList(sql);
        for (Map<String, Object> stringObjectMap : list) {
            String checkData = (String) stringObjectMap.get("check_data");
            List<CheckJZXData> listData = JSON.parseArray(checkData, CheckJZXData.class);
            for (CheckJZXData data : listData) {
                String initResult = data.getInitResult();
                String itemCommon = data.getItemNameComm();
                String result = data.getItemResults();
                if (StrUtil.isNotBlank(result) && StrUtil.isNotBlank(initResult) && StrUtil.isNotBlank(itemCommon) && initResult.contains("甲状腺彩超") && itemCommon.equals("描述")) {
                    String insertsql = "INSERT INTO \"jzx_data_insert\" (\"check_data\")" +
                            " VALUES ('" + result + "');";
                    pgTemplate.execute(insertsql);
                }
                System.out.println(data);
            }

        }
        return AjaxResult.success("推送成功");
    }

    @ApiOperation("获取检验数据")
    @RequestMapping(value = "/meinian/test", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult mnDataTest() {
        String words = "999";
        //获取用户数据
        String sql = "select * from test_data ";
        List<Map<String, Object>> list = pgTemplate.queryForList(sql);
        HashMap<String, MNCheckData> hashMap = new HashMap<>();
        for (Map<String, Object> stringObjectMap : list) {
            String checkData = (String) stringObjectMap.get("test_data");
            List<MNCheckData> listData = JSON.parseArray(checkData, MNCheckData.class);
            for (MNCheckData data : listData) {
                String testItemFt = data.getTestItemFt();
                String itemName = data.getItemName();
                String itemNameComm = data.getItemNameComm();
                String itemUnit = data.getItemUnit();
                String itemUnitComm = data.getUnitComm();
                String result = data.getItemResults();
                if (StrUtil.isNotBlank(result) && StrUtil.isNotBlank(itemName) && NumberUtil.isNumber(result)) {
                    hashMap.put(itemNameComm, data);
                }
            }
        }
        Iterator<Map.Entry<String, MNCheckData>> iterator = hashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, MNCheckData> next = iterator.next();
            MNCheckData data = next.getValue();
            String testItemFt = data.getTestItemFt();
            String itemName = data.getItemName();
            String itemNameComm = data.getItemNameComm();
            String itemUnit = data.getItemUnit();
            String itemUnitComm = data.getUnitComm();
            String result = data.getItemResults();
            if (StrUtil.isNotBlank(itemNameComm) && itemNameComm.contains("'")) {
                System.out.println(data);
                continue;
            }
            String insertsql = "INSERT INTO \"test_data_insert\" (\"item_ft\", \"item_name\", \"item_name_comm\", \"unit\", \"unit_comm\", \"result\")" +
                    " VALUES ('" + testItemFt + "','" + itemName + "','" + itemNameComm + "','" + itemUnit + "','" + itemUnitComm + "','" + result + "');";
            pgTemplate.execute(insertsql);
        }
        return AjaxResult.success("推送成功");
    }

    @ApiOperation("扁鹊离线数据推送-美年非mdt数据拉取下来-生成mdt体检数据")
    @RequestMapping(value = "/meinian/pushMnDataOffline", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult pushMnDataOffline() {
        String sql = "select * from mn_bianque_customer_info";
        List<MnBianqueCustomerInfo> list = pgTemplate.query(sql, new Object[]{}, new int[]{}, new BeanPropertyRowMapper<>(MnBianqueCustomerInfo.class));
        for (MnBianqueCustomerInfo customer : list) {
            MarketData ord = new MarketData();
            CustomerDto customerDto = new CustomerDto();
            int anInt = new Random().nextInt(99999999);
            String mobile = "000" + anInt;
            String vid = customer.getVid();
            customerDto.setAgentMobile(mobile);
            customerDto.setMobile(mobile);
            customerDto.setCustSfzh(customer.getCertificate_number());
            String sex = customer.getSex();
            if(StrUtil.isNotBlank(sex) && "FEMALE".equals(sex)){
                sex = "0";
            }else {
                sex = "1";
            }
            customerDto.setCustSex(sex);
            customerDto.setShopNo(customer.getShop_no());
            String name = customer.getName();
            customerDto.setCustName(name);
            customerDto.setVid(vid);
            String age = customer.getAge();
            Date checkTime = customer.getCheck_time();
            customerDto.setCheckDate(checkTime);
            Date birthday = new Date();
            if(StrUtil.isNotBlank(age) && checkTime !=null){
                birthday = DateUtil.offset(checkTime, DateField.YEAR, -Integer.valueOf(age));
            }
            customerDto.setCustCsrq(birthday);
            ord.setCustomer(customerDto);
            //获取检验数据
            ArrayList<CheckData> tTestList = new ArrayList<>();
            ArrayList<CheckData> tCheckList = new ArrayList<>();
            String checkSql = "select vid,big_category,item_ft,item_name,unit,result,normal_l from mn_bianque_check_info  where vid =?";
            List<MnBianqueCheckInfo> dataList = pgTemplate.query(checkSql, new Object[]{vid}, new int[]{Types.VARCHAR}, new BeanPropertyRowMapper<>(MnBianqueCheckInfo.class));
            if(CollectionUtils.isEmpty(dataList)){
                continue;
            }
            for (MnBianqueCheckInfo data : dataList) {
                if("LAB".equals(data.getBig_category())){
                    CheckData tData = new CheckData();
                    tData.setCategory(data.getItem_ft());
                    tData.setItemName(data.getItem_name());
                    tData.setNormalH(data.getNormal_h());
                    tData.setNormalL(data.getNormal_l());
                    tData.setResult(data.getResult());
                    tData.setUnit(data.getUnit());
                    tTestList.add(tData);
                }else {
                    CheckData checkData = new CheckData();
                    checkData.setCategory(data.getItem_ft());
                    checkData.setItemName(data.getItem_name());
                    checkData.setNormalH(data.getNormal_h());
                    checkData.setNormalL(data.getNormal_l());
                    checkData.setResult(data.getResult());
                    checkData.setUnit(data.getUnit());
                    tCheckList.add(checkData);
                }
            }
            /**
             * 小结处理
             */
            String xiaojieSql ="select vid,item_ft,result from mn_bianque_xiaojie_info where vid =?";
            List<MnBianqueCheckInfo> xiaojieDataList = pgTemplate.query(xiaojieSql, new Object[]{vid}, new int[]{Types.VARCHAR}, new BeanPropertyRowMapper<>(MnBianqueCheckInfo.class));
            for (MnBianqueCheckInfo data : xiaojieDataList) {
                List<String> resultStrList = new ArrayList<>();
                String results = data.getResult();
                if(StrUtil.isBlank(results)){
                    continue;
                }
                results = results.replace("\\", "");
                List<XiaojieDto> listResult = JSON.parseArray(results, XiaojieDto.class);
                for (XiaojieDto o : listResult) {
                    resultStrList.add(o.getConclusionName());
                }
                String result = "";
                if(resultStrList.size() > 0){
                    result = resultStrList.stream().collect(Collectors.joining("。"));
                }
                CheckData checkData = new CheckData();
                checkData.setCategory(data.getItem_ft());
                checkData.setItemName("小结");
                checkData.setResult(result);
                tCheckList.add(checkData);
            }
            ord.setCheckData(tCheckList);
            ord.setTestData(tTestList);
            Integer nextInt = new Random().nextInt(9999);
            ord.setOrderNo(System.currentTimeMillis() + String.valueOf(nextInt));
            int nextInt2 = new Random().nextInt(9999);
            ord.setNonceStr(System.currentTimeMillis() + String.valueOf(nextInt2));
            ord.setUsername(SdkConstant.USER_NAME);
            ord.setPackageId(SdkConstant.PACKAGE_ID);
            ord.setOrderStatus(2);
            //加密处理
            StringBuffer sb = new StringBuffer();
            sb.append("orderNo=" + (ord.getOrderNo() == null ? SdkConstant.NULL_STR : ord.getOrderNo()) + SdkConstant.SPLIT_OTHER);
            sb.append("vid=" + (ord.getCustomer().getVid() == null ? SdkConstant.NULL_STR : ord.getCustomer().getVid()) + SdkConstant.SPLIT_OTHER);
            sb.append("custName=" + (ord.getCustomer().getCustName() == null ? SdkConstant.NULL_STR : ord.getCustomer().getCustName()) + SdkConstant.SPLIT_OTHER);
            sb.append("custSex=" + (ord.getCustomer().getCustSex() == null ? SdkConstant.NULL_STR : ord.getCustomer().getCustSex()) + SdkConstant.SPLIT_OTHER);
            sb.append("shopNo=" + (ord.getCustomer().getShopNo() == null ? SdkConstant.NULL_STR : ord.getCustomer().getShopNo()) + SdkConstant.SPLIT_OTHER);
            sb.append("custSfzh=" + (ord.getCustomer().getCustSfzh() == null ? SdkConstant.NULL_STR : ord.getCustomer().getCustSfzh()) + SdkConstant.SPLIT_OTHER);
            sb.append("agentMobile=" + (ord.getCustomer().getAgentMobile() == null ? SdkConstant.NULL_STR : ord.getCustomer().getAgentMobile()) + SdkConstant.SPLIT_OTHER);
            sb.append("checkNum=" + ((ord.getCheckData() == null || ord.getCheckData().size() == 0) ? 0 : ord.getCheckData().size()) + SdkConstant.SPLIT_OTHER);
            sb.append("testNum=" + ((ord.getTestData() == null || ord.getTestData().size() == 0) ? 0 : ord.getTestData().size()) + SdkConstant.SPLIT_OTHER);
            sb.append("nonceStr=" + (ord.getNonceStr() == null ? SdkConstant.NULL_STR : ord.getNonceStr()) + SdkConstant.SPLIT_OTHER);
            sb.append("username=" + (ord.getUsername() == null ? SdkConstant.NULL_STR : ord.getUsername()));
            AES aes = SecureUtil.aes(HexUtil.decodeHex(SdkConstant.DES_KEY));
            String signStr = aes.encryptHex(sb.toString());
            //数据组装
            ord.setSignStr(signStr);
            try {
                LOGGER.info("数据推送-》外部订单号=》[{}] 体检编号[{}]", ord.getOrderNo(), ord.getCustomer().getVid());
                String body = HttpRequest.post(SdkConstant.URL)
                        .header("Content-Type", "application/json")
                        .body(JSONUtil.parse(ord))
                        .execute()
                        .body();
                LOGGER.info("响应结果[{}]", body);
            } catch (Exception e) {
                LOGGER.warn("数据推送异常{}", JSON.toJSONString(ord), e);
                return AjaxResult.error("推送异常");
            }
        }
        return AjaxResult.success("推送成功");
    }
}
