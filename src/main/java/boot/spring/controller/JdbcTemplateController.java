package boot.spring.controller;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

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
            String checkData = "[\n" +
                    "    {\n" +
                    "        \"category\":\"一般检查(1)\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"体重指数:23.10\\r\\n血压:106/65mmHg\\r\\n脉搏:92 次/分\\r\\n标准型\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"一般检查(1)\",\n" +
                    "        \"itemName\":\"体重\",\n" +
                    "        \"result\":\"63.4\",\n" +
                    "        \"unit\":\"Kg\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"一般检查(1)\",\n" +
                    "        \"itemName\":\"身高\",\n" +
                    "        \"result\":\"165.5\",\n" +
                    "        \"unit\":\"cm\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"一般检查(1)\",\n" +
                    "        \"itemName\":\"体重指数(18.5-23.9)\",\n" +
                    "        \"result\":\"23.10\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"一般检查(1)\",\n" +
                    "        \"itemName\":\"理想体重(kg)\",\n" +
                    "        \"result\":\"59.00\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"一般检查(1)\",\n" +
                    "        \"itemName\":\"收缩压\",\n" +
                    "        \"result\":\"106\",\n" +
                    "        \"unit\":\"mmHg\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"一般检查(1)\",\n" +
                    "        \"itemName\":\"舒张压\",\n" +
                    "        \"result\":\"65\",\n" +
                    "        \"unit\":\"mmHg\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"一般检查(1)\",\n" +
                    "        \"itemName\":\"脉搏（次/分）\",\n" +
                    "        \"result\":\"92\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"耳廓\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"外耳道\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"鼓膜\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"鼻外形\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"鼻中隔\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"鼻腔黏膜及分泌物\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"鼻甲\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"鼻窦压痛\",\n" +
                    "        \"result\":\"无\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"口咽黏膜\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"扁桃体\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"耳鼻咽喉科检查未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"耳鼻喉检查\",\n" +
                    "        \"itemName\":\"其它\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"肺罗音\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"肝大小\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"脾大小\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"内科检查  未发现明显异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"其他\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"神经深反射\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"心音\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"心律\",\n" +
                    "        \"result\":\"整齐\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"呼吸音\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"心率\",\n" +
                    "        \"result\":\"84\",\n" +
                    "        \"unit\":\"次/分\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"呼吸\",\n" +
                    "        \"result\":\"18\",\n" +
                    "        \"unit\":\"次/分\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"心脏杂音\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"肝脏质地\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"肝脏压痛\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"脾脏质地\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"脾脏压痛\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"病史\",\n" +
                    "        \"result\":\"无\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"腹壁\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"内科检查\",\n" +
                    "        \"itemName\":\"过敏史\",\n" +
                    "        \"result\":\"无过敏史\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"总检\",\n" +
                    "        \"itemName\":\"总结\",\n" +
                    "        \"result\":\"☆  重要异常结果（C） ☆\\r\\n\\r\\n★ CT检查（胸部）不出片:\\r\\n   肺结节影\\r\\n*建议呼吸内科或胸外科就诊作进一步检查。3个月内复查肺部HRCT。若有肺癌高危因素（年龄大于40岁、长期吸烟史包括二手烟、家属肿瘤史、慢性阻塞性肺疾患等），应当高度关注，密切随访。肺部结节影是指X线或CT所见单发或多发的圆型、类圆形的密度增高隐影。肿瘤、炎症、结核病、结节病、肺寄生虫病等，都有可能表现为肺结节影，通常结节越大，其恶变风险也大。\\r\\n\\r\\n\\r\\n☆ 其它主要异常结果 ☆\\r\\n\\r\\n★ 眼科检查结果:\\r\\n   裸眼视力右:0.15\\r\\n   裸眼视力左:0.06\\r\\n   眼底:双黄斑变性\\r\\n*黄斑病变指视网膜黄斑区发生病理改变的一组疾病的统称，包括视网膜静栓塞、糖尿病视网膜病变、葡萄膜炎、眼外伤、高度近视等。黄斑病变有致视力不可挽救损害的风险，建议尽快眼科就诊。\\r\\n\\r\\n★ 心电图检查结果:\\r\\n   窦性心律\\r\\n   肢体导联低电压\\r\\n★ CT检查（胸部）不出片:\\r\\n   主动脉及冠状动脉部分管壁钙化\\r\\n★ 经颅多谱勒(TCD) :\\r\\n   右侧椎动脉血流速度减慢\\r\\n★ 脂联素检测（ADPN） 降低:  (结果:2.91 范围：3-40)\\r\\n★ 糖化血红蛋白(HbA1c) 增高:  (结果:6.10 范围：4.0-6.0 %)\\r\\n★ 血清甘油三酯测定(TG) 增高:  (结果:1.87 范围：0.2-1.72mmol/L)\\r\\n★ 血清总胆固醇测定(TC) 增高:  (结果:5.76 范围：2.33-5.18 mmol/L)\\r\\n★ 血清低密度脂蛋白胆固醇测定(LDL-C)增高:  (结果:4.20 范围：2.06-3.1 mmol/L)\\r\\n*动脉硬化是心脑血管疾病中最常见的基础疾病，以上异常结果提示累及心血管系统，多见于动脉硬化性心血管疾病，如高血压心脏病、冠心病等。高血压、高血糖、高血脂是心血管疾病的直接危险因素。大部分动脉硬化发展缓慢，治疗关键是早期发现，早期干预，保护器官免受损害。建议心血管内科随诊，定期复查。\\r\\n\\r\\n★ 肝胆脾胰肾彩超:\\r\\n   右肾:右肾错构瘤可能\\r\\n*肾错构瘤又称肾血管平滑肌脂肪瘤。是最常见的肾良性肿瘤，由血管、平滑肌、脂肪等正常组织的错误组合构成。瘤体较小的不需处理，定期B超观察。本次彩超检查疑似错构瘤可能，建议3个内复查一次，以除外其他肾脏占位性病变。\\r\\n\\r\\n（未完  接下页）\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"总检\",\n" +
                    "        \"itemName\":\"健康状况\",\n" +
                    "        \"result\":\"健康\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"总检\",\n" +
                    "        \"itemName\":\"总结2\",\n" +
                    "        \"result\":\"☆  一般异常结果 ☆\\r\\n\\r\\n★ 外科检查结果:\\r\\n   甲状腺结节 \\r\\n★ 甲状腺彩超:\\r\\n   甲状腺左叶结节,（TI-RADS 3类）\\r\\n*甲状腺结节是甲状腺细胞局部异常生长所致，常见于单纯性甲状腺肿、甲状腺炎、甲状腺囊肿、甲状腺腺瘤、甲状腺癌等；判断甲状腺结节的良恶性主要依据穿刺细胞学检查。TI-RADS 3类，属于良性不典型结节，恶性风险很小（＜5%）。建议甲状腺外科随访，年度超声复查。\\r\\n\\r\\n★ 前列腺彩超:\\r\\n   前列腺:前列腺增生\\r\\n*前列腺增生是年长男性的常见病，随年龄增长症状亦加重。病因不是很清楚，可能与前列腺炎、人体性激素失衡等因素有关。主要表现为膀胱刺激征和尿路梗阻症状。建议泌尿外科随诊。\\r\\n\\r\\n★ 肝胆脾胰肾彩超:\\r\\n   肝:脂肪肝趋势\\r\\n*脂肪肝是由多因引起的肝细胞内脂肪堆积过多，当肝内脂肪超过一定量时，可被超声探测到。早期诊断、早期干预、如戒酒，平衡饮食，加强运动，减轻体重，控制血压、血糖、血脂，护肝保肝，避免使用损害肝脏药物等，脂肪肝是可逆的，可以减轻甚至消失。\\r\\n\\r\\n   胆:胆囊息肉\\r\\n*胆囊息肉是指胆囊壁向腔内呈息肉样突出的一类病变总称。临床上常见胆固醇性息肉、炎性息肉、胆囊腺肌瘤病等等，多数为良性，少数为恶性或癌变可能。直径&lt;10mm无临床症状的胆囊息肉，可以肝胆外科随访，定期复查。如发现异常（无蒂、广基息肉），或息肉明显增大，或息肉直径＞8mm，通常主张预防性手术切除，建议肝胆外科就诊咨询。\\r\\n\\r\\n   左肾:左肾囊肿（多发）\\r\\n*肾囊肿是常见的肾脏结构异常，多为良性的病变。可并发尿路梗阻、肾盂积水、尿路感染等。单纯肾囊肿若无并发症一般不予处理，以随访观察为主。\\r\\n\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查（视力）\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"裸眼视力右:0.15\\r\\n裸眼视力左:0.06\\r\\n余眼科检查未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查（视力）\",\n" +
                    "        \"itemName\":\"裸眼视力右\",\n" +
                    "        \"result\":\"0.15\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查（视力）\",\n" +
                    "        \"itemName\":\"裸眼视力左\",\n" +
                    "        \"result\":\"0.06\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查（视力）\",\n" +
                    "        \"itemName\":\"矫正视力右\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查（视力）\",\n" +
                    "        \"itemName\":\"矫正视力左\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"外科常规检查(男)\",\n" +
                    "        \"itemName\":\"淋巴结\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"外科常规检查(男)\",\n" +
                    "        \"itemName\":\"甲状腺\",\n" +
                    "        \"result\":\"甲状腺结节\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"外科常规检查(男)\",\n" +
                    "        \"itemName\":\"乳房\",\n" +
                    "        \"result\":\"未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"外科常规检查(男)\",\n" +
                    "        \"itemName\":\"脊柱\",\n" +
                    "        \"result\":\"未见异常，活动自如\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"外科常规检查(男)\",\n" +
                    "        \"itemName\":\"四肢关节\",\n" +
                    "        \"result\":\"未见异常，活动自如\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"外科常规检查(男)\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"甲状腺结节\\r\\n肛门指检:直肠指检：自愿弃查\\r\\n前列腺:自愿弃查\\r\\n余外科检查未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"外科常规检查(男)\",\n" +
                    "        \"itemName\":\"肛门指检\",\n" +
                    "        \"result\":\"直肠指检：自愿弃查\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"外科常规检查(男)\",\n" +
                    "        \"itemName\":\"前列腺\",\n" +
                    "        \"result\":\"自愿弃查\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"外科常规检查(男)\",\n" +
                    "        \"itemName\":\"其他检查\",\n" +
                    "        \"result\":\"无\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"外科常规检查(男)\",\n" +
                    "        \"itemName\":\"皮肤\",\n" +
                    "        \"result\":\"未见明显异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"静态心电图\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"窦性心律\\r\\n肢体导联低电压\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"甲状腺彩超\",\n" +
                    "        \"itemName\":\"描述\",\n" +
                    "        \"result\":\"双侧甲状腺大小形态正常，峡部不厚，包膜光整，甲状腺左叶下极可见一个混合回声结节，大小约9.7mm×8.7mm，边界清，内部回声不均匀，CDFI示结节内部血供不明显。\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"甲状腺彩超\",\n" +
                    "        \"itemName\":\"小结\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"甲状腺彩超\",\n" +
                    "        \"itemName\":\"描述\",\n" +
                    "        \"result\":\"甲状腺左叶结节,（TI-RADS 3类）\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"甲状腺彩超\",\n" +
                    "        \"itemName\":\"小结\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝胆脾胰肾彩超\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"肝:脂肪肝趋势\\r\\n胆:胆囊息肉\\r\\n右肾:右肾错构瘤可能\\r\\n左肾:左肾囊肿（多发）\\r\\n胰、脾未发现明显异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝胆脾胰肾彩超\",\n" +
                    "        \"itemName\":\"肝\",\n" +
                    "        \"result\":\"肝脏形态大小正常，轮廓规整，肝内回声稍呈点状密集弥漫性增强，肝内管道结构尚清晰。\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝胆脾胰肾彩超\",\n" +
                    "        \"itemName\":\"胆\",\n" +
                    "        \"result\":\"胆囊形态大小正常，壁欠光滑，内壁见一个等回声团附着，大小约5.5mm×5.0mm，后无声影，不随体位改变而移动。\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝胆脾胰肾彩超\",\n" +
                    "        \"itemName\":\"胰\",\n" +
                    "        \"result\":\"胰腺大小、形态正常，边缘规整，内部回声均匀，胰管未见扩张。\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝胆脾胰肾彩超\",\n" +
                    "        \"itemName\":\"脾\",\n" +
                    "        \"result\":\"脾脏大小、形态正常，包膜光整，回声均匀。\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝胆脾胰肾彩超\",\n" +
                    "        \"itemName\":\"左肾\",\n" +
                    "        \"result\":\"左肾形态大小正常，包膜光整，实质回声均匀，肾内可见多个液暗区，较大者位于下极，大小约18.1mm×15.3mm，壁薄而清晰，后方回声增强，CDFI示未探及明显血流信号。集合系统未见分离。\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝胆脾胰肾彩超\",\n" +
                    "        \"itemName\":\"右肾\",\n" +
                    "        \"result\":\"右肾形态大小正常，包膜光整，实质回声均匀，中部隐约可见一个高回声团，大小约5.7mm×4.9mm，边界清，内部回声欠均匀。集合系统未见分离。\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"前列腺彩超\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"前列腺:前列腺增生\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"前列腺彩超\",\n" +
                    "        \"itemName\":\"前列腺\",\n" +
                    "        \"result\":\"前列腺增大，大小约48.0mm×37.5mm×45.9mm，未见明显突向膀胱，内部回声欠均匀，内腺增大，未见明显占位。\"\n" +
                    "    },\n" +
                    " \n" +
                    "    {\n" +
                    "        \"category\":\"CT检查（胸部）不出片\",\n" +
                    "        \"itemName\":\"描述\",\n" +
                    "        \"result\":\"两侧胸廓对称。左肺上叶尖后段（Se5-Img18）见部分实性结节，大小约为8mm×5mm，边界清（建议6月后HRCT随诊）。两侧肺门不大。纵隔窗示心影及大血管形态正常，主动脉及冠状动脉管壁可见钙化。纵隔内未见肿块及明显肿大淋巴结。印象与意见：无胸腔积液及胸膜增厚。\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"CT检查（胸部）不出片\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"肺结节影\\r\\n主动脉及冠状动脉部分管壁钙化\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"经颅多谱勒(TCD)\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"右侧椎动脉血流速度减慢\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"冠状动脉钙化积分\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"详见纸质报告\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"眼睑\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"结膜\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"眼球\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"巩膜\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"角膜\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"眼底:双黄斑变性\\r\\n余眼科检查未见异常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"虹膜\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"晶体\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"瞳孔\",\n" +
                    "        \"result\":\"正常\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"眼底\",\n" +
                    "        \"result\":\"双黄斑变性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"眼科检查(外眼、眼底)\",\n" +
                    "        \"itemName\":\"其它\"\n" +
                    "    },{\n" +
                    "        \"category\":\"C14呼气试验\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"HP阳性(dpm=170) （正常范围：<100）\"\n" +
                    "    },{\n" +
                    "        \"category\":\"胸部低剂量螺旋CT(不出片）\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"1.结论见上\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"胸部低剂量螺旋CT(不出片）\",\n" +
                    "        \"itemName\":\"胸部CT平扫\",\n" +
                    "        \"result\":\"右肺上叶IM39可见小结节影，最大直径约5mm，IM39可见钙化灶，最大直径约7mm，右肺下叶可见斑索影，气管通畅，未见纵隔肺门淋巴结肿大，心影未见明显异常，胸膜无肥厚。肝内可见钙化灶脾脏可见钙化灶结论 肺结节影肺内钙化灶少许慢性炎症 肝内钙化灶（考虑不排除胆管结石可能）脾脏钙化灶 。\"\n" +
                    "    },{\n" +
                    "        \"category\":\"液基薄层细胞学检测\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"无上皮内病变或恶性病变(NILM)\"\n" +
                    "    },{\n" +
                    "        \"category\":\"头颅CT平扫(不出片)\",\n" +
                    "        \"itemName\":\"小结\",\n" +
                    "        \"result\":\"小结：头颅CT 平扫未见明显异常 副鼻窦术后，副鼻窦炎。\"\n" +
                    "    }\n" +
                    "]";
            String testData = "[\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清间接胆红素(计算值)(非结合胆红素)\",\n" +
                    "        \"normalH\":\"19.2\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"6.4\",\n" +
                    "        \"unit\":\"umol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清球蛋白(计算值)\",\n" +
                    "        \"normalH\":\"40\",\n" +
                    "        \"normalL\":\"20\",\n" +
                    "        \"result\":\"28.6\",\n" +
                    "        \"unit\":\"g/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清碱性磷酸酶\",\n" +
                    "        \"normalH\":\"135\",\n" +
                    "        \"normalL\":\"50\",\n" +
                    "        \"result\":\"87\",\n" +
                    "        \"unit\":\"U/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清γ-谷氨酰基转移酶\",\n" +
                    "        \"normalH\":\"45\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"10\",\n" +
                    "        \"unit\":\"U/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清丙氨酸氨基转移酶\",\n" +
                    "        \"normalH\":\"40\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"15\",\n" +
                    "        \"unit\":\"U/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清天门冬氨酸氨基转移酶\",\n" +
                    "        \"normalH\":\"35\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"17\",\n" +
                    "        \"unit\":\"U/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清总蛋白\",\n" +
                    "        \"normalH\":\"85\",\n" +
                    "        \"normalL\":\"65\",\n" +
                    "        \"result\":\"72.2\",\n" +
                    "        \"unit\":\"g/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清白蛋白/血清球蛋白(计算值)\",\n" +
                    "        \"normalH\":\"2.4\",\n" +
                    "        \"normalL\":\"1.2\",\n" +
                    "        \"result\":\"1.52\",\n" +
                    "        \"unit\":\"/\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清总胆红素\",\n" +
                    "        \"normalH\":\"21\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"9.3\",\n" +
                    "        \"unit\":\"umol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清直接胆红素(结合胆红素)\",\n" +
                    "        \"normalH\":\"8\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"2.9\",\n" +
                    "        \"unit\":\"umol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肝功能11项\",\n" +
                    "        \"itemName\":\"血清白蛋白\",\n" +
                    "        \"normalH\":\"55\",\n" +
                    "        \"normalL\":\"35\",\n" +
                    "        \"result\":\"43.6\",\n" +
                    "        \"unit\":\"g/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿隐血\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿酸碱度\",\n" +
                    "        \"normalH\":\"8\",\n" +
                    "        \"normalL\":\"5\",\n" +
                    "        \"result\":\"6.50\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿白细胞（镜检）\",\n" +
                    "        \"normalH\":\"5\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"未见\",\n" +
                    "        \"unit\":\"个/HP\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿葡萄糖\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿胆红素\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿酮体\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿比重\",\n" +
                    "        \"normalH\":\"1.03\",\n" +
                    "        \"normalL\":\"1.005\",\n" +
                    "        \"result\":\"1.020\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿蛋白质\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿胆原\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    \n" +
                    "    {\n" +
                    "        \"itemName\":\"尿胆红素(BIL)\",\n" +
                    "        \"itemResults\":\"阴性\",\n" +
                    "        \"itemOkResults\":\"阴性\",\n" +
                    "        \"itemUnit\":null,\n" +
                    "        \"testItemFt\":\"★尿液分析\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"bigCategory\":\"\",\n" +
                    "        \"smallCategory\":\"\",\n" +
                    "        \"itemNameComm\":\"尿胆红素（BIL）\",\n" +
                    "        \"resultsDiscrete\":-1,\n" +
                    "        \"unitComm\":null,\n" +
                    "        \"itemNo\":\"3803768\",\n" +
                    "        \"itemId\":null,\n" +
                    "        \"remark\":null,\n" +
                    "        \"cleanStatus\":0,\n" +
                    "        \"normalLOk\":\"阴性\",\n" +
                    "        \"normalHOk\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"itemNo\":null,\n" +
                    "        \"itemName\":\"尿胆原(UBG)\",\n" +
                    "        \"result\":\"+1\",\n" +
                    "        \"category\":\"★尿液分析\",\n" +
                    "        \"unit\":null,\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"normalH\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿亚硝酸盐\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿结晶（镜检）\",\n" +
                    "        \"result\":\"未见\",\n" +
                    "        \"unit\":\"/HP\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿液颜色\",\n" +
                    "        \"normalH\":\"黄色\",\n" +
                    "        \"normalL\":\"淡黄色\",\n" +
                    "        \"result\":\"淡黄色\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿透明度\",\n" +
                    "        \"normalH\":\"透明\",\n" +
                    "        \"normalL\":\"透明\",\n" +
                    "        \"result\":\"透明\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿红细胞（镜检）\",\n" +
                    "        \"normalH\":\"3\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"未见\",\n" +
                    "        \"unit\":\"个/HP\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿白细胞酯酶\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"尿维生素C\",\n" +
                    "        \"result\":\"0.6\",\n" +
                    "        \"unit\":\"mmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"透明管型\",\n" +
                    "        \"normalH\":\"未见\",\n" +
                    "        \"normalL\":\"未见\",\n" +
                    "        \"result\":\"未见\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"尿液分析\",\n" +
                    "        \"itemName\":\"颗粒管型\",\n" +
                    "        \"normalH\":\"未见\",\n" +
                    "        \"normalL\":\"未见\",\n" +
                    "        \"result\":\"未见\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"空腹血糖（Glu）\",\n" +
                    "        \"itemName\":\"空腹血糖\",\n" +
                    "        \"normalH\":\"6.11\",\n" +
                    "        \"normalL\":\"3.89\",\n" +
                    "        \"result\":\"5.35\",\n" +
                    "        \"unit\":\"mmol/L\"\n" +
                    "    },\n" +
                    "    \n" +
                    "    {\n" +
                    "        \"category\":\"白带常规\",\n" +
                    "        \"itemName\":\"霉菌\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"白带常规\",\n" +
                    "        \"itemName\":\"滴虫\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"心肌酶谱三项（CK LDH HBD）\",\n" +
                    "        \"itemName\":\"血清α羟基丁酸脱氢酶\",\n" +
                    "        \"normalH\":\"182\",\n" +
                    "        \"normalL\":\"72\",\n" +
                    "        \"result\":\"126\",\n" +
                    "        \"unit\":\"U/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"心肌酶谱三项（CK LDH HBD）\",\n" +
                    "        \"itemName\":\"血清肌酸激酶\",\n" +
                    "        \"normalH\":\"200\",\n" +
                    "        \"normalL\":\"40\",\n" +
                    "        \"result\":\"79\",\n" +
                    "        \"unit\":\"U/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"心肌酶谱三项（CK LDH HBD）\",\n" +
                    "        \"itemName\":\"血清乳酸脱氢酶\",\n" +
                    "        \"normalH\":\"250\",\n" +
                    "        \"normalL\":\"120\",\n" +
                    "        \"result\":\"151\",\n" +
                    "        \"unit\":\"U/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血脂九项\",\n" +
                    "        \"itemName\":\"血清载脂蛋白A1/载脂蛋白B(计算值)\",\n" +
                    "        \"result\":\"2.44\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血脂九项\",\n" +
                    "        \"itemName\":\"血清甘油三酯\",\n" +
                    "        \"normalH\":\"1.81\",\n" +
                    "        \"normalL\":\"0.4\",\n" +
                    "        \"result\":\"0.50\",\n" +
                    "        \"unit\":\"mmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血脂九项\",\n" +
                    "        \"itemName\":\"血清高密度脂蛋白胆固醇\",\n" +
                    "        \"normalH\":\"2.1\",\n" +
                    "        \"normalL\":\"1\",\n" +
                    "        \"result\":\"1.71\",\n" +
                    "        \"unit\":\"mmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血脂九项\",\n" +
                    "        \"itemName\":\"血清低密度脂蛋白胆固醇\",\n" +
                    "        \"normalH\":\"3.37\",\n" +
                    "        \"normalL\":\"1.5\",\n" +
                    "        \"result\":\"2.32\",\n" +
                    "        \"unit\":\"mmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血脂九项\",\n" +
                    "        \"itemName\":\"血清载脂蛋白AI\",\n" +
                    "        \"normalH\":\"1.6\",\n" +
                    "        \"normalL\":\"1\",\n" +
                    "        \"result\":\"1.71\",\n" +
                    "        \"unit\":\"g/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血脂九项\",\n" +
                    "        \"itemName\":\"血清载脂蛋白B\",\n" +
                    "        \"normalH\":\"1.1\",\n" +
                    "        \"normalL\":\"0.6\",\n" +
                    "        \"result\":\"0.70\",\n" +
                    "        \"unit\":\"g/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血脂九项\",\n" +
                    "        \"itemName\":\"脂蛋白a\",\n" +
                    "        \"normalH\":\"400\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"175\",\n" +
                    "        \"unit\":\"mg/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血脂九项\",\n" +
                    "        \"itemName\":\"血清总胆固醇\",\n" +
                    "        \"normalH\":\"5.2\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"4.55\",\n" +
                    "        \"unit\":\"mmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血脂九项\",\n" +
                    "        \"itemName\":\"动脉粥样硬化指数(计算值）\",\n" +
                    "        \"normalH\":\"4\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"1.66\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肾功三项（BUN Cr UA）\",\n" +
                    "        \"itemName\":\"血清尿素\",\n" +
                    "        \"normalH\":\"7.5\",\n" +
                    "        \"normalL\":\"2.6\",\n" +
                    "        \"result\":\"5.15\",\n" +
                    "        \"unit\":\"mmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肾功三项（BUN Cr UA）\",\n" +
                    "        \"itemName\":\"血清肌酐\",\n" +
                    "        \"normalH\":\"73\",\n" +
                    "        \"normalL\":\"41\",\n" +
                    "        \"result\":\"61.1\",\n" +
                    "        \"unit\":\"μmoI/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肾功三项（BUN Cr UA）\",\n" +
                    "        \"itemName\":\"血清尿酸\",\n" +
                    "        \"normalH\":\"360\",\n" +
                    "        \"normalL\":\"150\",\n" +
                    "        \"result\":\"308\",\n" +
                    "        \"unit\":\"μmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"糖化血红蛋白（HbA1c）\",\n" +
                    "        \"itemName\":\"糖化血红蛋白\",\n" +
                    "        \"normalH\":\"6\",\n" +
                    "        \"normalL\":\"4\",\n" +
                    "        \"result\":\"5.40\",\n" +
                    "        \"unit\":\"%\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"甲状腺功能五项(T3 T4 FT3 FT4 TSH)\",\n" +
                    "        \"itemName\":\"血清游离三碘甲状原氨酸(FT3)\",\n" +
                    "        \"normalH\":\"6.45\",\n" +
                    "        \"normalL\":\"2.76\",\n" +
                    "        \"result\":\"2.84\",\n" +
                    "        \"unit\":\"pmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"甲状腺功能五项(T3 T4 FT3 FT4 TSH)\",\n" +
                    "        \"itemName\":\"血清三碘甲状原氨酸测定(T3)\",\n" +
                    "        \"normalH\":\"2.49\",\n" +
                    "        \"normalL\":\"0.89\",\n" +
                    "        \"result\":\"1.47\",\n" +
                    "        \"unit\":\"nmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"甲状腺功能五项(T3 T4 FT3 FT4 TSH)\",\n" +
                    "        \"itemName\":\"血清促甲状腺激素测定(TSH)\",\n" +
                    "        \"normalH\":\"5.10\",\n" +
                    "        \"normalL\":\"0.35\",\n" +
                    "        \"result\":\"0.393\",\n" +
                    "        \"unit\":\"mIU/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"甲状腺功能五项(T3 T4 FT3 FT4 TSH)\",\n" +
                    "        \"itemName\":\"血清游离甲状腺素测定(FT4)\",\n" +
                    "        \"normalH\":\"23.81\",\n" +
                    "        \"normalL\":\"11.20\",\n" +
                    "        \"result\":\"16.40\",\n" +
                    "        \"unit\":\"pmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"甲状腺功能五项(T3 T4 FT3 FT4 TSH)\",\n" +
                    "        \"itemName\":\"血清甲状腺素测定(T4)\",\n" +
                    "        \"normalH\":\"186.64\",\n" +
                    "        \"normalL\":\"64.36\",\n" +
                    "        \"result\":\"151.60\",\n" +
                    "        \"unit\":\"nmol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"超敏C反应蛋白\",\n" +
                    "        \"itemName\":\"超敏C反应蛋白\",\n" +
                    "        \"normalH\":\"3\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"2.84\",\n" +
                    "        \"unit\":\"mg/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"CK-MB同功酶\",\n" +
                    "        \"itemName\":\"血清肌酸激酶同工酶\",\n" +
                    "        \"normalH\":\"24\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"10.5\",\n" +
                    "        \"unit\":\"U/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"同型半胱氨酸\",\n" +
                    "        \"itemName\":\"血同型半胱氨酸\",\n" +
                    "        \"normalH\":\"20\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"7.64\",\n" +
                    "        \"unit\":\"umol/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"胃功能三项\",\n" +
                    "        \"itemName\":\"胃蛋白酶原Ⅰ\",\n" +
                    "        \"normalH\":\"165\",\n" +
                    "        \"normalL\":\"40\",\n" +
                    "        \"result\":\"105.37\",\n" +
                    "        \"unit\":\"ng/mL\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"胃功能三项\",\n" +
                    "        \"itemName\":\"胃蛋白酶原Ⅱ\",\n" +
                    "        \"normalH\":\"23.0\",\n" +
                    "        \"normalL\":\"3.0\",\n" +
                    "        \"result\":\"14.60\",\n" +
                    "        \"unit\":\"ng/mL\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"胃功能三项\",\n" +
                    "        \"itemName\":\"胃蛋白酶原比值\",\n" +
                    "        \"normalH\":\"100\",\n" +
                    "        \"normalL\":\"3\",\n" +
                    "        \"result\":\"7.22\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV 26（高危型）\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV 40（低危型）\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV 44（低危型）\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-6型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-11型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-16型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-18型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-31型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-33型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-35型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-39型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-42型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-43型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV81\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-45型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-51型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-52型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-53型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-56型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-58型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-59型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-66型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-68型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-73型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"HPV-DNA分型检测\",\n" +
                    "        \"itemName\":\"HPV-DNA-83型\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"血小板分布宽度\",\n" +
                    "        \"normalH\":\"20\",\n" +
                    "        \"normalL\":\"10\",\n" +
                    "        \"result\":\"11.7\",\n" +
                    "        \"unit\":\"%\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"血小板压积\",\n" +
                    "        \"normalH\":\"0.45\",\n" +
                    "        \"normalL\":\"0.07\",\n" +
                    "        \"result\":\"0.22\",\n" +
                    "        \"unit\":\"%\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"平均血小板体积\",\n" +
                    "        \"normalH\":\"13\",\n" +
                    "        \"normalL\":\"5\",\n" +
                    "        \"result\":\"11.1\",\n" +
                    "        \"unit\":\"fL\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"血小板计数\",\n" +
                    "        \"normalH\":\"350\",\n" +
                    "        \"normalL\":\"125\",\n" +
                    "        \"result\":\"196\",\n" +
                    "        \"unit\":\"10^9/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"红细胞体积分布宽度-标准差\",\n" +
                    "        \"normalH\":\"51\",\n" +
                    "        \"normalL\":\"37\",\n" +
                    "        \"result\":\"43.8\",\n" +
                    "        \"unit\":\"fL\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"平均血红蛋白浓度\",\n" +
                    "        \"normalH\":\"354\",\n" +
                    "        \"normalL\":\"316\",\n" +
                    "        \"result\":\"323\",\n" +
                    "        \"unit\":\"g/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"平均血红蛋白含量\",\n" +
                    "        \"normalH\":\"34\",\n" +
                    "        \"normalL\":\"27\",\n" +
                    "        \"result\":\"29.7\",\n" +
                    "        \"unit\":\"pg\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"红细胞平均体积\",\n" +
                    "        \"normalH\":\"100\",\n" +
                    "        \"normalL\":\"82\",\n" +
                    "        \"result\":\"91.9\",\n" +
                    "        \"unit\":\"fL\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"红细胞比容\",\n" +
                    "        \"normalH\":\"45\",\n" +
                    "        \"normalL\":\"35\",\n" +
                    "        \"result\":\"39.9\",\n" +
                    "        \"unit\":\"%\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"血红蛋白\",\n" +
                    "        \"normalH\":\"150\",\n" +
                    "        \"normalL\":\"115\",\n" +
                    "        \"result\":\"129\",\n" +
                    "        \"unit\":\"g/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"红细胞计数\",\n" +
                    "        \"normalH\":\"5.1\",\n" +
                    "        \"normalL\":\"3.8\",\n" +
                    "        \"result\":\"4.34\",\n" +
                    "        \"unit\":\"10^12/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"嗜碱性粒细胞绝对值\",\n" +
                    "        \"normalH\":\"0.06\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"0.01\",\n" +
                    "        \"unit\":\"10^9/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"嗜酸性粒细胞绝对值\",\n" +
                    "        \"normalH\":\"0.52\",\n" +
                    "        \"normalL\":\"0.02\",\n" +
                    "        \"result\":\"0.03\",\n" +
                    "        \"unit\":\"10^9/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"中性粒细胞绝对值\",\n" +
                    "        \"normalH\":\"6.3\",\n" +
                    "        \"normalL\":\"1.8\",\n" +
                    "        \"result\":\"3.08\",\n" +
                    "        \"unit\":\"10^9/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"单核细胞绝对值\",\n" +
                    "        \"normalH\":\"0.6\",\n" +
                    "        \"normalL\":\"0.1\",\n" +
                    "        \"result\":\"0.23\",\n" +
                    "        \"unit\":\"10^9/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"淋巴细胞绝对值\",\n" +
                    "        \"normalH\":\"3.2\",\n" +
                    "        \"normalL\":\"1.1\",\n" +
                    "        \"result\":\"1.26\",\n" +
                    "        \"unit\":\"10^9/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"嗜碱性粒细胞百分数\",\n" +
                    "        \"normalH\":\"1\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"0.2\",\n" +
                    "        \"unit\":\"%\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"嗜酸性粒细胞百分数\",\n" +
                    "        \"normalH\":\"8\",\n" +
                    "        \"normalL\":\"0.4\",\n" +
                    "        \"result\":\"0.7\",\n" +
                    "        \"unit\":\"%\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"中性粒细胞百分数\",\n" +
                    "        \"normalH\":\"75\",\n" +
                    "        \"normalL\":\"40\",\n" +
                    "        \"result\":\"66.8\",\n" +
                    "        \"unit\":\"%\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"单核细胞百分数\",\n" +
                    "        \"normalH\":\"10\",\n" +
                    "        \"normalL\":\"3\",\n" +
                    "        \"result\":\"5.0\",\n" +
                    "        \"unit\":\"%\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"淋巴细胞百分数\",\n" +
                    "        \"normalH\":\"50\",\n" +
                    "        \"normalL\":\"20\",\n" +
                    "        \"result\":\"27.3\",\n" +
                    "        \"unit\":\"%\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"血常规五分类\",\n" +
                    "        \"itemName\":\"白细胞计数\",\n" +
                    "        \"normalH\":\"9.5\",\n" +
                    "        \"normalL\":\"3.5\",\n" +
                    "        \"result\":\"4.61\",\n" +
                    "        \"unit\":\"10^9/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"脂联素\",\n" +
                    "        \"itemName\":\"脂联素\",\n" +
                    "        \"normalH\":\"40\",\n" +
                    "        \"normalL\":\"3.4\",\n" +
                    "        \"result\":\"4.70\",\n" +
                    "        \"unit\":\"mg/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肾损伤早期筛查四项\",\n" +
                    "        \"itemName\":\"尿α1微球蛋白\",\n" +
                    "        \"normalH\":\"24\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"9.11\",\n" +
                    "        \"unit\":\"mg/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肾损伤早期筛查四项\",\n" +
                    "        \"itemName\":\"尿肌酐\",\n" +
                    "        \"result\":\"8492\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肾损伤早期筛查四项\",\n" +
                    "        \"itemName\":\"尿微量白蛋白\",\n" +
                    "        \"normalH\":\"24\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"16.12\",\n" +
                    "        \"unit\":\"mg/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肾损伤早期筛查四项\",\n" +
                    "        \"itemName\":\"尿微量白蛋白/尿肌酐\",\n" +
                    "        \"normalH\":\"3\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"1.9\",\n" +
                    "        \"unit\":\"mg/g\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"粪便隐血（定量）\",\n" +
                    "        \"itemName\":\"粪便隐血试验(定量)\",\n" +
                    "        \"normalH\":\"100\",\n" +
                    "        \"normalL\":\"0\",\n" +
                    "        \"result\":\"1\",\n" +
                    "        \"unit\":\"ng/ml\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"脂蛋白相关磷脂酶A2(LP-PLA2)\",\n" +
                    "        \"itemName\":\"脂蛋白磷脂酶A2\",\n" +
                    "        \"normalH\":\"≤535\",\n" +
                    "        \"result\":\"355.2\",\n" +
                    "        \"unit\":\"U/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"EB病毒衣壳抗原IgA抗体 （VCA-IgA）\",\n" +
                    "        \"normalH\":\"阴性\",\n" +
                    "        \"normalL\":\"阴性\",\n" +
                    "        \"result\":\"阴性\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"糖链抗原CA19-9测定\",\n" +
                    "        \"normalH\":\"30.0\",\n" +
                    "        \"normalL\":\"0.0\",\n" +
                    "        \"result\":\"11.85\",\n" +
                    "        \"unit\":\"U/ml\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"神经元特异烯醇化酶测定(NSE）\",\n" +
                    "        \"normalH\":\"20.0\",\n" +
                    "        \"normalL\":\"0.0\",\n" +
                    "        \"result\":\"9.33\",\n" +
                    "        \"unit\":\"ng/ml\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"癌胚抗原测定(CEA)定量\",\n" +
                    "        \"normalH\":\"5.0\",\n" +
                    "        \"normalL\":\"0.0\",\n" +
                    "        \"result\":\"2.08\",\n" +
                    "        \"unit\":\"ng/ml\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"血清β人绒毛膜促性腺激素测定(β-HCG)\",\n" +
                    "        \"normalH\":\"5.00\",\n" +
                    "        \"normalL\":\"0.0\",\n" +
                    "        \"result\":\"1.08\",\n" +
                    "        \"unit\":\"IU/L\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"甲胎蛋白测定 (AFP)定量\",\n" +
                    "        \"normalH\":\"7.00\",\n" +
                    "        \"normalL\":\"0.00\",\n" +
                    "        \"result\":\"5.98\",\n" +
                    "        \"unit\":\"ng/ml\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"糖链抗原CA125测定\",\n" +
                    "        \"normalH\":\"47.0\",\n" +
                    "        \"normalL\":\"0.0\",\n" +
                    "        \"result\":\"10.50\",\n" +
                    "        \"unit\":\"U/ml\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"糖链抗原CA15-3测定\",\n" +
                    "        \"normalH\":\"20.0\",\n" +
                    "        \"normalL\":\"0.0\",\n" +
                    "        \"result\":\"7.72\",\n" +
                    "        \"unit\":\"U/ml\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"糖链抗原CA50测定\",\n" +
                    "        \"normalH\":\"20.0\",\n" +
                    "        \"normalL\":\"0.0\",\n" +
                    "        \"result\":\"2.95\",\n" +
                    "        \"unit\":\"U/ml\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"糖链抗原CA242测定\",\n" +
                    "        \"normalH\":\"20.0\",\n" +
                    "        \"normalL\":\"0.0\",\n" +
                    "        \"result\":\"4.11\",\n" +
                    "        \"unit\":\"U/ml\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"细胞角蛋白19片段测定(CYFRA21-1)\",\n" +
                    "        \"normalH\":\"4.20\",\n" +
                    "        \"normalL\":\"0.00\",\n" +
                    "        \"result\":\"2.41\",\n" +
                    "        \"unit\":\"ng/ml\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"category\":\"肿瘤12项（女）\",\n" +
                    "        \"itemName\":\"糖链抗原CA72-4测定\",\n" +
                    "        \"normalL\":\"＞7.5\",\n" +
                    "        \"result\":\"3.53\",\n" +
                    "        \"unit\":\"U/ml\"\n" +
                    "    },\n" +
                    "{\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿浊度\",\n" +
                    "                \"result\": \"微浑\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"透明\",\n" +
                    "                \"normalH\": \"透明\"\n" +
                    "            },{\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"白带清洁度\",\n" +
                    "                \"result\": \"III度\",\n" +
                    "                \"category\": \"白带常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"Ⅰ\",\n" +
                    "                \"normalH\": \"Ⅱ\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"液基薄层细胞学\",\n" +
                    "                \"result\": \"鳞状上皮内低度病变（LSIL）;\",\n" +
                    "                \"category\": \"液基薄层细胞学检测\",\n" +
                    "                \"unit\": \"\",\n" +
                    "                \"normalL\": \"未见异常\",\n" +
                    "                \"normalH\": null\n" +
                    "            },{\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"白细胞\",\n" +
                    "                \"result\": \">30\",\n" +
                    "                \"category\": \"阴道分泌物检测\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"0\",\n" +
                    "                \"normalH\": \"15\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"宫颈刮片\",\n" +
                    "                \"result\": \"（宫颈刮片）未见上皮内病变或恶性细胞。\",\n" +
                    "                \"category\": \"宫颈刮片\",\n" +
                    "                \"unit\": \"/\",\n" +
                    "                \"normalL\": \"/\",\n" +
                    "                \"normalH\": null\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"碳13尿素呼气试验\",\n" +
                    "                \"result\": \"2\",\n" +
                    "                \"category\": \"碳13尿素呼气试验\",\n" +
                    "                \"unit\": \"DOB\",\n" +
                    "                \"normalL\": \"-1--3.9\",\n" +
                    "                \"normalH\": null\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"碳14尿素呼气试验\",\n" +
                    "                \"result\": \"00075\",\n" +
                    "                \"category\": \"碳14尿素呼气试验\",\n" +
                    "                \"unit\": \"dpm\",\n" +
                    "                \"normalL\": \"0--99\",\n" +
                    "                \"normalH\": null\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"真菌\",\n" +
                    "                \"result\": \"++++\",\n" +
                    "                \"category\": \"阴道分泌物检测\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"阴性\",\n" +
                    "                \"normalH\": \"阴性\"\n" +
                    "            },\n" +
                    "{\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿维生素C(VC)\",\n" +
                    "                \"result\": \"-\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": null,\n" +
                    "                \"normalH\": null\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿白细胞酯酶(LEU)\",\n" +
                    "                \"result\": \"-\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"阴性\",\n" +
                    "                \"normalH\": \"阴性\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿酮体(KET)\",\n" +
                    "                \"result\": \"-\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"阴性\",\n" +
                    "                \"normalH\": \"阴性\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿亚硝酸盐(NIT)\",\n" +
                    "                \"result\": \"-\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"阴性\",\n" +
                    "                \"normalH\": \"阴性\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿胆原 (URO)\",\n" +
                    "                \"result\": \"阴性\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"阴性\",\n" +
                    "                \"normalH\": \"弱阳性\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿胆红素(BIL)\",\n" +
                    "                \"result\": \"-\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"阴性\",\n" +
                    "                \"normalH\": \"阴性\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿蛋白质(PRO)\",\n" +
                    "                \"result\": \"-\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"阴性\",\n" +
                    "                \"normalH\": \"阴性\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿葡萄糖(GLU)\",\n" +
                    "                \"result\": \"-\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"阴性\",\n" +
                    "                \"normalH\": \"阴性\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿比重(SG)\",\n" +
                    "                \"result\": \"1.030\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"1.003\",\n" +
                    "                \"normalH\": \"1.030\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿隐血（BLD）\",\n" +
                    "                \"result\": \"-\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"阴性\",\n" +
                    "                \"normalH\": \"阴性\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿酸碱度(PH)\",\n" +
                    "                \"result\": \"5.5\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"5.0\",\n" +
                    "                \"normalH\": \"8.4\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿液颜色（UCO）\",\n" +
                    "                \"result\": \"黄色\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"淡黄色\",\n" +
                    "                \"normalH\": \"黄色\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿液透明度\",\n" +
                    "                \"result\": \"透明\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"透明\",\n" +
                    "                \"normalH\": \"透明\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿白细胞（镜检）\",\n" +
                    "                \"result\": \"未见\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"0\",\n" +
                    "                \"normalH\": \"5\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿红细胞（镜检）\",\n" +
                    "                \"result\": \"未见\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"0\",\n" +
                    "                \"normalH\": \"3\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿结晶（镜检）\",\n" +
                    "                \"result\": \"未见\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": null,\n" +
                    "                \"normalH\": null\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"尿管型（镜检）\",\n" +
                    "                \"result\": \"未见\",\n" +
                    "                \"category\": \"尿常规\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": null,\n" +
                    "                \"normalH\": null\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"空腹血糖\",\n" +
                    "                \"result\": \"6.24\",\n" +
                    "                \"category\": \"空腹血糖（GlU)\",\n" +
                    "                \"unit\": \"mmol/L\",\n" +
                    "                \"normalL\": \"3.9\",\n" +
                    "                \"normalH\": \"6.1\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清碱性磷酸酶\",\n" +
                    "                \"result\": \"83.0\",\n" +
                    "                \"category\": \"血清碱性磷酸酶测定（ALP）\",\n" +
                    "                \"unit\": \"u/L\",\n" +
                    "                \"normalL\": \"45\",\n" +
                    "                \"normalH\": \"125\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清尿素\",\n" +
                    "                \"result\": \"10.67\",\n" +
                    "                \"category\": \"肾功三项（BUN、Cr、UA）\",\n" +
                    "                \"unit\": \"mmol/L\",\n" +
                    "                \"normalL\": \"2.50\",\n" +
                    "                \"normalH\": \"8.30\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清肌酐\",\n" +
                    "                \"result\": \"79.7\",\n" +
                    "                \"category\": \"肾功三项（BUN、Cr、UA）\",\n" +
                    "                \"unit\": \"umol/L\",\n" +
                    "                \"normalL\": \"35\",\n" +
                    "                \"normalH\": \"123\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清尿酸\",\n" +
                    "                \"result\": \"414.30\",\n" +
                    "                \"category\": \"肾功三项（BUN、Cr、UA）\",\n" +
                    "                \"unit\": \"umol/L\",\n" +
                    "                \"normalL\": \"160.00\",\n" +
                    "                \"normalH\": \"430.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清丙氨酸氨基转移酶\",\n" +
                    "                \"result\": \"42\",\n" +
                    "                \"category\": \"肝功一项(ALT)\",\n" +
                    "                \"unit\": \"u/L\",\n" +
                    "                \"normalL\": \"7\",\n" +
                    "                \"normalH\": \"50\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清白蛋白\",\n" +
                    "                \"result\": \"47.0\",\n" +
                    "                \"category\": \"血清白蛋白测定（Alb）\",\n" +
                    "                \"unit\": \"g/L\",\n" +
                    "                \"normalL\": \"35\",\n" +
                    "                \"normalH\": \"55\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清总胆红素\",\n" +
                    "                \"result\": \"6.5\",\n" +
                    "                \"category\": \"血清总胆红素测定（T-Bil）\",\n" +
                    "                \"unit\": \"umol/L\",\n" +
                    "                \"normalL\": \"3.40\",\n" +
                    "                \"normalH\": \"24.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清间接胆红素(计算值)(非结合胆红素)\",\n" +
                    "                \"result\": \"4.9\",\n" +
                    "                \"category\": \"血清间接胆红素（I-Bil）（计算值）（非结\",\n" +
                    "                \"unit\": \"umol/L\",\n" +
                    "                \"normalL\": \"1.7\",\n" +
                    "                \"normalH\": \"20\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清球蛋白(计算值)\",\n" +
                    "                \"result\": \"29.9\",\n" +
                    "                \"category\": \"血清球蛋白（计算值）\",\n" +
                    "                \"unit\": \"g/L\",\n" +
                    "                \"normalL\": \"25.00\",\n" +
                    "                \"normalH\": \"35.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清总蛋白\",\n" +
                    "                \"result\": \"76.9\",\n" +
                    "                \"category\": \"血清总蛋白测定（TP）\",\n" +
                    "                \"unit\": \"g/L\",\n" +
                    "                \"normalL\": \"60\",\n" +
                    "                \"normalH\": \"85\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清直接胆红素(结合胆红素)\",\n" +
                    "                \"result\": \"1.6\",\n" +
                    "                \"category\": \"血清直接胆红素测定（D-Bil）（结合胆红素\",\n" +
                    "                \"unit\": \"umol/L\",\n" +
                    "                \"normalL\": \"0.00\",\n" +
                    "                \"normalH\": \"6.80\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清γ-谷氨酰基转移酶\",\n" +
                    "                \"result\": \"27\",\n" +
                    "                \"category\": \"血清r-谷氨酰转肽酶\",\n" +
                    "                \"unit\": \"u/L\",\n" +
                    "                \"normalL\": \"10.00\",\n" +
                    "                \"normalH\": \"60.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清天门冬氨酸氨基转移酶\",\n" +
                    "                \"result\": \"26\",\n" +
                    "                \"category\": \"血清天门冬氨酸氨基转移酶测定（AST)\",\n" +
                    "                \"unit\": \"u/L\",\n" +
                    "                \"normalL\": \"1\",\n" +
                    "                \"normalH\": \"40\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清白蛋白/血清球蛋白(计算值)\",\n" +
                    "                \"result\": \"1.6\",\n" +
                    "                \"category\": \"血清白/球比值\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"1.20\",\n" +
                    "                \"normalH\": \"2.50\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清总胆固醇\",\n" +
                    "                \"result\": \"6.14\",\n" +
                    "                \"category\": \"血清总胆固醇测定（TC）\",\n" +
                    "                \"unit\": \"mmol/L\",\n" +
                    "                \"normalL\": \"2.33\",\n" +
                    "                \"normalH\": \"5.7\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清甘油三酯\",\n" +
                    "                \"result\": \"1.89\",\n" +
                    "                \"category\": \"血清甘油三酯测定（TG）\",\n" +
                    "                \"unit\": \"mmol/L\",\n" +
                    "                \"normalL\": \"0.45\",\n" +
                    "                \"normalH\": \"1.80\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清高密度脂蛋白胆固醇\",\n" +
                    "                \"result\": \"1.24\",\n" +
                    "                \"category\": \"血清高密度脂蛋白胆固醇测定（HDL-C）\",\n" +
                    "                \"unit\": \"mmol/L\",\n" +
                    "                \"normalL\": \"0.90\",\n" +
                    "                \"normalH\": \"1.96\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清低密度脂蛋白胆固醇\",\n" +
                    "                \"result\": \"4.26\",\n" +
                    "                \"category\": \"血清低密度脂蛋白胆固醇测定（LDL-C）\",\n" +
                    "                \"unit\": \"mmol/L\",\n" +
                    "                \"normalL\": \"1.3\",\n" +
                    "                \"normalH\": \"3.37\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"总前列腺特异性抗原\",\n" +
                    "                \"result\": \"0.530\",\n" +
                    "                \"category\": \"T5（男）\",\n" +
                    "                \"unit\": \"ng/mL\",\n" +
                    "                \"normalL\": \"0\",\n" +
                    "                \"normalH\": \"4\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"癌胚抗原（定量）\",\n" +
                    "                \"result\": \"1.020\",\n" +
                    "                \"category\": \"T5（男）\",\n" +
                    "                \"unit\": \"ng/ml\",\n" +
                    "                \"normalL\": \"0\",\n" +
                    "                \"normalH\": \"5\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"甲胎蛋白（定量）\",\n" +
                    "                \"result\": \"3.960\",\n" +
                    "                \"category\": \"T5（男）\",\n" +
                    "                \"unit\": \"ng/mL\",\n" +
                    "                \"normalL\": null,\n" +
                    "                \"normalH\": \"≤10\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"游离前列腺特异性抗原\",\n" +
                    "                \"result\": \"0.230\",\n" +
                    "                \"category\": \"T5（男）\",\n" +
                    "                \"unit\": \"ng/mL\",\n" +
                    "                \"normalL\": \"0\",\n" +
                    "                \"normalH\": \"1\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"糖链抗原CA19-9\",\n" +
                    "                \"result\": \"7.550\",\n" +
                    "                \"category\": \"T5（男）\",\n" +
                    "                \"unit\": \"U/mL\",\n" +
                    "                \"normalL\": null,\n" +
                    "                \"normalH\": \"≤35\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"单核细胞百分数\",\n" +
                    "                \"result\": \"5.9\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"%\",\n" +
                    "                \"normalL\": \"3.00\",\n" +
                    "                \"normalH\": \"10.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"单核细胞绝对值\",\n" +
                    "                \"result\": \"0.40\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"10^9/L\",\n" +
                    "                \"normalL\": \"0.12\",\n" +
                    "                \"normalH\": \"1.20\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"嗜酸性粒细胞百分数\",\n" +
                    "                \"result\": \"3.8\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"%\",\n" +
                    "                \"normalL\": \"0.50\",\n" +
                    "                \"normalH\": \"5.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"嗜酸性粒细胞绝对值\",\n" +
                    "                \"result\": \"0.26\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"10^9/L\",\n" +
                    "                \"normalL\": \"0.02\",\n" +
                    "                \"normalH\": \"0.50\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"嗜碱性粒细胞百分数\",\n" +
                    "                \"result\": \"0.50\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"%\",\n" +
                    "                \"normalL\": \"0.00\",\n" +
                    "                \"normalH\": \"1.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"嗜碱性粒细胞绝对值\",\n" +
                    "                \"result\": \"0.03\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"10^9/L\",\n" +
                    "                \"normalL\": \"0.00\",\n" +
                    "                \"normalH\": \"0.10\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"白细胞计数\",\n" +
                    "                \"result\": \"6.91\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"10^9/L\",\n" +
                    "                \"normalL\": \"4.00\",\n" +
                    "                \"normalH\": \"10.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"红细胞计数\",\n" +
                    "                \"result\": \"5.18\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"10^12/L\",\n" +
                    "                \"normalL\": \"4.00\",\n" +
                    "                \"normalH\": \"5.70\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血红蛋白\",\n" +
                    "                \"result\": \"159\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"g/L\",\n" +
                    "                \"normalL\": \"120\",\n" +
                    "                \"normalH\": \"170\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"红细胞比容\",\n" +
                    "                \"result\": \"48.1\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"%\",\n" +
                    "                \"normalL\": \"34\",\n" +
                    "                \"normalH\": \"49\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"红细胞平均体积\",\n" +
                    "                \"result\": \"92.8\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"fl\",\n" +
                    "                \"normalL\": \"80.00\",\n" +
                    "                \"normalH\": \"100.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"平均血红蛋白含量\",\n" +
                    "                \"result\": \"30.7\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"pg\",\n" +
                    "                \"normalL\": \"27.0\",\n" +
                    "                \"normalH\": \"34.0\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"平均血红蛋白浓度\",\n" +
                    "                \"result\": \"331\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"g/L\",\n" +
                    "                \"normalL\": \"320\",\n" +
                    "                \"normalH\": \"360\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血小板计数\",\n" +
                    "                \"result\": \"265\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"10^9/L\",\n" +
                    "                \"normalL\": \"100\",\n" +
                    "                \"normalH\": \"300\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"中性粒细胞百分数\",\n" +
                    "                \"result\": \"55.9\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"%\",\n" +
                    "                \"normalL\": \"50\",\n" +
                    "                \"normalH\": \"70\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"中性粒细胞绝对值\",\n" +
                    "                \"result\": \"3.87\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"10^9/L\",\n" +
                    "                \"normalL\": \"2.00\",\n" +
                    "                \"normalH\": \"7.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"淋巴细胞百分数\",\n" +
                    "                \"result\": \"33.9\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"%\",\n" +
                    "                \"normalL\": \"20.00\",\n" +
                    "                \"normalH\": \"40.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"淋巴细胞绝对值\",\n" +
                    "                \"result\": \"2.35\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"10^9/L\",\n" +
                    "                \"normalL\": \"0.80\",\n" +
                    "                \"normalH\": \"4.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"红细胞体积分布宽度-标准差\",\n" +
                    "                \"result\": \"46.4\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"fl\",\n" +
                    "                \"normalL\": \"35.0\",\n" +
                    "                \"normalH\": \"56.0\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"红细胞体积分布宽度-变异系数\",\n" +
                    "                \"result\": \"13.8\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"%\",\n" +
                    "                \"normalL\": \"11.0\",\n" +
                    "                \"normalH\": \"16.0\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血小板分布宽度\",\n" +
                    "                \"result\": \"10.8\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"9.0\",\n" +
                    "                \"normalH\": \"17.0\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"平均血小板体积\",\n" +
                    "                \"result\": \"10.0\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"fl\",\n" +
                    "                \"normalL\": \"6.5\",\n" +
                    "                \"normalH\": \"12.0\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"大血小板比率\",\n" +
                    "                \"result\": \"21.6\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"%\",\n" +
                    "                \"normalL\": \"9.00\",\n" +
                    "                \"normalH\": \"45.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"大血小板数\",\n" +
                    "                \"result\": \"57\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"10^9/L\",\n" +
                    "                \"normalL\": \"13\",\n" +
                    "                \"normalH\": \"129\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血小板压积\",\n" +
                    "                \"result\": \"0.27\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"%\",\n" +
                    "                \"normalL\": \"0.10\",\n" +
                    "                \"normalH\": \"0.28\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"动脉粥样硬化指数(计算值)\",\n" +
                    "                \"result\": \"3.95\",\n" +
                    "                \"category\": \"动脉粥样硬化指数\",\n" +
                    "                \"unit\": null,\n" +
                    "                \"normalL\": \"0.00\",\n" +
                    "                \"normalH\": \"4.00\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清低密度脂蛋白胆固醇\",\n" +
                    "                \"result\": \"3.61\",\n" +
                    "                \"category\": \"血脂六项\",\n" +
                    "                \"unit\": \"mmol/L\",\n" +
                    "                \"normalL\": \"<3.37\",\n" +
                    "                \"normalH\": \"\"\n" +
                    "            }\n" +
                    ",\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"镜检管型\",\n" +
                    "                \"result\": null,\n" +
                    "                \"category\": \"尿液分析1+镜检\",\n" +
                    "                \"unit\": \"\",\n" +
                    "                \"normalL\": \"0\",\n" +
                    "                \"normalH\": \"1\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血小板压积\",\n" +
                    "                \"result\": \"0.27\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"%\",\n" +
                    "                \"normalL\": null,\n" +
                    "                \"normalH\": \">0.25\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血小板压积\",\n" +
                    "                \"result\": \"0.27\",\n" +
                    "                \"category\": \"血常规（五分类）\",\n" +
                    "                \"unit\": \"mmoI/L\",\n" +
                    "                \"normalL\": \">0.25\",\n" +
                    "                \"normalH\": \"\"\n" +
                    "            },{\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"嗜碱性粒细胞绝对值\",\n" +
                    "                \"result\": \"0.050\",\n" +
                    "                \"category\": \"血常规-5分类\",\n" +
                    "                \"normalL\": \"≤0.06\",\n" +
                    "                \"normalH\": \"\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"itemNo\": null,\n" +
                    "                \"itemName\": \"血清低密度脂蛋白胆固醇\",\n" +
                    "                \"result\": \"1.015-6.0\",\n" +
                    "                \"category\": \"血脂六项\",\n" +
                    "                \"unit\": \"mmol/L\",\n" +
                    "                \"normalL\": \"7\",\n" +
                    "                \"normalH\": \"10\"\n" +
                    "            }\n" +
                    "]";
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
}
