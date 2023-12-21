package boot.spring.po;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * @author Qiang
 * @Description
 * @Time 2023-12-15 10:33
 */

@Data
@ToString
public class DemoData {

    @ExcelProperty(index = 0)
    private String name;

    @ExcelProperty(index = 1)
    private String standardName;
}
