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
    @RequestMapping(value = "/meinian/easyPushData", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult easyPushData(@RequestBody DataParam dataParam) {
        String mark = dataParam.getMark();
        Integer limit = dataParam.getLimit();
        if (StrUtil.isBlank(mark)) {
            mark = "mq";
        }
        if (limit == null) {
            limit = 1;
        }
        for (int i = 0; i < limit; i++) {
            MarketData ord = new MarketData();
            String checkData = "[{\"category\":\"甲状腺彩超\",\"itemName\":\"描述\",\"result\":\"甲状腺大小形态正常，包膜光整，甲状腺右叶中极可见一囊性结节，大小约3mmx2mm，边缘清晰，后方回声增强。CDFI：甲状腺实质血流信号未见明显异常。\",\"unit\":\"\"},{\"category\":\"甲状腺彩超\",\"itemName\":\"小结\",\"result\":\"甲状腺右叶囊性结节,（ACR TI-RADS 1类）\",\"unit\":\"\"},{\"category\":\"总检\",\"itemName\":\"总检\",\"result\":\"★ 一般检查结果:\\r\\n   体重指数:25.70\\r\\n   血压:147/74mmHg\\r\\n\\r\\n★ 眼科检查结果:\\r\\n   裸眼视力右:0.6\\r\\n   裸眼视力左:0.8\\r\\n\\r\\n★ 眼底照相及AI人工智能结果：\\r\\n   右眼眼底检查可见异常\\r\\n   右眼视杯扩大\\r\\n\\r\\n★ 口腔科检查结果:\\r\\n   牙楔状缺损\\r\\n\\r\\n★ 心电图检查结果:\\r\\n   窦性心律\\r\\n   心电轴左偏\\r\\n\\r\\n★ 甲状腺彩超:\\r\\n   甲状腺右叶囊性结节,（ACR TI-RADS 1类）   建议随访复查\\r\\n\\r\\n★ 女性盆腔彩超:\\r\\n   绝经后子宫\\r\\n   双附件显示不清\\r\\n\\r\\n★ 低剂量胸部CT扫描（不出片）#:\\r\\n   肺结节影\\r\\n   肺纤维灶\\r\\n   胸膜增厚\\r\\n   主动脉部分管壁钙化\\r\\n\\r\\n★ 超声骨密度检查结果:\\r\\n   骨质疏松\\r\\n\\r\\n★ 尿隐血（BLD） 阳性:  (结果:阳性(1+))\\r\\n\\r\\n★ 白带清洁度 偏高:  (结果:III° 范围：Ⅰ-Ⅱ)\\r\\n\\r\\n★ 血清γ-谷氨酰基转移酶测定（GGT) 增高:  (结果:45.70 范围：7-45 U/L)\\r\\n\\r\\n★ 血清总胆固醇测定(TC) 增高:  (结果:5.54 范围：0-5.17 mmol/L)\\r\\n★ 血清低密度脂蛋白胆固醇测定(LDL-C)增高:  (结果:3.52 范围：0-3.37 mmol/L)\",\"unit\":\"\"},{\"category\":\"一般检查\",\"itemName\":\"身高\",\"result\":\"149\",\"unit\":\"Cm\"},{\"category\":\"一般检查\",\"itemName\":\"体重\",\"result\":\"57\",\"unit\":\"kg\"},{\"category\":\"一般检查\",\"itemName\":\"体重指数(18.5-24)\",\"result\":\"25.70\",\"unit\":\"\"},{\"category\":\"一般检查\",\"itemName\":\"收缩压\",\"result\":\"147\",\"unit\":\"mmHg\"},{\"category\":\"一般检查\",\"itemName\":\"舒张压\",\"result\":\"74\",\"unit\":\"mmHg\"},{\"category\":\"一般检查\",\"itemName\":\"小结\",\"result\":\"体重指数:25.70\\r\\n血压:147/74mmHg\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"既往疾病史\",\"result\":\"甲状腺功能亢进（治疗中）\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"家族史\",\"result\":\"无\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"心率\",\"result\":\"62次/分\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"心律\",\"result\":\"整齐\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"心界\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"心音\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"心脏杂音\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"肺罗音\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"肺部其他\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"呼吸音\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"语音震颤\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"腹壁\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"肝大小\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"肝脏质地\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"肝脏压痛\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"脾大小\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"脾脏质地\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"脾脏压痛\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"神经浅反射\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"其他\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"内科检查\",\"itemName\":\"小结\",\"result\":\"内科检查未发现明显异常\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"裸眼视力右\",\"result\":\"0.6\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"裸眼视力左\",\"result\":\"0.8\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"矫正视力右\",\"result\":\"\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"矫正视力左\",\"result\":\"\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"色觉\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"眼睑\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"眼球\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"结膜\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"巩膜\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"其它\",\"result\":\"\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"角膜\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"虹膜\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"晶体\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"瞳孔\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"眼科检查\",\"itemName\":\"小结\",\"result\":\"裸眼视力右:0.6\\r\\n裸眼视力左:0.8\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"耳廓\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"外耳道\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"鼓膜\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"乳突（耳）\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"鼻外形\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"鼻中隔\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"鼻腔黏膜及分泌物\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"鼻甲\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"鼻窦压痛\",\"result\":\"无\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"口咽黏膜\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"口咽悬雍垂\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"扁桃体\",\"result\":\"正常\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"其它\",\"result\":\"\",\"unit\":\"\"},{\"category\":\"耳鼻喉检查\",\"itemName\":\"小结\",\"result\":\"耳鼻喉检查未见异常\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"口腔黏膜\",\"result\":\"未见明显异常\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"牙龈\",\"result\":\"未见明显异常\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"龋齿\",\"result\":\"无\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"缺齿\",\"result\":\"无\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"义齿\",\"result\":\"无\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"牙周\",\"result\":\"未见明显异常\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"牙齿松动\",\"result\":\"无\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"颞下颌关节\",\"result\":\"未见明显异常\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"腮腺\",\"result\":\"未见明显异常\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"其他\",\"result\":\"有牙楔状缺损\",\"unit\":\"\"},{\"category\":\"口腔检查\",\"itemName\":\"小结\",\"result\":\"牙楔状缺损\",\"unit\":\"\"},{\"category\":\"心电图\",\"itemName\":\"小结\",\"result\":\"窦性心律\\r\\n心电轴左偏\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"肝\",\"result\":\"肝脏形态大小正常，轮廓规整，实质回声均匀，肝内管道结构清晰。门静脉不宽。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"胆\",\"result\":\"胆囊形态大小正常，壁光滑，内未见明显异常光团。胆总管不扩张。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"胰\",\"result\":\"胰腺形态大小正常，轮廓规整，实质回声均匀，主胰管不扩张。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"脾\",\"result\":\"脾脏形态正常，包膜光整，实质回声均匀。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"小结\",\"result\":\"肝脏未见明显异常\\r\\n胆囊未见明显异常\\r\\n胰腺未见明显异常\\r\\n脾脏未见明显异常\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"左肾\",\"result\":\"左肾形态大小正常，包膜光整，实质回声均匀。集合系统未见分离。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"右肾\",\"result\":\"右肾形态大小正常，包膜光整，实质回声均匀。集合系统未见分离。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"膀胱\",\"result\":\"膀胱充盈良好，壁光整，腔内未见异常回声。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"双侧输尿管\",\"result\":\"双侧输尿管未见明显异常光团。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"小结\",\"result\":\"左肾未见明显异常\\r\\n右肾未见明显异常\\r\\n膀胱未见明显异常\\r\\n双侧输尿管未见明显异常\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"子宫\",\"result\":\"子宫体积缩小，轮廓光整，实质回声均匀，内膜不厚，未见明显异常回声。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"左附件\",\"result\":\"左附件显示不清。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"右附件\",\"result\":\"右附件显示不清。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"小结\",\"result\":\"绝经后子宫\\r\\n左附件显示不清\\r\\n右附件显示不清\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"描述\",\"result\":\"双侧乳腺形态轮廓正常，层次清晰，腺体无增厚，分布均匀，腺管无扩张，未见明显异常回声。CDFI：腺体内未见明显异常血流信号。\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"小结\",\"result\":\"双侧乳腺未见明显异常\",\"unit\":\"\"},{\"category\":\"影像学检查\",\"itemName\":\"小结\",\"result\":\"骨质疏松\",\"unit\":\"\"}]";
            String testData = "[{\"category\":\"实验室检查\",\"itemName\":\"病史\",\"result\":\"无\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"外阴\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"阴道\",\"result\":\"通畅\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"宫颈\",\"result\":\"光滑\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"子宫\",\"result\":\"萎缩\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"右侧附件\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"左侧附件\",\"result\":\"未见异常\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"小结\",\"result\":\"妇科检查未发现明显异常\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"血清丙氨酸氨基转移酶测定(ALT)\",\"normalH\":\"40\",\"normalL\":\"7\",\"result\":\"14.80\",\"unit\":\"U/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清天门冬氨酸氨基转移酶测定(AST)\",\"normalH\":\"35\",\"normalL\":\"13\",\"result\":\"20.90\",\"unit\":\"U/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清γ-谷氨酰基转移酶测定（γ-GT)\",\"normalH\":\"45\",\"normalL\":\"7\",\"result\":\"45.70\",\"unit\":\"U/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清碱性磷酸酶测定(ALP)\",\"normalH\":\"135\",\"normalL\":\"50\",\"result\":\"113.20\",\"unit\":\"U/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清总蛋白测定(TP)\",\"normalH\":\"85\",\"normalL\":\"65\",\"result\":\"78.70\",\"unit\":\"g/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清白蛋白测定(Alb)\",\"normalH\":\"55\",\"normalL\":\"40\",\"result\":\"44.70\",\"unit\":\"g/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清球蛋白（计算值）\",\"normalH\":\"40\",\"normalL\":\"20\",\"result\":\"34.00\",\"unit\":\"g/L\"},{\"category\":\"实验室检查\",\"itemName\":\"白蛋白/球蛋白（计算值）\",\"normalH\":\"2.4\",\"normalL\":\"1.2\",\"result\":\"1.31\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"血清总胆红素测定(T-Bil)\",\"normalH\":\"23\",\"normalL\":\"0\",\"result\":\"11.98\",\"unit\":\"μmol/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清直接胆红素测定(D-Bil)（结合胆红素）\",\"normalH\":\"6.8\",\"normalL\":\"0.01\",\"result\":\"2.86\",\"unit\":\"μmol/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清间接胆红素(I-Bil)（计算值）（非结合胆红素）\",\"normalH\":\"17\",\"normalL\":\"0\",\"result\":\"9.12\",\"unit\":\"μmol/L\"},{\"category\":\"实验室检查\",\"itemName\":\"人乳头瘤病毒基因分型(HPV-16/18)(高危型)定量\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阴性\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"血清尿素测定(Urea)\",\"normalH\":\"8.8\",\"normalL\":\"2.6\",\"result\":\"4.30\",\"unit\":\"mmol/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清肌酐测定(CREA)\",\"normalH\":\"73\",\"normalL\":\"41\",\"result\":\"46.95\",\"unit\":\"μmol/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清尿酸测定\",\"normalH\":\"360\",\"normalL\":\"150\",\"result\":\"351.30\",\"unit\":\"μmol/L\"},{\"category\":\"实验室检查\",\"itemName\":\"总胆固醇/高密度脂蛋白\",\"normalH\":\"5\",\"normalL\":\"0\",\"result\":\"3.09\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"血清甘油三酯测定(TG)\",\"normalH\":\"2.3\",\"normalL\":\"0\",\"result\":\"0.94\",\"unit\":\"mmol/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清总胆固醇测定(TC)\",\"normalH\":\"5.17\",\"normalL\":\"0\",\"result\":\"5.54\",\"unit\":\"mmol/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清高密度脂蛋白胆固醇测定(HDL-C)\",\"normalH\":\"\",\"normalL\":\"≥0.91\",\"result\":\"1.79\",\"unit\":\"mmol/L\"},{\"category\":\"实验室检查\",\"itemName\":\"血清低密度脂蛋白胆固醇测定(LDL-C)\",\"normalH\":\"3.37\",\"normalL\":\"0\",\"result\":\"3.52\",\"unit\":\"mmol/L\"},{\"category\":\"实验室检查\",\"itemName\":\"白带清洁度\",\"normalH\":\"\",\"normalL\":\"Ⅰ/Ⅱ\",\"result\":\"III°\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"脓细胞（分泌物）\",\"normalH\":\"15/HP\",\"normalL\":\"0\",\"result\":\"2-3\",\"unit\":\"\"},{\"category\":\"实验室检查\",\"itemName\":\"红细胞（分泌物）\",\"normalH\":\"15/HP\",\"normalL\":\"0\",\"result\":\"未见\",\"unit\":\"/HP\"},{\"category\":\"实验室检查\",\"itemName\":\"真菌（镜检）\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"未见\",\"unit\":\"/HP\"},{\"category\":\"实验室检查\",\"itemName\":\"滴虫（镜检）\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"未见\",\"unit\":\"/HP\"},{\"category\":\"实验室检查\",\"itemName\":\"线索细胞（镜检）\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"未见\",\"unit\":\"/HP\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"EB病毒抗体定性(EBV-VCAIGA)\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阴性(-)\",\"unit\":\"\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"糖链抗原CA15-3测定\",\"normalH\":\"30\",\"normalL\":\"0\",\"result\":\"5.45\",\"unit\":\"IU/mL\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"神经元特异烯醇化酶测定(NSE)\",\"normalH\":\"20\",\"normalL\":\"0\",\"result\":\"12.29\",\"unit\":\"ng/mL\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"癌胚抗原测定(CEA)定量\",\"normalH\":\"5\",\"normalL\":\"0\",\"result\":\"2.09\",\"unit\":\"ng/ml\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"甲胎蛋白测定 (AFP)定量\",\"normalH\":\"10\",\"normalL\":\"0\",\"result\":\"2.48\",\"unit\":\"ng/ml\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"糖链抗原CA125测定\",\"normalH\":\"34\",\"normalL\":\"0\",\"result\":\"2.85\",\"unit\":\"IU/mL\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"鳞状细胞癌相关抗原测定(SCC)\",\"normalH\":\"1.2\",\"normalL\":\"0\",\"result\":\"0.11\",\"unit\":\"ng/mL\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"糖链抗原CA242测定\",\"normalH\":\"20\",\"normalL\":\"0\",\"result\":\"7.13\",\"unit\":\"IU/mL\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"糖链抗原CA19-9测定\",\"normalH\":\"35\",\"normalL\":\"0\",\"result\":\"11.40\",\"unit\":\"IU/mL\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"糖链抗原CA72-4测定\",\"normalH\":\"10\",\"normalL\":\"0\",\"result\":\"3.53\",\"unit\":\"IU/mL\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"细胞角蛋白19片段测定(CYFRA21-1)\",\"normalH\":\"3\",\"normalL\":\"0\",\"result\":\"1.04\",\"unit\":\"ng/mL\"},{\"category\":\"肿瘤筛查\",\"itemName\":\"血清β人绒毛膜促性腺激素测定(β-HCG)\",\"normalH\":\"5\",\"normalL\":\"0\",\"result\":\"0.71 0--5\",\"unit\":\"mIU/ml\"},{\"category\":\"尿常规\",\"itemName\":\"尿胆原(UBG)\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阴性\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿葡萄糖(GLU)\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阴性\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿酮体(KET)\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阴性\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿胆红素(BIL)\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阴性\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿蛋白质(PRO)\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阴性\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿亚硝酸盐(NIT)\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阴性\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿酸碱度(PH)\",\"normalH\":\"8\",\"normalL\":\"4.5\",\"result\":\"6.0\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿隐血(RBC)\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阳性(1+)\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿比重(SG)\",\"normalH\":\"1.03\",\"normalL\":\"1.003\",\"result\":\"1.015\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿白细胞(LEU)\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阴性\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿维生素C(VC)\",\"normalH\":\"\",\"normalL\":\"阴性\",\"result\":\"阴性\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"尿白细胞（镜检）\",\"normalH\":\"5\",\"normalL\":\"0\",\"result\":\"未见\",\"unit\":\"/HP\"},{\"category\":\"尿常规\",\"itemName\":\"尿红细胞（镜检）\",\"normalH\":\"3\",\"normalL\":\"0\",\"result\":\"2-3\",\"unit\":\"/HP\"},{\"category\":\"尿常规\",\"itemName\":\"尿管型（镜检）\",\"normalH\":\"\",\"normalL\":\"无\",\"result\":\"未见\",\"unit\":\"个/LP\"},{\"category\":\"尿常规\",\"itemName\":\"尿结晶\",\"normalH\":\"\",\"normalL\":\"无\",\"result\":\"未见\",\"unit\":\"\"},{\"category\":\"尿常规\",\"itemName\":\"其他\",\"normalH\":\"\",\"normalL\":\"无\",\"result\":\"未见\",\"unit\":\"\"},{\"category\":\"血液分析\",\"itemName\":\"白细胞计数(WBC)\",\"normalH\":\"9.5\",\"normalL\":\"3.5\",\"result\":\"4.79\",\"unit\":\"10^9/L\"},{\"category\":\"血液分析\",\"itemName\":\"中性粒细胞绝对值(NEUT)\",\"normalH\":\"6.3\",\"normalL\":\"1.8\",\"result\":\"2.49\",\"unit\":\"10^9/L\"},{\"category\":\"血液分析\",\"itemName\":\"淋巴细胞绝对值(LYM)\",\"normalH\":\"3.2\",\"normalL\":\"1.1\",\"result\":\"1.99\",\"unit\":\"10^9/L\"},{\"category\":\"血液分析\",\"itemName\":\"单核细胞绝对值(MON)\",\"normalH\":\"0.6\",\"normalL\":\"0.1\",\"result\":\"0.27\",\"unit\":\"10^9/L\"},{\"category\":\"血液分析\",\"itemName\":\"嗜酸性粒细胞绝对值(EO)\",\"normalH\":\"0.52\",\"normalL\":\"0.02\",\"result\":\"0.03\",\"unit\":\"10^9/L\"},{\"category\":\"血液分析\",\"itemName\":\"嗜碱性粒细胞绝对值(BASO)\",\"normalH\":\"0.06\",\"normalL\":\"0\",\"result\":\"0.01\",\"unit\":\"10^9/L\"},{\"category\":\"血液分析\",\"itemName\":\"中性粒细胞百分数(NEU%)\",\"normalH\":\"75\",\"normalL\":\"40\",\"result\":\"52.0\",\"unit\":\"%\"},{\"category\":\"血液分析\",\"itemName\":\"淋巴细胞百分数(LYM%)\",\"normalH\":\"50\",\"normalL\":\"20\",\"result\":\"41.5\",\"unit\":\"%\"},{\"category\":\"血液分析\",\"itemName\":\"单核细胞百分数(MON%)\",\"normalH\":\"10\",\"normalL\":\"3\",\"result\":\"5.7\",\"unit\":\"%\"},{\"category\":\"血液分析\",\"itemName\":\"嗜酸性粒细胞百分数(EOS%)\",\"normalH\":\"8\",\"normalL\":\"0.4\",\"result\":\"0.6\",\"unit\":\"%\"},{\"category\":\"血液分析\",\"itemName\":\"嗜碱性粒细胞百分数(BASO%)\",\"normalH\":\"1\",\"normalL\":\"0\",\"result\":\"0.2\",\"unit\":\"%\"},{\"category\":\"血液分析\",\"itemName\":\"红细胞计数(RBC)\",\"normalH\":\"5.1\",\"normalL\":\"3.8\",\"result\":\"4.26\",\"unit\":\"10^12/L\"},{\"category\":\"血液分析\",\"itemName\":\"血红蛋白测定(Hb)\",\"normalH\":\"150\",\"normalL\":\"115\",\"result\":\"133\",\"unit\":\"g/L\"},{\"category\":\"血液分析\",\"itemName\":\"红细胞比容测定(HCT)\",\"normalH\":\"45\",\"normalL\":\"35\",\"result\":\"40.50\",\"unit\":\"%\"},{\"category\":\"血液分析\",\"itemName\":\"红细胞平均体积(MCV)\",\"normalH\":\"100\",\"normalL\":\"82\",\"result\":\"95.20\",\"unit\":\"fL\"},{\"category\":\"血液分析\",\"itemName\":\"平均血红蛋白含量(MCH)\",\"normalH\":\"34\",\"normalL\":\"27\",\"result\":\"31.30\",\"unit\":\"pg\"},{\"category\":\"血液分析\",\"itemName\":\"平均血红蛋白浓度(MCHC)\",\"normalH\":\"354\",\"normalL\":\"316\",\"result\":\"329\",\"unit\":\"g/L\"},{\"category\":\"血液分析\",\"itemName\":\"红细胞分布宽度-标准差(RDW-SD)\",\"normalH\":\"56\",\"normalL\":\"35\",\"result\":\"44.20\",\"unit\":\"fL\"},{\"category\":\"血液分析\",\"itemName\":\"红细胞分布宽度-变异系数(RDW-CV)\",\"normalH\":\"16\",\"normalL\":\"11\",\"result\":\"12.50\",\"unit\":\"%\"},{\"category\":\"血液分析\",\"itemName\":\"血小板计数(PLT)\",\"normalH\":\"350\",\"normalL\":\"125\",\"result\":\"218\",\"unit\":\"10^9/L\"},{\"category\":\"血液分析\",\"itemName\":\"平均血小板体积(MPV)\",\"normalH\":\"12\",\"normalL\":\"6.5\",\"result\":\"8.7\",\"unit\":\"fL\"},{\"category\":\"血液分析\",\"itemName\":\"血小板压积(PCT)\",\"normalH\":\"0.282\",\"normalL\":\"0.108\",\"result\":\"0.19\",\"unit\":\"%\"},{\"category\":\"血液分析\",\"itemName\":\"血小板分布宽度(PDW)\",\"normalH\":\"17\",\"normalL\":\"9\",\"result\":\"15.7\",\"unit\":\"fL\"},{\"category\":\"其他\",\"itemName\":\"眼底照相及Amsler\",\"result\":\"右眼眼底检查可见异常\\r\\n右眼视杯扩大\\r\\n左眼眼底检查未见明显异常\",\"unit\":\"\"},{\"category\":\"其他\",\"itemName\":\"小结\",\"result\":\"右眼眼底检查可见异常\\r\\n右眼视杯扩大\\r\\n左眼眼底检查未见明显异常\",\"unit\":\"\"},{\"category\":\"其他\",\"itemName\":\"描述\",\"result\":\"两侧胸廓对称。肺窗示右肺中叶外段（Se4-Img69）见实性结节，大小约为3mm×2mm，边界清，右肺中下叶及左肺可见较多纤维条索影，边界清，余两肺野纹理清晰，未见明显异常密度影。两侧肺门不大。纵隔窗示心影及大血管形态正常，纵隔内未见肿块及明显肿大淋巴结。无胸腔积液，双上胸膜增厚\\r\\n主动脉管壁可见钙化\",\"unit\":\"\"},{\"category\":\"其他\",\"itemName\":\"小结\",\"result\":\"肺结节影\\r\\n肺纤维灶\\r\\n胸膜增厚\\r\\n主动脉部分管壁钙化\",\"unit\":\"\"},{\"category\":\"其他\",\"itemName\":\"小结\",\"result\":\"请通过优健康APP查看AI-MDT会诊报告。\",\"unit\":\"\"},{\"category\":\"其他\",\"itemName\":\"尿微量白蛋白测定(mAlb)\",\"normalH\":\"24\",\"normalL\":\"0\",\"result\":\"9.91\",\"unit\":\"mg/L\"},{\"category\":\"其他\",\"itemName\":\"尿肌酐(UCR)\",\"result\":\"4137\",\"unit\":\"umol/L\"},{\"category\":\"其他\",\"itemName\":\"尿微量白蛋白/尿肌酐（UMA/UCR)（计算值）\",\"normalH\":\"3\",\"normalL\":\"0\",\"result\":\"2.40\",\"unit\":\"\"},{\"category\":\"其他\",\"itemName\":\"液基薄层细胞学检测（TCT）\",\"result\":\"未见上皮内病变或恶性细胞（NILM）,一年后复查。\",\"unit\":\"\"},{\"category\":\"其他\",\"itemName\":\"血清游离甲状腺素测定(FT4)\",\"normalH\":\"23.81\",\"normalL\":\"11.2\",\"result\":\"17.18\",\"unit\":\"pmol/L\"},{\"category\":\"其他\",\"itemName\":\"血清促甲状腺激素测定(TSH)\",\"normalH\":\"5.1\",\"normalL\":\"0.35\",\"result\":\"4.76\",\"unit\":\"μIU/mL \"},{\"category\":\"其他\",\"itemName\":\"血清游离三碘甲状原氨酸(FT3)\",\"normalH\":\"6.45\",\"normalL\":\"2.76\",\"result\":\"4.72\",\"unit\":\"pmol/L\"},{\"category\":\"其他\",\"itemName\":\"空腹血糖(GLU)\",\"normalH\":\"6.11\",\"normalL\":\"3.89\",\"result\":\"5.10\",\"unit\":\"mmol/L\"}]";
            CustomerDto dto = new CustomerDto();
            dto.setAgentMobile("18826415976");
            dto.setCheckDate(DateUtil.parse("2022-09-29", "yyyy-MM-dd"));
            dto.setCustCsrq(DateUtil.parse("1965-10-26", "yyyy-MM-dd"));
            dto.setCustName("陈汉凤" + mark + i);
            dto.setCustSex("0");
            dto.setMobile("18826415976");
            dto.setCustSfzh("440528196510260922");
            dto.setShopNo("17");
            dto.setVid("Y522135444889" + mark + i);
            ord.setCustomer(dto);
            ord.setCheckData(JSON.parseArray(checkData, CheckData.class));
            ord.setTestData(JSON.parseArray(testData, CheckData.class));
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
                        if(data.getItem_name().contains("液基")){
                            zj.append("★")
                                    .append(data.getItem_ft())
                                    .append("：")
                                    .append(data.getResults());
                        }else {
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
                customerDto.setMobile("18826415976");
                customerDto.setCustSfzh("36078220081023" + nextInt);
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

    @ApiOperation("门店数据推送")
    @RequestMapping(value = "/meinian/cityData/{words}", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult cityData(@PathVariable("words") String word) {
        String[] stringArr = word.split("[,，]");
        //获取门店
        StringBuilder stringBuilder = new StringBuilder();
        String msg = "";
        for (String s : stringArr) {
            String sql  = "select o.vid,count(*) as number,max(to_number(to_char(m.birth_date,'YYYY'),'9999')) as year,max(m.shop_no) as shop from mn_dwd_customer_info m \n" +
                    "left join mn_dim_shop_info n on m.shop_no = n.in_factory \n" +
                    "left join mn_dwd_check_result_info o on m.vid=o.vid \n" +
                    "where n.city = '"+s+"' and m.birth_date is not null group by o.vid";
            List<CityDataRecord> list = pgTemplate.query(sql, new Object[]{}, new BeanPropertyRowMapper<>(CityDataRecord.class));
            if (list.isEmpty()) {
                LOGGER.error("地市[{}]，未找到门店数据",s);
                continue;
            }
            Map<String, List<CityDataRecord>> collect = list.stream().collect(Collectors.groupingBy(CityDataRecord::getShop));
            //门店数
            Integer count = collect.keySet().size();
            int cityNumber = 10;
            int yu = cityNumber / count;
            int i = 0;
            Iterator<String> iterator = collect.keySet().iterator();
            while (iterator.hasNext()){
                i++;
                String mapKey = iterator.next();
                List<CityDataRecord> cityDataRecords = collect.get(mapKey);
                int yearGold;
                int numberGold;
                for (CityDataRecord yearRecord : cityDataRecords) {
                    if(yearRecord.getYear() < 1960){
                        yearGold = 9;
                    }else if(yearRecord.getYear() >=1960 && yearRecord.getYear()<1970){
                        yearGold = 8;
                    }else if(yearRecord.getYear() >=1970 && yearRecord.getYear()<1980){
                        yearGold = 7;
                    }else if(yearRecord.getYear() >=1980 && yearRecord.getYear()<1990){
                        yearGold = 6;
                    }else if(yearRecord.getYear() >=1990 && yearRecord.getYear()<2000){
                        yearGold = 5;
                    }else {
                        yearGold = 4;
                    }
                    if(yearRecord.getNumber() >200){
                        numberGold =11;
                    }else if(yearRecord.getNumber() >180 && yearRecord.getNumber()<=200){
                        numberGold =10;
                    }else if(yearRecord.getNumber() >160 && yearRecord.getNumber()<=180){
                        numberGold =9;
                    }else if(yearRecord.getNumber() >140 && yearRecord.getNumber()<=160){
                        numberGold =8;
                    }else if(yearRecord.getNumber() >120 && yearRecord.getNumber()<=140){
                        numberGold =7;
                    }else if(yearRecord.getNumber() >100 && yearRecord.getNumber()<=120){
                        numberGold =6;
                    }else if(yearRecord.getNumber() >80 && yearRecord.getNumber()<=100){
                        numberGold =5;
                    }else if(yearRecord.getNumber() >60 && yearRecord.getNumber()<=80){
                        numberGold =4;
                    }else if(yearRecord.getNumber() >40 && yearRecord.getNumber()<=60){
                        numberGold =3;
                    }else if(yearRecord.getNumber() >20 && yearRecord.getNumber()<=40){
                        numberGold =2;
                    }else {
                        numberGold =1;
                    }
                    yearRecord.setGold(yearGold * numberGold);
                }
                List<CityDataRecord> records = cityDataRecords.stream().sorted(Comparator.comparing(CityDataRecord::getGold).reversed()).collect(Collectors.toList());
                if(count == i){
                    yu = 10 -((count -1) * yu);
                    stringBuilder.append(records.stream().limit(yu).map(CityDataRecord::getVid).collect(Collectors.joining(",")));
                    stringBuilder.append(",");
                    int cityNumberO = stringBuilder.toString().split(",").length;
                    if( cityNumberO<cityNumber){
                        int cha = cityNumber-cityNumberO;
                        msg = msg +"【"+s+"】地市符合规则数据缺少"+cha+"条。";
                    }
                }else{
                    stringBuilder.append(records.stream().limit(yu).map(CityDataRecord::getVid).collect(Collectors.joining(",")));
                    stringBuilder.append(",");
                }
            }
        }
        String finalStr = stringBuilder.toString();
        if(StrUtil.isNotBlank(msg)){
            finalStr = msg +"。"+finalStr;
        }
        return AjaxResult.success(finalStr);
    }

    @ApiOperation("医院数据推送")
    @RequestMapping(value = "/meinian/yyData", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult yyData() {
        String tableList = SdkConstant.TABLE_STR+"_user_report_list";
        String tableInfo = SdkConstant.TABLE_STR+"_user_report_info";
        //获取用户数据
        executor.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
        AtomicInteger tjCount = new AtomicInteger();
        int allCount = 0;
        int i = 0;
        while (true){
            try {
            int offset = i * COUNT;
            String sql ="select member_id as vid,name,sex,sfzh,tel,birthday birth_date," +
                    "check_time from "+tableList+"" +
                    " LIMIT "+COUNT+" OFFSET "+offset+"";
            List<CustomerInfo> list = pgTemplate.query(sql, new Object[]{}, new int[]{}, new BeanPropertyRowMapper<>(CustomerInfo.class));

            int mo = list.size() % 1000;
            int mo1 = 0;
            if(mo == 0){
              mo1=  list.size() / 1000;
            }else{
                mo1=  list.size() / 1000+1;
            }
                List<Thread> listT=new ArrayList<>();
                CountDownLatch cd = new CountDownLatch(mo1);
            for(int j=0; j<mo1; j++){
                int m = 0;
                if(list.size()-j * 1000>=1000){
                    m=1000;
                }else{
                    m=list.size()-j * 1000;
                }

                List<CustomerInfo> customerInfos = list.subList(j * 1000, (j * 1000)+m);
                String s1 = customerInfos.stream().map(info -> "'" + info.getVid() + "'").collect(Collectors.joining(","));
                String sql2 = "select member_id as vid,class_name as item_ft ,item_name,result_value results,unit,reference normal_l,image_describe,image_diagnose,data_type from " + tableInfo + " where member_id in("+s1+")";
                List<TestData> testDataLists = pgTemplate.query(sql2, new Object[]{}, new int[]{}, new BeanPropertyRowMapper<>(TestData.class));
                Map<String, List<TestData>> collect = testDataLists.stream().collect(Collectors.groupingBy(TestData::getVid));

          listT.add(new Thread(()->{
                    for (CustomerInfo customer : customerInfos) {
                        MarketData ord = new MarketData();
                        int nextInt = new Random().nextInt(9999);
                        CustomerDto customerDto = new CustomerDto();
                        int anInt = new Random().nextInt(99999999);
                        customerDto.setAgentMobile(customer.getTel());
                        customerDto.setMobile(customer.getTel());
                        customerDto.setCustSfzh(customer.getSfzh());
                        if(StrUtil.isBlank(customer.getTel())){
                            customerDto.setAgentMobile("176" + anInt);
                            customerDto.setMobile("18826415976");
                        }
                        if(StrUtil.isBlank(customer.getSfzh())){
                            customerDto.setCustSfzh("360782199401236619");
                        }
                        if("女".equals(customer.getSex())){
                            customerDto.setCustSex("0");
                        }else {
                            customerDto.setCustSex("1");
                        }
                        if(customer.getBirth_date() == null){
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
                        if(testDataList == null){
                            continue;
                        }
                        Boolean flag = false;
                        for (TestData data : testDataList) {
                            if("2".equals(data.getData_type())){
                                if(StrUtil.isNotBlank(data.getImage_describe())){
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
                                }else{
                                    CheckData tData = new CheckData();
                                    tData.setCategory(data.getItem_ft());
                                    tData.setItemName(data.getItem_name());
                                    tData.setNormalH(data.getNormal_h());
                                    tData.setNormalL(data.getNormal_l());
                                    tData.setResult(data.getResults());
                                    tData.setUnit(data.getUnit());
                                    tCheckList.add(tData);
                                }
                            }else if("3".equals(data.getData_type())){
                                CheckData tData = new CheckData();
                                tData.setCategory(data.getItem_ft());
                                tData.setItemName(data.getItem_name());
                                //tData.setNormalH(data.getNormal_h());
                                tData.setNormalL(data.getNormal_l());
                                tData.setResult(data.getResults());
                                tData.setUnit(data.getUnit());
                                tTestList.add(tData);
                            }
                            if(StrUtil.isNotBlank(data.getItem_name()) && (data.getItem_name().contains("身高") || data.getItem_name().contains("体重"))){
                                flag = true;
                            }
                        }
                        //身高体重默认赋值
                        if(!flag){
                            CheckData tData = new CheckData();
                            CheckData tData2 = new CheckData();
                            if("0".equals(customerDto.getCustSex())){
                                tData.setCategory("一般检查");
                                tData.setItemName("身高");
                                tData.setResult("160");
                                tData.setUnit("Cm");
                                tData2.setCategory("一般检查");
                                tData2.setItemName("体重");
                                tData2.setResult("55");
                                tData2.setUnit("kg");
                            }else{
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
                            }else {
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
                while(var13.hasNext()){
                    Thread thread = (Thread)var13.next();
                    thread.start();
                };
            cd.await();
            if(list.size() < COUNT){
                allCount = i * COUNT + list.size();
                break;
            }

            }catch (Exception e){
                LOGGER.error(e.getMessage());
            }finally {
                i++;
            }
        }
        String str = "推送数据总数:" +allCount +"。推送成功数据数:"+tjCount;
        return AjaxResult.success(str);
    }
}
