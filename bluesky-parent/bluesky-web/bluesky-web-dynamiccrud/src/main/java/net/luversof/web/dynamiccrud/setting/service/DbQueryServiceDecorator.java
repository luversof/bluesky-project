package net.luversof.web.dynamiccrud.setting.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import org.springframework.stereotype.Service;

@Service
public class DbQueryServiceDecorator
        implements SettingServiceListSupplier<DbQuery>, SettingServiceDecorator {

    private Map<String, SettingServiceListSupplier<DbQuery>> dbQueryServiceMap;

    public DbQueryServiceDecorator(
            Map<String, SettingServiceListSupplier<DbQuery>> dbQueryServiceMap) {
        this.dbQueryServiceMap = getSortedSettingServiceMap(dbQueryServiceMap);
    }

    @Override
    public List<DbQuery> findList(SettingParameter settingParameter) {
        for (var entry : dbQueryServiceMap.entrySet()) {
            var target = entry.getValue().findList(settingParameter);
            if (!target.isEmpty()) {
                return target;
            }
        }
        return Collections.emptyList();
    }
}
