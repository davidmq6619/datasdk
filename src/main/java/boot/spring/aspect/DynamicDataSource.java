package boot.spring.aspect;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;


public class DynamicDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> dataSources = new InheritableThreadLocal<>();

    public static void setDataSource(String dataSource) {
        dataSources.set(dataSource);
    }

    public static void clearDataSource() {
        dataSources.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return dataSources.get();
    }

}
