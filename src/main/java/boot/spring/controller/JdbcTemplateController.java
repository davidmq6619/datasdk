package boot.spring.controller;

import boot.spring.constant.SdkConstant;
import boot.spring.pagemodel.AjaxResult;
import boot.spring.po.*;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@Api(tags = "jdbcTemplate接口")
@RestController
public class JdbcTemplateController {

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

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTemplateController.class);
    public static int COUNT = 10000;

    @ApiOperation("查询mysql")
    @RequestMapping(value = "/jdbc/actors/{id}/{name}", method = RequestMethod.GET)
    @ResponseBody
    public List<Actor> getactorlist(@PathVariable("id") Short id, @PathVariable("name") String name) {
        List<Actor> list = mysqlTemplate.query("select * from actor where actor_id = ? or first_name = ?", new Object[]{id, name}, new BeanPropertyRowMapper<>(Actor.class));
        return list;
    }

    @ApiOperation("查询potgresql")
    @RequestMapping(value = "/jdbc/hotwords/{word}", method = RequestMethod.GET)
    public List<MarketDataRecord> gethotwords(@PathVariable("word") String word) {
        Long wordL = Long.valueOf(word);
        List<MarketDataRecord> list = pgTemplate.query("select * from market_data_record2 where id = ?", new Object[]{wordL}, new BeanPropertyRowMapper<>(MarketDataRecord.class));
        return list;
    }

    @ApiOperation("查询oracle")
    @RequestMapping(value = "/jdbc/dw", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> gethotwords() {
        List<Map<String, Object>> list = oracleTemplate.queryForList("select * from HBHZK.dwb ");
        return list;
    }

    @ApiOperation("数据推送")
    @RequestMapping(value = "/meinian/easyPush", method = RequestMethod.POST)
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

    @ApiOperation("数据推送")
    @RequestMapping(value = "/meinian/easyPushTest/{word}", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult easyPushTest(@PathVariable("word") String word) {
        if (StrUtil.isBlank(word)) {
            word = "1";
        }
        if (!NumberUtil.isNumber(word)) {
            return AjaxResult.error("推送数据的数量应为数字");
        }
        Long wordL = Long.valueOf(word);
        List<MarketDataRecord> list = pgTemplate.query("select * from market_data_record2 limit ?", new Object[]{wordL}, new BeanPropertyRowMapper<>(MarketDataRecord.class));
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


    @ApiOperation("数据推送")
    @RequestMapping(value = "/meinian/easyPushData", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult easyPushData() {
        int i = 0;
        while (true) {
            MarketData ord = new MarketData();
            String checkData = "";
            String testData = "";
            CustomerDto dto = new CustomerDto();
            int nextInt = new Random().nextInt(99999);
            String vid = "Y52213544" + nextInt;
            String phone = "176651" + nextInt;
            dto.setAgentMobile(phone);
            dto.setCheckDate(DateUtil.parse("2022-09-29", "yyyy-MM-dd"));
            dto.setCustCsrq(DateUtil.parse("1965-10-26", "yyyy-MM-dd"));
            dto.setCustName("测试" + nextInt);
            dto.setCustSex("0");
            dto.setMobile(phone);
            dto.setCustSfzh(vid);
            dto.setShopNo("17");
            dto.setVid(vid);
            ord.setCustomer(dto);
            ord.setCheckData(JSON.parseArray(checkData, CheckData.class));
            ord.setTestData(JSON.parseArray(testData, CheckData.class));
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
                Thread.sleep(100);
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
            i++;
            if(i == 100){
                return AjaxResult.error("推送完成");
            }
        }
    }

    //扁鹊数据推送
    @ApiOperation("扁鹊数据推送")
    @RequestMapping(value = "/meinian/mnData/{words}", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult easyPushMnData(@PathVariable("words") String words) {
        if (StrUtil.isBlank(words)) {
            words = "1";
        }
        //获取用户数据
        String[] splits = words.split("[,，]");
        for (String wordL : splits) {
            List<CustomerInfo> list = pgTemplate.query("select * from mn_dwd_customer_info where vid = ?", new Object[]{wordL}, new int[]{Types.VARCHAR}, new BeanPropertyRowMapper<>(CustomerInfo.class));
            for (CustomerInfo customer : list) {
                MarketData ord = new MarketData();
                int nextInt = new Random().nextInt(9999);
                CustomerDto customerDto = new CustomerDto();
                int anInt = new Random().nextInt(99999999);
                customerDto.setAgentMobile("176" + anInt);
                customerDto.setMobile("18826415976");
                customerDto.setCustSfzh("36078220081023" + nextInt);
                customerDto.setCheckDate(customer.getCheck_time());
                customerDto.setCustSex(customer.getSex().toString());
                String name = customer.getName();
                /*String xing = "";
                if (StrUtil.isNotBlank(name)) {
                    if (name.length() <= 3) {
                        xing = name.substring(0, 1);
                    }
                }
                xing = xing + "**";*/
                customerDto.setCustName(name);
                customerDto.setVid(customer.getVid());
                customerDto.setCustCsrq(customer.getBirth_date());
                customerDto.setShopNo(customer.getShop_no());
                ord.setCustomer(customerDto);
                //获取检验数据
                ArrayList<CheckData> tTestList = new ArrayList<>();
                List<TestData> testDataList = pgTemplate.query("select t.vid,t.item_ft,t.item_name,t.unit," +
                        "t.results,t.normal_l,t.normal_h,t.summary from  (select *  from mn_dwd_check_info i where " +
                        "(i.big_category='LAB' or results like '%阴性%' or  results like '%阳性%' or (i.normal_l is not null" +
                        " and i.normal_l!='') or (i.normal_h is not null and i.normal_h!='') or (i.item_name like '%呼气试验%') " +
                        " ) and " +
                        "item_ft not like '%检查%' and i.item_name not like '%液基%')t where t.vid =?", new Object[]{wordL}, new int[]{Types.VARCHAR}, new BeanPropertyRowMapper<>(TestData.class));
                for (TestData data : testDataList) {
                    CheckData tData = new CheckData();
                    tData.setCategory(data.getItem_ft());
                    tData.setItemName(data.getItem_name());
                    tData.setNormalH(data.getNormal_h());
                    tData.setNormalL(data.getNormal_l());
                    tData.setResult(data.getResults());
                    tData.setUnit(data.getUnit());
                    tTestList.add(tData);
                }

                ArrayList<CheckData> tCheckList = new ArrayList<>();
                List<TestData> checkDataList = pgTemplate.query("select t.vid,t.item_ft,t.item_name,t.unit,t.results,t.normal_l,t.normal_h,summary from\n" +
                        "(select * from mn_dwd_check_info i where item_ft like '%检查%' or i.item_name like '%液基%' or (i.normal_l is null  and i.normal_h is null  and results not like '%阴性%' and  results not like '%阳性%' and i.item_name is not null and i.item_name!='' and i.item_name not like '%呼气试验%'))t where t.vid =?;", new Object[]{wordL}, new int[]{Types.VARCHAR}, new BeanPropertyRowMapper<>(TestData.class));
                StringBuilder zj = new StringBuilder();
                for (TestData data : checkDataList) {
                    if ((StrUtil.isNotBlank(data.getSummary()) && !data.getItem_name().contains("身高") && !data.getItem_name().contains("体重")
                            && !data.getItem_name().contains("舒张压") && !data.getItem_name().contains("收缩压")
                            && !data.getItem_name().contains("体重指数")) || "甲状腺彩超".equals(data.getItem_ft()) ||
                            data.getItem_name().contains("液基")) {
                        CheckData tData = new CheckData();
                        tData.setCategory(data.getItem_ft());
                        tData.setItemName("描述");
                        tData.setNormalH(data.getNormal_h());
                        tData.setNormalL(data.getNormal_l());
                        tData.setResult(data.getResults());
                        tData.setUnit(data.getUnit());
                        tCheckList.add(tData);
                        CheckData tData2 = new CheckData();
                        tData2.setCategory(data.getItem_ft());
                        tData2.setItemName("小结");
                        tData2.setNormalH(data.getNormal_h());
                        tData2.setNormalL(data.getNormal_l());
                        tData2.setResult(data.getSummary());
                        tData2.setUnit(data.getUnit());
                        tCheckList.add(tData2);
                        if (data.getItem_name().contains("液基")) {
                            zj.append("★")
                                    .append(data.getItem_ft())
                                    .append("：")
                                    .append(data.getResults());
                        } else {
                            zj.append("★")
                                    .append(data.getItem_ft())
                                    .append("：")
                                    .append(data.getSummary());
                        }
                    } else {
                        CheckData tData = new CheckData();
                        tData.setCategory(data.getItem_ft());
                        tData.setItemName(data.getItem_name());
                        tData.setNormalH(data.getNormal_h());
                        tData.setNormalL(data.getNormal_l());
                        tData.setResult(data.getResults());
                        tData.setUnit(data.getUnit());
                        tCheckList.add(tData);
                    }
                }
                String zjAll = zj.toString();
                if (StrUtil.isNotBlank(zjAll)) {
                    CheckData tData = new CheckData();
                    tData.setCategory("总结");
                    tData.setItemName("总结");
                    tData.setResult(zjAll);
                    tCheckList.add(tData);
                }
                ord.setCheckData(tCheckList);
                ord.setTestData(tTestList);
                nextInt = new Random().nextInt(9999);
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
        }
        return AjaxResult.success("推送成功");
    }

    @ApiOperation("扁鹊数据推送2")
    @RequestMapping(value = "/meinian/mnData2/{words}", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult easyPushMnData2(@PathVariable("words") String words) {
        if (StrUtil.isBlank(words)) {
            words = "1";
        }
        //获取用户数据
        String[] splits = words.split("[,，]");
        for (String wordL : splits) {
            List<CustomerInfo> list = pgTemplate.query("select * from mn_dwd_customer_info where vid = ?", new Object[]{wordL}, new int[]{Types.VARCHAR}, new BeanPropertyRowMapper<>(CustomerInfo.class));
            for (CustomerInfo customer : list) {
                MarketData ord = new MarketData();
                int nextInt = new Random().nextInt(9999);
                CustomerDto customerDto = new CustomerDto();
                int anInt = new Random().nextInt(99999999);
                customerDto.setAgentMobile("176" + anInt);
                customerDto.setMobile("10126410000");
                customerDto.setCustSfzh(customer.getVid());
                customerDto.setCheckDate(customer.getCheck_time());
                customerDto.setCustSex(customer.getSex().toString());
                customerDto.setShopNo(customer.getShop_no());
                String name = customer.getName();
                /*String xing = "";
                if (StrUtil.isNotBlank(name)) {
                    xing = name.substring(0, 1);
                }
                xing = xing + "**";*/
                customerDto.setCustName(name);
                customerDto.setVid(customer.getVid());
                customerDto.setCustCsrq(customer.getBirth_date());
                ord.setCustomer(customerDto);
                //获取检验数据
                ArrayList<CheckData> tTestList = new ArrayList<>();
                List<TestData> testDataList = pgTemplate.query("select t.vid,t.item_ft,t.item_name,t.unit," +
                        "t.results,t.normal_l,t.normal_h from   mn_dwd_check_result_info t where " +
                        "t.big_category ='lis_result' and  t.vid =?", new Object[]{wordL}, new int[]{Types.VARCHAR}, new BeanPropertyRowMapper<>(TestData.class));
                for (TestData data : testDataList) {
                    CheckData tData = new CheckData();
                    tData.setCategory(data.getItem_ft());
                    tData.setItemName(data.getItem_name());
                    tData.setNormalH(data.getNormal_h());
                    tData.setNormalL(data.getNormal_l());
                    tData.setResult(data.getResults());
                    tData.setUnit(data.getUnit());
                    tTestList.add(tData);
                }

                ArrayList<CheckData> tCheckList = new ArrayList<>();
                List<TestData> checkDataList = pgTemplate.query("select t.vid,t.item_ft,t.item_name,t.unit,t.results,t.normal_l,t.normal_h from\n" +
                        " mn_dwd_check_result_info t where t.big_category !='lis_result' and  t.vid =?;", new Object[]{wordL}, new int[]{Types.VARCHAR}, new BeanPropertyRowMapper<>(TestData.class));
                for (TestData data : checkDataList) {
                    CheckData tData = new CheckData();
                    tData.setCategory(data.getItem_ft());
                    tData.setItemName(data.getItem_name());
                    tData.setNormalH(data.getNormal_h());
                    tData.setNormalL(data.getNormal_l());
                    tData.setResult(data.getResults());
                    tData.setUnit(data.getUnit());
                    tCheckList.add(tData);
                }
                ord.setCheckData(tCheckList);
                ord.setTestData(tTestList);
                nextInt = new Random().nextInt(9999);
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
        }
        return AjaxResult.success("推送成功");
    }

    @ApiOperation("门店数据推送")
    @RequestMapping(value = "/meinian/cityData/{words}", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult cityData(@PathVariable("words") String word) {
        String[] stringArr = word.split("[,，]");
        //获取门店
        StringBuilder stringBuilder = new StringBuilder();
        String msg = "";
        for (String s : stringArr) {
            String sql = "select o.vid,count(*) as number,max(to_number(to_char(m.birth_date,'YYYY'),'9999')) as year,max(m.shop_no) as shop from mn_dwd_customer_info m \n" +
                    "left join mn_dim_shop_info n on m.shop_no = n.in_factory \n" +
                    "left join mn_dwd_check_result_info o on m.vid=o.vid \n" +
                    "where n.city = '" + s + "' and m.birth_date is not null group by o.vid";
            List<CityDataRecord> list = pgTemplate.query(sql, new Object[]{}, new BeanPropertyRowMapper<>(CityDataRecord.class));
            if (list.isEmpty()) {
                LOGGER.error("地市[{}]，未找到门店数据", s);
                continue;
            }
            Map<String, List<CityDataRecord>> collect = list.stream().collect(Collectors.groupingBy(CityDataRecord::getShop));
            //门店数
            Integer count = collect.keySet().size();
            int cityNumber = 10;
            int yu = cityNumber / count;
            int i = 0;
            Iterator<String> iterator = collect.keySet().iterator();
            while (iterator.hasNext()) {
                i++;
                String mapKey = iterator.next();
                List<CityDataRecord> cityDataRecords = collect.get(mapKey);
                int yearGold;
                int numberGold;
                for (CityDataRecord yearRecord : cityDataRecords) {
                    if (yearRecord.getYear() < 1960) {
                        yearGold = 9;
                    } else if (yearRecord.getYear() >= 1960 && yearRecord.getYear() < 1970) {
                        yearGold = 8;
                    } else if (yearRecord.getYear() >= 1970 && yearRecord.getYear() < 1980) {
                        yearGold = 7;
                    } else if (yearRecord.getYear() >= 1980 && yearRecord.getYear() < 1990) {
                        yearGold = 6;
                    } else if (yearRecord.getYear() >= 1990 && yearRecord.getYear() < 2000) {
                        yearGold = 5;
                    } else {
                        yearGold = 4;
                    }
                    if (yearRecord.getNumber() > 200) {
                        numberGold = 11;
                    } else if (yearRecord.getNumber() > 180 && yearRecord.getNumber() <= 200) {
                        numberGold = 10;
                    } else if (yearRecord.getNumber() > 160 && yearRecord.getNumber() <= 180) {
                        numberGold = 9;
                    } else if (yearRecord.getNumber() > 140 && yearRecord.getNumber() <= 160) {
                        numberGold = 8;
                    } else if (yearRecord.getNumber() > 120 && yearRecord.getNumber() <= 140) {
                        numberGold = 7;
                    } else if (yearRecord.getNumber() > 100 && yearRecord.getNumber() <= 120) {
                        numberGold = 6;
                    } else if (yearRecord.getNumber() > 80 && yearRecord.getNumber() <= 100) {
                        numberGold = 5;
                    } else if (yearRecord.getNumber() > 60 && yearRecord.getNumber() <= 80) {
                        numberGold = 4;
                    } else if (yearRecord.getNumber() > 40 && yearRecord.getNumber() <= 60) {
                        numberGold = 3;
                    } else if (yearRecord.getNumber() > 20 && yearRecord.getNumber() <= 40) {
                        numberGold = 2;
                    } else {
                        numberGold = 1;
                    }
                    yearRecord.setGold(yearGold * numberGold);
                }
                List<CityDataRecord> records = cityDataRecords.stream().sorted(Comparator.comparing(CityDataRecord::getGold).reversed()).collect(Collectors.toList());
                if (count == i) {
                    yu = 10 - ((count - 1) * yu);
                    stringBuilder.append(records.stream().limit(yu).map(CityDataRecord::getVid).collect(Collectors.joining(",")));
                    stringBuilder.append(",");
                    int cityNumberO = stringBuilder.toString().split(",").length;
                    if (cityNumberO < cityNumber) {
                        int cha = cityNumber - cityNumberO;
                        msg = msg + "【" + s + "】地市符合规则数据缺少" + cha + "条。";
                    }
                } else {
                    stringBuilder.append(records.stream().limit(yu).map(CityDataRecord::getVid).collect(Collectors.joining(",")));
                    stringBuilder.append(",");
                }
            }
        }
        String finalStr = stringBuilder.toString();
        if (StrUtil.isNotBlank(msg)) {
            finalStr = msg + "。" + finalStr;
        }
        return AjaxResult.success(finalStr);
    }

    @ApiOperation("医院数据推送")
    @RequestMapping(value = "/meinian/yyData", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult yyData() {
        String tableList = SdkConstant.TABLE_STR + "_user_report_list";
        String tableInfo = SdkConstant.TABLE_STR + "_user_report_info";
        //获取用户数据
        executor.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
        AtomicInteger tjCount = new AtomicInteger();
        int allCount = 0;
        int i = 0;
        while (true) {
            try {
                int offset = i * COUNT;
                String sql = "select member_id as vid,name,sex,sfzh,tel,birthday birth_date," +
                        "check_time from " + tableList + "" +
                        " LIMIT " + COUNT + " OFFSET " + offset + "";
                List<CustomerInfo> list = pgTemplate.query(sql, new Object[]{}, new int[]{}, new BeanPropertyRowMapper<>(CustomerInfo.class));

                int mo = list.size() % 1000;
                int mo1 = 0;
                if (mo == 0) {
                    mo1 = list.size() / 1000;
                } else {
                    mo1 = list.size() / 1000 + 1;
                }
                List<Thread> listT = new ArrayList<>();
                CountDownLatch cd = new CountDownLatch(mo1);
                for (int j = 0; j < mo1; j++) {
                    int m = 0;
                    if (list.size() - j * 1000 >= 1000) {
                        m = 1000;
                    } else {
                        m = list.size() - j * 1000;
                    }

                    List<CustomerInfo> customerInfos = list.subList(j * 1000, (j * 1000) + m);
                    String s1 = customerInfos.stream().map(info -> "'" + info.getVid() + "'").collect(Collectors.joining(","));
                    String sql2 = "select member_id as vid,class_name as item_ft ,item_name,result_value results,unit,reference normal_l,image_describe,image_diagnose,data_type from " + tableInfo + " where member_id in(" + s1 + ")";
                    List<TestData> testDataLists = pgTemplate.query(sql2, new Object[]{}, new int[]{}, new BeanPropertyRowMapper<>(TestData.class));
                    Map<String, List<TestData>> collect = testDataLists.stream().collect(Collectors.groupingBy(TestData::getVid));

                    listT.add(new Thread(() -> {
                        for (CustomerInfo customer : customerInfos) {
                            MarketData ord = new MarketData();
                            int nextInt = new Random().nextInt(9999);
                            CustomerDto customerDto = new CustomerDto();
                            int anInt = new Random().nextInt(99999999);
                            customerDto.setAgentMobile(customer.getTel());
                            customerDto.setMobile(customer.getTel());
                            customerDto.setCustSfzh(customer.getSfzh());
                            if (StrUtil.isBlank(customer.getTel())) {
                                customerDto.setAgentMobile("176" + anInt);
                                customerDto.setMobile("18826415976");
                            }
                            if (StrUtil.isBlank(customer.getSfzh())) {
                                customerDto.setCustSfzh("360782199401236619");
                            }
                            if ("女".equals(customer.getSex())) {
                                customerDto.setCustSex("0");
                            } else {
                                customerDto.setCustSex("1");
                            }
                            if (customer.getBirth_date() == null) {
                                customer.setBirth_date(DateUtil.parse("1994-01-23", "yyyy-MM-dd"));
                            }
                            customerDto.setCheckDate(customer.getCheck_time());
                            customerDto.setShopNo(SdkConstant.SHOP_NO);
                            String name = customer.getName();
                            customerDto.setCustName(name);
                            customerDto.setVid(customer.getVid());
                            customerDto.setCustCsrq(customer.getBirth_date());
                            ord.setCustomer(customerDto);
                            //获取检验数据
                            ArrayList<CheckData> tTestList = new ArrayList<>();
                            ArrayList<CheckData> tCheckList = new ArrayList<>();
                            List<TestData> testDataList = collect.get(customer.getVid());
                            if (testDataList == null) {
                                continue;
                            }
                            Boolean flag = false;
                            for (TestData data : testDataList) {
                                if ("2".equals(data.getData_type())) {
                                    if (StrUtil.isNotBlank(data.getImage_describe())) {
                                        CheckData tData = new CheckData();
                                        tData.setCategory(data.getItem_ft());
                                        tData.setItemName("描述");
                                        tData.setResult(data.getImage_describe());
                                        tCheckList.add(tData);
                                        CheckData tDataXJ = new CheckData();
                                        tDataXJ.setCategory(data.getItem_ft());
                                        tDataXJ.setItemName("小结");
                                        tDataXJ.setResult(data.getImage_diagnose());
                                        tCheckList.add(tDataXJ);
                                    } else {
                                        CheckData tData = new CheckData();
                                        tData.setCategory(data.getItem_ft());
                                        tData.setItemName(data.getItem_name());
                                        tData.setNormalH(data.getNormal_h());
                                        tData.setNormalL(data.getNormal_l());
                                        tData.setResult(data.getResults());
                                        tData.setUnit(data.getUnit());
                                        tCheckList.add(tData);
                                    }
                                } else if ("3".equals(data.getData_type())) {
                                    CheckData tData = new CheckData();
                                    tData.setCategory(data.getItem_ft());
                                    tData.setItemName(data.getItem_name());
                                    //tData.setNormalH(data.getNormal_h());
                                    tData.setNormalL(data.getNormal_l());
                                    tData.setResult(data.getResults());
                                    tData.setUnit(data.getUnit());
                                    tTestList.add(tData);
                                }
                                if (StrUtil.isNotBlank(data.getItem_name()) && (data.getItem_name().contains("身高") || data.getItem_name().contains("体重"))) {
                                    flag = true;
                                }
                            }
                            //身高体重默认赋值
                            if (!flag) {
                                CheckData tData = new CheckData();
                                CheckData tData2 = new CheckData();
                                if ("0".equals(customerDto.getCustSex())) {
                                    tData.setCategory("一般检查");
                                    tData.setItemName("身高");
                                    tData.setResult("160");
                                    tData.setUnit("Cm");
                                    tData2.setCategory("一般检查");
                                    tData2.setItemName("体重");
                                    tData2.setResult("55");
                                    tData2.setUnit("kg");
                                } else {
                                    tData.setCategory("一般检查");
                                    tData.setItemName("身高");
                                    tData.setResult("175");
                                    tData.setUnit("Cm");
                                    tData2.setCategory("一般检查");
                                    tData2.setItemName("体重");
                                    tData2.setResult("65");
                                    tData2.setUnit("kg");
                                }
                                tCheckList.add(tData);
                                tCheckList.add(tData2);
                            }
                            ord.setCheckData(tCheckList);
                            ord.setTestData(tTestList);
                            nextInt = new Random().nextInt(9999);
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
                                String s = HttpRequest.post(SdkConstant.URL)
                                        .header("Content-Type", "application/json")
                                        .body(JSONUtil.parse(ord))
                                        .execute()
                                        .body();
                                JSONObject jsonObject = JSON.parseObject(s);
                                if (!"0".equals(jsonObject.get("code"))) {
                                    LOGGER.error("数据推送失败-》外部订单号=》[{}] 体检编号[{}],结果返回{}", ord.getOrderNo(), ord.getCustomer().getVid(), s);
                                } else {
                                    tjCount.getAndIncrement();
                                }
                            } catch (Exception e) {
                                LOGGER.info("数据推送-》外部订单号=》[{}] 体检编号[{}]", ord.getOrderNo(), ord.getCustomer().getVid());
                                e.printStackTrace();
                            }
                        }
                        cd.countDown();
                    }));

                }
                Iterator var13 = listT.iterator();
                while (var13.hasNext()) {
                    Thread thread = (Thread) var13.next();
                    thread.start();
                }
                ;
                cd.await();
                if (list.size() < COUNT) {
                    allCount = i * COUNT + list.size();
                    break;
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            } finally {
                i++;
            }
        }
        String str = "推送数据总数:" + allCount + "。推送成功数据数:" + tjCount;
        return AjaxResult.success(str);
    }

    @ApiOperation("医院数据推送定制化-广东省人民医院")
    @RequestMapping(value = "/meinian/gdyyData", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult gdyyData() {
        String tableList = SdkConstant.TABLE_STR + "_user_report_list";
        String tableInfo = SdkConstant.TABLE_STR + "_user_report_info";
        //获取用户数据
        AtomicInteger tjCount = new AtomicInteger();
        int allCount = 0;
        int i = 0;
        while (true) {
            try {
                int offset = i * COUNT;
                String sql = "select member_id as vid from " + tableInfo + " where member_id not in ('352','356','507','503','489','506','572','508','352','499','550','485','510','560','340','573','504','552','505','500','569','553','555','498','501','356','502','509','574','496','511') group by member_id" +
                        " LIMIT " + COUNT + " OFFSET " + offset + "";
                List<CustomerInfo> list = pgTemplate.query(sql, new Object[]{}, new int[]{}, new BeanPropertyRowMapper<>(CustomerInfo.class));
                int mo = list.size() % 1000;
                int mo1 = 0;
                if (mo == 0) {
                    mo1 = list.size() / 1000;
                } else {
                    mo1 = list.size() / 1000 + 1;
                }
                List<Thread> listT = new ArrayList<>();
                CountDownLatch cd = new CountDownLatch(mo1);
                for (int j = 0; j < mo1; j++) {
                    int m = 0;
                    if (list.size() - j * 1000 >= 1000) {
                        m = 1000;
                    } else {
                        m = list.size() - j * 1000;
                    }

                    List<CustomerInfo> customerInfos = list.subList(j * 1000, (j * 1000) + m);
                    String s1 = customerInfos.stream().map(info -> "'" + info.getVid() + "'").collect(Collectors.joining(","));
                    String sql2 = "select member_id as vid,class_name as item_ft ,item_name,result_value results,unit,reference normal_l,image_describe,image_diagnose,data_type from " + tableInfo + " where member_id in(" + s1 + ")";
                    List<TestData> testDataLists = pgTemplate.query(sql2, new Object[]{}, new int[]{}, new BeanPropertyRowMapper<>(TestData.class));
                    Map<String, List<TestData>> collect = testDataLists.stream().collect(Collectors.groupingBy(TestData::getVid));

                    listT.add(new Thread(() -> {
                        for (CustomerInfo customer : customerInfos) {
                            MarketData ord = new MarketData();
                            int nextInt = new Random().nextInt(9999);
                            CustomerDto customerDto = new CustomerDto();
                            int anInt = new Random().nextInt(99999999);
                            customerDto.setAgentMobile("176" + anInt);
                            customerDto.setMobile("176" + anInt);
                            customerDto.setCustSfzh(customer.getVid());
                            customerDto.setCheckDate(new Date());
                            customerDto.setShopNo(SdkConstant.SHOP_NO);
                            customerDto.setCustSex(new Random().nextInt(2) + "");
                            customerDto.setCustName("赖总" + customer.getVid());
                            customerDto.setVid(customer.getVid());
                            customerDto.setCustCsrq(DateUtil.parse("1978-07-08", "yyyy-MM-dd"));
                            ord.setCustomer(customerDto);
                            //获取检验数据
                            ArrayList<CheckData> tTestList = new ArrayList<>();
                            ArrayList<CheckData> tCheckList = new ArrayList<>();
                            List<TestData> testDataList = collect.get(customer.getVid());
                            if (testDataList == null) {
                                continue;
                            }
                            Boolean flag = false;
                            for (TestData data : testDataList) {
                                if ("2".equals(data.getData_type())) {
                                    if (StrUtil.isBlank(data.getNormal_l()) && StrUtil.isNotBlank(data.getItem_ft()) && !"血压".equals(data.getItem_ft())) {
                                        CheckData tDataXJ = new CheckData();
                                        tDataXJ.setCategory(data.getItem_ft());
                                        tDataXJ.setItemName("小结");
                                        tDataXJ.setResult(data.getResults());
                                        tCheckList.add(tDataXJ);
                                    } else {
                                        CheckData tData = new CheckData();
                                        tData.setCategory(data.getItem_ft());
                                        tData.setItemName(data.getItem_name());
                                        tData.setNormalL(data.getNormal_l());
                                        tData.setResult(data.getResults());
                                        tData.setUnit(data.getUnit());
                                        tCheckList.add(tData);
                                    }
                                } else if ("3".equals(data.getData_type())) {
                                    CheckData tData = new CheckData();
                                    tData.setCategory(data.getItem_ft());
                                    tData.setItemName(data.getItem_name());
                                    //tData.setNormalH(data.getNormal_h());
                                    tData.setNormalL(data.getNormal_l());
                                    tData.setResult(data.getResults());
                                    tData.setUnit(data.getUnit());
                                    tTestList.add(tData);
                                }
                                if (StrUtil.isNotBlank(data.getItem_name()) && (data.getItem_name().contains("身高") || data.getItem_name().contains("体重"))) {
                                    flag = true;
                                }
                            }
                            //身高体重默认赋值
                            if (!flag) {
                                CheckData tData = new CheckData();
                                CheckData tData2 = new CheckData();
                                if ("0".equals(customerDto.getCustSex())) {
                                    tData.setCategory("一般检查");
                                    tData.setItemName("身高");
                                    tData.setResult("160");
                                    tData.setUnit("Cm");
                                    tData2.setCategory("一般检查");
                                    tData2.setItemName("体重");
                                    tData2.setResult("55");
                                    tData2.setUnit("kg");
                                } else {
                                    tData.setCategory("一般检查");
                                    tData.setItemName("身高");
                                    tData.setResult("175");
                                    tData.setUnit("Cm");
                                    tData2.setCategory("一般检查");
                                    tData2.setItemName("体重");
                                    tData2.setResult("65");
                                    tData2.setUnit("kg");
                                }
                                tCheckList.add(tData);
                                tCheckList.add(tData2);
                            }
                            ord.setCheckData(tCheckList);
                            ord.setTestData(tTestList);
                            nextInt = new Random().nextInt(9999);
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
                                String s = HttpRequest.post(SdkConstant.URL)
                                        .header("Content-Type", "application/json")
                                        .body(JSONUtil.parse(ord))
                                        .execute()
                                        .body();
                                JSONObject jsonObject = JSON.parseObject(s);
                                if (!"0".equals(jsonObject.get("code"))) {
                                    LOGGER.error("数据推送失败-》外部订单号=》[{}] 体检编号[{}],结果返回{}", ord.getOrderNo(), ord.getCustomer().getVid(), s);
                                } else {
                                    tjCount.getAndIncrement();
                                }
                            } catch (Exception e) {
                                LOGGER.info("数据推送-》外部订单号=》[{}] 体检编号[{}]", ord.getOrderNo(), ord.getCustomer().getVid());
                                e.printStackTrace();
                            }
                        }
                        cd.countDown();
                    }));

                }
                Iterator var13 = listT.iterator();
                while (var13.hasNext()) {
                    Thread thread = (Thread) var13.next();
                    thread.start();
                }
                ;
                cd.await();
                if (list.size() < COUNT) {
                    allCount = i * COUNT + list.size();
                    break;
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            } finally {
                i++;
            }
        }
        String str = "推送数据总数:" + allCount + "。推送成功数据数:" + tjCount;
        return AjaxResult.success(str);
    }

    @ApiOperation("美年线上数据推送")
    @RequestMapping(value = "/meinian/mnDataOnline/{words}", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult mnDataOnline(@PathVariable("words") String words) {
        if (StrUtil.isBlank(words)) {
            words = "1";
        }
        //获取用户数据
        String[] splits = words.split("[,，]");
        for (String wordL : splits) {
            String sql = "select a.vid,a.cust_name,a.cust_sex,a.cust_csrq,a.cust_sfzh,a.check_date,a.shop_no,a.mobile,b.test_data,c.check_data from temp_cust_info2 a left join temp_test_info2 b on a.id=b.id left join temp_check_info2 c on a.id=c.id where a.vid=?";
            List<CustomerOnlineInfo> list = pgTemplate.query(sql, new Object[]{wordL}, new int[]{Types.VARCHAR}, new BeanPropertyRowMapper<>(CustomerOnlineInfo.class));
            if (list.size() == 0 || list == null) {
                continue;
            }
            CustomerOnlineInfo customer = list.get(0);
            MarketData ord = new MarketData();
            CustomerDto customerDto = new CustomerDto();
            String phone = "101" + new Random().nextInt(99999999);
            customerDto.setAgentMobile(phone);
            customerDto.setMobile(phone);
            customerDto.setCustSfzh(customer.getCust_sfzh());
            customerDto.setCheckDate(customer.getCheck_date());
            customerDto.setCustSex(customer.getCust_sex());
            customerDto.setShopNo(customer.getShop_no());
            int nextInt = new Random().nextInt(99);
            customerDto.setCustName(customer.getCust_name());
            customerDto.setVid(customer.getVid());
            customerDto.setCompanyId("886991");
            customerDto.setCompanyName("2023年武汉市农村经济经营管理局  30人");
            customerDto.setCustCsrq(customer.getCust_csrq());
            ord.setCustomer(customerDto);
            //获取检验数据
            ArrayList<CheckData> tTestList = new ArrayList<>();
            List<CheckData> testDataList = JSON.parseArray(customer.getTest_data(), CheckData.class);
            for (CheckData data : testDataList) {
                CheckData tData = new CheckData();
                tData.setCategory(data.getCategory());
                tData.setItemName(data.getItemName());
                tData.setNormalH(data.getNormalH());
                tData.setNormalL(data.getNormalL());
                tData.setResult(data.getResult());
                tData.setUnit(data.getUnit());
                tData.setItemNo(data.getItemNo());
                tTestList.add(tData);
            }
            ArrayList<CheckData> tCheckList = new ArrayList<>();
            List<CheckData> checkDataList = JSON.parseArray(customer.getCheck_data(), CheckData.class);
            for (CheckData data : checkDataList) {
                CheckData tData = new CheckData();
                tData.setCategory(data.getCategory());
                tData.setItemName(data.getItemName());
                tData.setNormalH(data.getNormalH());
                tData.setNormalL(data.getNormalL());
                tData.setResult(data.getResult());
                tData.setUnit(data.getUnit());
                tCheckList.add(tData);
            }
            ord.setCheckData(tCheckList);
            ord.setTestData(tTestList);
            nextInt = new Random().nextInt(9999);
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


    @ApiOperation("省人医-离线数据拉取-数据抽查")
    @RequestMapping(value = "/hospital/gdsdata/{vids}", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult gdsdata(@PathVariable(value = "vids",required = false) String vids) {
        if(StrUtil.isBlank(vids)){
            vids = "";
        }        
        String tableList = SdkConstant.TABLE_STR + "_user_report_list";
        String tableInfo = SdkConstant.TABLE_STR + "_user_report_info";
        //获取用户数据
        AtomicInteger tjCount = new AtomicInteger();
        int allCount = 0;
        int i = 0;
        while (true) {
            try {
                int offset = i * COUNT;
                String sql = "" +
                        "check_time from " + tableList + "" +
                        " LIMIT " + COUNT + " OFFSET " + offset + "";
                StringBuilder builder = new StringBuilder();
                builder.append("select member_id as vid,name,sex,sfzh,tel,birthday birth_date,check_time from ");
                builder.append(tableList);
                builder.append(" where 1=1");
                if(StrUtil.isNotBlank(vids)){
                    String[] split = vids.split("[,、]");
                    String s = Arrays.asList(split).stream().map(item -> "'" + item + "'").distinct().collect(Collectors.joining(","));
                    builder.append(" and member_id in (" +s+ ")");
                }
                builder.append(" LIMIT " + COUNT + " OFFSET " + offset + "");
                List<CustomerInfo> list = pgTemplate.query(builder.toString(), new Object[]{}, new int[]{}, new BeanPropertyRowMapper<>(CustomerInfo.class));
                int mo = list.size() % 1000;
                int mo1 = 0;
                if (mo == 0) {
                    mo1 = list.size() / 1000;
                } else {
                    mo1 = list.size() / 1000 + 1;
                }
                List<Thread> listT = new ArrayList<>();
                CountDownLatch cd = new CountDownLatch(mo1);
                for (int j = 0; j < mo1; j++) {
                    int m = 0;
                    if (list.size() - j * 1000 >= 1000) {
                        m = 1000;
                    } else {
                        m = list.size() - j * 1000;
                    }

                    List<CustomerInfo> customerInfos = list.subList(j * 1000, (j * 1000) + m);
                    String s1 = customerInfos.stream().map(info -> "'" + info.getVid() + "'").collect(Collectors.joining(","));
                    String sql2 = "select member_id as vid,class_name as item_ft ,item_name,result_value results,unit,reference normal_l,image_describe,image_diagnose,data_type,summary from " + tableInfo + " where member_id in(" + s1 + ")";
                    List<TestData> testDataLists = pgTemplate.query(sql2, new Object[]{}, new int[]{}, new BeanPropertyRowMapper<>(TestData.class));
                    Map<String, List<TestData>> collect = testDataLists.stream().collect(Collectors.groupingBy(TestData::getVid));
                    listT.add(new Thread(() -> {
                        for (CustomerInfo customer : customerInfos) {
                            MarketData ord = new MarketData();
                            int nextInt = new Random().nextInt(9999);
                            CustomerDto customerDto = new CustomerDto();
                            int anInt = new Random().nextInt(99999999);
                            customerDto.setAgentMobile(customer.getTel());
                            customerDto.setMobile(customer.getTel());
                            customerDto.setCustSfzh(customer.getSfzh());
                            if (StrUtil.isBlank(customer.getTel())) {
                                customerDto.setAgentMobile("176" + anInt);
                                customerDto.setMobile("18826415976");
                            }
                            if (StrUtil.isBlank(customer.getSfzh())) {
                                customerDto.setCustSfzh(customer.getVid());
                            }
                            if ("女".equals(customer.getSex())) {
                                customerDto.setCustSex("0");
                            } else {
                                customerDto.setCustSex("1");
                            }
                            if (customer.getBirth_date() == null) {
                                customer.setBirth_date(DateUtil.parse("1999-01-23", "yyyy-MM-dd"));
                            }
                            customerDto.setCheckDate(customer.getCheck_time());
                            customerDto.setShopNo(SdkConstant.SHOP_NO);
                            String name = customer.getName();
                            customerDto.setCustName(name);
                            customerDto.setVid(customer.getVid());
                            customerDto.setCustCsrq(customer.getBirth_date());
                            ord.setCustomer(customerDto);
                            //获取检验数据
                            ArrayList<CheckData> tTestList = new ArrayList<>();
                            ArrayList<CheckData> tCheckList = new ArrayList<>();
                            List<TestData> testDataList = collect.get(customer.getVid());
                            if (testDataList == null) {
                                continue;
                            }
                            Boolean flag = false;
                            List<TestData> xjDataList = new ArrayList<>();
                            for (TestData data : testDataList) {
                                if ("2".equals(data.getData_type())) {
                                    if (StrUtil.isNotBlank(data.getImage_describe())) {
                                        CheckData tData = new CheckData();
                                        tData.setCategory(data.getItem_ft());
                                        tData.setItemName("描述");
                                        tData.setResult(data.getImage_describe());
                                        tCheckList.add(tData);
                                        CheckData tDataXJ = new CheckData();
                                        tDataXJ.setCategory(data.getItem_ft());
                                        tDataXJ.setItemName("小结");
                                        tDataXJ.setResult(data.getImage_diagnose());
                                        tCheckList.add(tDataXJ);
                                    } else {
                                        CheckData tData = new CheckData();
                                        tData.setCategory(data.getItem_ft());
                                        tData.setItemName(data.getItem_name());
                                        tData.setNormalH(data.getNormal_h());
                                        tData.setNormalL(data.getNormal_l());
                                        tData.setResult(data.getResults());
                                        tData.setUnit(data.getUnit());
                                        tCheckList.add(tData);
                                        if(StrUtil.isNotBlank(data.getSummary())){
                                            xjDataList.add(data);
                                        }
                                    }
                                } else if ("3".equals(data.getData_type())) {
                                    CheckData tData = new CheckData();
                                    tData.setCategory(data.getItem_ft());
                                    tData.setItemName(data.getItem_name());
                                    //tData.setNormalH(data.getNormal_h());
                                    tData.setNormalL(data.getNormal_l());
                                    tData.setResult(data.getResults());
                                    tData.setUnit(data.getUnit());
                                    tTestList.add(tData);
                                }else {
                                    CheckData tData = new CheckData();
                                    tData.setCategory(data.getItem_ft());
                                    tData.setItemName(data.getItem_name());
                                    tData.setResult(data.getResults());
                                    tCheckList.add(tData);
                                }
                                if (StrUtil.isNotBlank(data.getItem_name()) && (data.getItem_name().contains("身高") || data.getItem_name().contains("体重"))) {
                                    flag = true;
                                }
                            }
                            if(CollectionUtils.isNotEmpty(xjDataList)){
                                Map<String, List<TestData>> collect1 = xjDataList.stream().collect(Collectors.groupingBy(TestData::getItem_ft));
                                Iterator<String> iterator = collect1.keySet().iterator();
                                while (iterator.hasNext()){
                                    String itemFt = iterator.next();
                                    List<TestData> checkDataList = collect1.get(itemFt);
                                    String summary = checkDataList.stream()
                                            .map(TestData::getSummary)
                                            .collect(Collectors.joining("。"));
                                    if(StrUtil.isNotBlank(summary)){
                                        CheckData tData = new CheckData();
                                        tData.setCategory(itemFt);
                                        tData.setItemName("小结");
                                        tData.setResult(summary);
                                        tCheckList.add(tData);
                                    }
                                }
                            }
                            ord.setCheckData(tCheckList);
                            ord.setTestData(tTestList);
                            nextInt = new Random().nextInt(9999);
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
                                String s = HttpRequest.post(SdkConstant.URL)
                                        .header("Content-Type", "application/json")
                                        .body(JSONUtil.parse(ord))
                                        .execute()
                                        .body();
                                JSONObject jsonObject = JSON.parseObject(s);
                                if (!"0".equals(jsonObject.get("code"))) {
                                    LOGGER.error("数据推送失败-》外部订单号=》[{}] 体检编号[{}],结果返回{}", ord.getOrderNo(), ord.getCustomer().getVid(), s);
                                } else {
                                    tjCount.getAndIncrement();
                                }
                            } catch (Exception e) {
                                LOGGER.info("数据推送-》外部订单号=》[{}] 体检编号[{}]", ord.getOrderNo(), ord.getCustomer().getVid());
                                e.printStackTrace();
                            }
                        }
                        cd.countDown();
                    }));

                }
                Iterator var13 = listT.iterator();
                while (var13.hasNext()) {
                    Thread thread = (Thread) var13.next();
                    thread.start();
                }
                ;
                cd.await();
                if (list.size() < COUNT) {
                    allCount = i * COUNT + list.size();
                    break;
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            } finally {
                i++;
            }
        }
        String str = "推送数据总数:" + allCount + "。推送成功数据数:" + tjCount;
        return AjaxResult.success(str);
    }
}
