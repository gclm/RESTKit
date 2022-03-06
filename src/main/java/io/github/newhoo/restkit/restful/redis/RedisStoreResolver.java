package io.github.newhoo.restkit.restful.redis;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import io.github.newhoo.restkit.common.RestItem;
import io.github.newhoo.restkit.config.CommonSetting;
import io.github.newhoo.restkit.config.CommonSettingComponent;
import io.github.newhoo.restkit.config.SettingConfigurable;
import io.github.newhoo.restkit.restful.RequestResolver;
import io.github.newhoo.restkit.restful.ep.RestfulResolverProvider;
import io.github.newhoo.restkit.util.HtmlUtil;
import io.github.newhoo.restkit.util.JsonUtils;
import io.github.newhoo.restkit.util.NotifierUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.github.newhoo.restkit.common.RestConstant.WEB_FRAMEWORK_REDIS;

public class RedisStoreResolver implements RequestResolver {

    private final Project project;
    private final CommonSetting setting;

    public RedisStoreResolver(Project project) {
        this.project = project;
        this.setting = CommonSettingComponent.getInstance(project).getState();
    }

    @Override
    public String getFrameworkName() {
        return WEB_FRAMEWORK_REDIS;
    }

    @Override
    public ScanType getScanType() {
        return ScanType.STORAGE;
    }

    @Override
    public boolean checkConfig() {
        if (StringUtils.isAnyEmpty(setting.getRedisIp(), setting.getRedisPort(), setting.getRedisProject())) {
            NotifierUtils.infoBalloon("", "Redis server config is empty. " + HtmlUtil.link("Edit", "Edit"), new NotificationListener.Adapter() {
                @Override
                protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingConfigurable.class);
                }
            }, project);
            return false;
        }
        return true;
    }

    @Override
    public List<RestItem> findRestItemInProject(@NotNull Project project) {
        try {
            nl.melp.redis.Redis redis = new nl.melp.redis.Redis(new Socket(setting.getRedisIp(), Integer.parseInt(setting.getRedisPort())));
            List<Object> result = redis.call("LRANGE", "RESTKit:" + setting.getRedisProject(), "0", "100000");
            if (CollectionUtils.isNotEmpty(result)) {
                return result.stream()
                             .map(e -> JsonUtils.fromJson(new String((byte[]) e), RestItem.class))
                             .collect(Collectors.toList());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public void add(List<RestItem> itemList) {
        AppExecutorUtil.getAppExecutorService().submit(() -> {
            try {
                nl.melp.redis.Redis redis = new nl.melp.redis.Redis(new Socket(setting.getRedisIp(), Integer.parseInt(setting.getRedisPort())));
                for (RestItem restItem : itemList) {
                    restItem.setId(UUID.randomUUID().toString());
                    redis.call("LPUSH", "RESTKit:" + setting.getRedisProject(), JsonUtils.toJson(restItem));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void update(List<RestItem> itemList) {
        AppExecutorUtil.getAppExecutorService().submit(() -> {
            List<RestItem> list = getRemainList(itemList);
            list.addAll(itemList);
            replaceAll(list);
        });
    }

    @Override
    public void delete(List<RestItem> itemList) {
//        Set<Long> idSet = itemList.stream().map(RestItem::getId).collect(Collectors.toSet());
        AppExecutorUtil.getAppExecutorService().submit(() -> {
            List<RestItem> list = getRemainList(itemList);
            replaceAll(list);
        });
    }

    private List<RestItem> getRemainList(List<RestItem> itemList) {
        List<RestItem> list = new ArrayList<>(findRestItemInProject(project));
        Set<String> keySet = itemList.stream().map(item -> item.getUrl() + item.getMethod()).collect(Collectors.toSet());
        list.removeIf(item -> keySet.contains(item.getUrl() + item.getMethod()));
        return list;
    }

    public void replaceAll(List<RestItem> itemList) {
        try {
            nl.melp.redis.Redis redis = new nl.melp.redis.Redis(new Socket(setting.getRedisIp(), Integer.parseInt(setting.getRedisPort())));
            redis.call("DEL", "RESTKit:api");
            for (RestItem restItem : itemList) {
                redis.call("LPUSH", "RESTKit:" + setting.getRedisProject(), JsonUtils.toJson(restItem));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class RedisStoreResolverProvider implements RestfulResolverProvider {

        @Override
        public RequestResolver createRequestResolver(@NotNull Project project) {
            return new RedisStoreResolver(project);
        }
    }
}
