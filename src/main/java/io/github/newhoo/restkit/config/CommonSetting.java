package io.github.newhoo.restkit.config;

import com.intellij.lang.IdeLanguageCustomization;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

import static io.github.newhoo.restkit.common.RestConstant.WEB_FRAMEWORK_LOCAL;
import static io.github.newhoo.restkit.common.RestConstant.WEB_FRAMEWORK_SPRING_MVC;

/**
 * common setting
 *
 * @author huzunrong
 * @since 2.0.0
 */
@Data
public class CommonSetting {

    private Set<String> enabledWebFrameworks = getDefaultEnabledFrameworks();

    private boolean scanServiceWithLib = false;

    private int requestTimeout = 0;

    private boolean saveRequestLog = false;
    private boolean showModuleInSearchEvery = false;
    private boolean displayApiGroupUsingFileName = false;
    private boolean displayTreeListUsingApiDesc = false;
    private boolean enableParameterLibrary = true;
    private String preRequestScriptPath = "";
    private String postRequestScriptPath = "";
    private String apiFilePath = "";
    private String redisIp = "";
    private String redisPort = "";
    private String redisProject = "";

    public boolean isModified(CommonSetting modifiedSetting) {
        return requestTimeout != modifiedSetting.getRequestTimeout()
                || saveRequestLog != modifiedSetting.isSaveRequestLog()
                || showModuleInSearchEvery != modifiedSetting.isShowModuleInSearchEvery()
                || displayApiGroupUsingFileName != modifiedSetting.isDisplayApiGroupUsingFileName()
                || displayTreeListUsingApiDesc != modifiedSetting.isDisplayTreeListUsingApiDesc()
                || enableParameterLibrary != modifiedSetting.isEnableParameterLibrary()
                || !StringUtils.equals(preRequestScriptPath, modifiedSetting.getPreRequestScriptPath())
                || !StringUtils.equals(postRequestScriptPath, modifiedSetting.getPostRequestScriptPath())
                || !StringUtils.equals(apiFilePath, modifiedSetting.getApiFilePath())
                || !StringUtils.equals(redisIp, modifiedSetting.getRedisIp())
                || !StringUtils.equals(redisPort, modifiedSetting.getRedisPort())
                || !StringUtils.equals(redisProject, modifiedSetting.getRedisProject())
                || !CollectionUtils.isEqualCollection(enabledWebFrameworks, modifiedSetting.getEnabledWebFrameworks());
    }

    private static Set<String> getDefaultEnabledFrameworks() {
        Set<String> set = new HashSet<>();
        set.add(WEB_FRAMEWORK_LOCAL);
        IdeLanguageCustomization.getInstance().getPrimaryIdeLanguages()
                                .stream()
                                .filter(l -> "JAVA".equals(l.getID()))
                                .findAny()
                                .ifPresent(l -> set.add(WEB_FRAMEWORK_SPRING_MVC));
        return set;
    }
}
