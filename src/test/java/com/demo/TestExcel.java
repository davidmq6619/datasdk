package com.demo;

import boot.spring.po.DemoData;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;

/**
 * @author Qiang
 * @Description
 * @Time 2023-12-14 18:03
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestExcel {

    @Test
    public void testEasyExcel(){
        // 写法1：JDK8+ ,不用额外写一个DemoDataListener
        //Users/mingqiang/Desktop/检验+检查字典.xlsx
        String fileName = "/Users/mingqiang/Desktop"  + File.separator + "检验+检查字典.xlsx";
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 文件流会自动关闭
        // 这里默认每次会读取100条数据 然后返回过来 直接调用使用数据就行
        // 具体需要返回多少行可以在`PageReadListener`的构造函数设置
        File file = new File(fileName);
        if(file.exists()){
            EasyExcel.read(file, DemoData.class, new PageReadListener<DemoData>(dataList -> {
                for (DemoData demoData : dataList) {

                    log.info("读取到一条数据{}", JSON.toJSONString(demoData));
                }
            })).sheet().doRead();
        }else {
            log.error("文件不存在");
        }
    }

    @Test
    public void testApplication(){
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:application.yml");
        System.out.println(context);
    }
}
