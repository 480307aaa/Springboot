1)build.gradle添加核心依赖:
比如：
dependencies {
    compile project(':spring-boot-starter-upesn-core')
    compile 'org.springframework.boot:spring-boot-starter-web'
}
---------------------------------------------------------------------------------------------------------------------------------------------------------------

2）spring.factories文件中添加配置，指定starter配置类的位置和名称
比如：org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.yonyoucloud.ec.sns.starter.todo.center.TodoCenterAutoConfiguration
其中TodoCenterAutoConfiguration为配置类，其内容大致如下：
package com.yonyoucloud.ec.sns.starter.todo.center;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonyoucloud.ec.sns.starter.todo.center.module.TodoCenterI18nUpesnService;
import com.yonyoucloud.ec.sns.starter.todo.center.module.TodoCenterI18nUserCenterService;
import com.yonyoucloud.ec.sns.starter.todo.center.module.TodoCenterUpesnService;
import com.yonyoucloud.ec.sns.starter.todo.center.module.TodoCenterUserCenterService;
import com.yonyoucloud.ec.sns.starter.todo.center.module.impl.TodoCenterI18nUpesnServiceImpl;
import com.yonyoucloud.ec.sns.starter.todo.center.module.impl.TodoCenterI18nUserCenterServiceImpl;
import com.yonyoucloud.ec.sns.starter.todo.center.module.impl.TodoCenterUpesnServiceImpl;
import com.yonyoucloud.ec.sns.starter.todo.center.module.impl.TodoCenterUserCenterServiceImpl;
import com.yonyoucloud.ec.sns.starter.todo.center.support.TodoCenterRestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yegk7
 */
@Configuration
@EnableConfigurationProperties(TodoCenterProperties.class)
@Slf4j
@RequiredArgsConstructor
public class TodoCenterAutoConfiguration {

    private final TodoCenterProperties properties;

    @ConditionalOnMissingBean
    @Bean
    public TodoCenterRestTemplate todoCenterRestTemplate(ObjectMapper objectMapper) {
        return new TodoCenterRestTemplate(properties, objectMapper);
    }

    @ConditionalOnMissingBean
    @Bean
    public TodoCenterI18nUpesnService todoCenterI18nUpesnService(TodoCenterRestTemplate todoCenterRestTemplate) {
        return new TodoCenterI18nUpesnServiceImpl(properties, todoCenterRestTemplate);
    }

    @ConditionalOnMissingBean
    @Bean
    public TodoCenterI18nUserCenterService todoCenterI18nUserCenterService(TodoCenterRestTemplate todoCenterRestTemplate) {
        return new TodoCenterI18nUserCenterServiceImpl(properties, todoCenterRestTemplate);
    }

    @ConditionalOnMissingBean
    @Bean
    public TodoCenterUpesnService todoCenterUpesnService(TodoCenterRestTemplate todoCenterRestTemplate) {
        return new TodoCenterUpesnServiceImpl(properties, todoCenterRestTemplate);
    }

    @ConditionalOnMissingBean
    @Bean
    public TodoCenterUserCenterService todoCenterUserCenterService(TodoCenterRestTemplate todoCenterRestTemplate) {
        return new TodoCenterUserCenterServiceImpl(properties, todoCenterRestTemplate);
    }

}
-------------------------------------------------------------------------------------------------------------------------------------------------------------
3）上个文件中@EnableConfigurationProperties注解指定的键值对配置文件：TodoCenterProperties
内容大致如下：
package com.yonyoucloud.ec.sns.starter.todo.center;

import com.yonyoucloud.ec.sns.starter.core.module.AbstractApplicationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @author yegk7
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@ConfigurationProperties(prefix = "yonyoucloud.upesn.todo-center.sdk")
@Validated
public class TodoCenterProperties extends AbstractApplicationProperties {

    @Valid
    @NotNull
    private String host;

    private Integer connectTimeoutMs = 3 * 1000;

    private Integer readTimeoutMs = 120 * 1000;

}
---------------------------------------------------------------------------------------------------------------------------------------------------------------
4）上个文件中指定的@ConfigurationProperties(prefix = "yonyoucloud.upesn.todo-center.sdk"),需要在application.yml文件中指出
    todo-center:
      sdk:
        host: http://172.20.1.177:6058/todocenter
-----------------------------------------------------------------------------------------------------------------------------------------------------------------
5）AbstractApplicationProperties文件内容
package com.yonyoucloud.ec.sns.starter.core.module;

import com.yonyoucloud.ec.sns.error.ECConfigurationException;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author liuhaoi
 */
public class AbstractApplicationProperties {

    public static final char URL_SPLIT_CHAT = '/';

    public Set<String> parseCommaSplitParam(String source) {

        if (StringUtils.isBlank(source)) {
            return Collections.emptySet();
        }

        String[] split = source.split(",");

        return Stream.of(split).map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }


    public String concatURL(String urlPrefix, String... urlFragment) {

        if (urlPrefix == null) {
            throw new ECConfigurationException("invalid url prefix, can not be null");
        }


        StringBuilder builder = new StringBuilder(urlPrefix);

        for (String fragment : urlFragment) {
            concatURLFragment(builder, fragment);
        }

        return builder.toString();
    }

    private void concatURLFragment(StringBuilder builder, String fragment) {

        if (StringUtils.isBlank(fragment)) {
            return;
        }

        if (fragment.length() == 1 && fragment.charAt(0) == URL_SPLIT_CHAT) {
            return;
        }

        String trimFragment = fragment.trim();

        //参数之后
        if (builder.indexOf("?") != -1) {
            builder.append(trimFragment);
            return;
        }

        if (trimFragment.startsWith("?")) {
            builder.append(trimFragment);
            return;
        }

        concatPaths(builder, trimFragment);
    }

    private void concatPaths(StringBuilder builder, String trimFragment) {
        char c1 = builder.charAt(builder.length() - 1);

        char c2 = trimFragment.charAt(0);

        if (c1 == URL_SPLIT_CHAT && c2 == URL_SPLIT_CHAT) {
            builder.append(trimFragment.substring(1));
            return;
        }

        if (c1 == URL_SPLIT_CHAT || c2 == URL_SPLIT_CHAT) {
            builder.append(trimFragment);
            return;
        }

        builder.append(URL_SPLIT_CHAT).append(trimFragment);
    }


}
---------------------------------------------------------------------------------------------------------------------------------------------------------------
6）RestTemplate文件(创建连接等信息)
package com.yonyoucloud.ec.sns.starter.todo.center.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonyoucloud.ec.sns.starter.todo.center.TodoCenterProperties;
import lombok.Getter;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * TodoCenter使用的REST Template, 不和其他应用混淆, 不被其他配置影响, 如SpringMVC
 * 使用Jackson作为序列化和反序列化
 *
 * @author yegk7
 */
@Getter
public class TodoCenterRestTemplate {

    private RestTemplate restTemplate;

    public TodoCenterRestTemplate(TodoCenterProperties todoCenterProperties, ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(todoCenterProperties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(todoCenterProperties.getReadTimeoutMs()))
                .messageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .customizers((RestTemplateCustomizer) restTemplate -> {
                    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

                    //创建连接管理器
                    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
                    connectionManager.setMaxTotal(2000);
                    connectionManager.setDefaultMaxPerRoute(3);

                    httpClientBuilder.setConnectionManager(connectionManager);

                    HttpClient httpClient = httpClientBuilder.build();

                    //创建HttpComponentsClientHttpRequestFactory
                    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                            httpClient);
                    requestFactory.setConnectTimeout(1000);
                    requestFactory.setReadTimeout(60 * 1000);
                    requestFactory.setConnectionRequestTimeout(500);

                    restTemplate.setRequestFactory(requestFactory);
                }).build();

    }

}
-----------------------------------------------------------------------------------------------------------------------------------------------
7）实现步骤二中的多个服务：

比如：
package com.yonyoucloud.ec.sns.starter.todo.center.module.impl;

import com.yonyoucloud.ec.sns.starter.todo.center.TodoCenterProperties;
import com.yonyoucloud.ec.sns.starter.todo.center.domain.pojo.FinishToDoItemVO;
import com.yonyoucloud.ec.sns.starter.todo.center.domain.pojo.ToDoItemVO;
import com.yonyoucloud.ec.sns.starter.todo.center.domain.response.RespData;
import com.yonyoucloud.ec.sns.starter.todo.center.domain.response.ResponseData;
import com.yonyoucloud.ec.sns.starter.todo.center.module.TodoCenterUpesnService;
import com.yonyoucloud.ec.sns.starter.todo.center.support.TodoCenterRestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

@RequiredArgsConstructor
public class TodoCenterUpesnServiceImpl implements TodoCenterUpesnService {

    private static final String URL_SEND_UPESN_TODO = "rest/thirdpart/todo/item?resendable=%s&omitNotify=%s";

    private static final String URL_FINISH_UPESN_TODO = "rest/thirdpart/todo/item/done";

    private static final String URL_DELETE_UPESN_TODO = "rest/thirdpart/todo/item/revocation";

    private final TodoCenterProperties properties;

    private final TodoCenterRestTemplate restTemplate;

    @Override
    public RespData<Boolean> postToDoItem(ToDoItemVO request, boolean resendable, boolean omitNotify) {

        String url = properties.concatURL(properties.getHost(), String.format(URL_SEND_UPESN_TODO, resendable, omitNotify));

        ResponseEntity<ResponseData> responseEntity = restTemplate.getRestTemplate().postForEntity(url, request, ResponseData.class);

        return Objects.requireNonNull(responseEntity.getBody()).getRespData();
    }

    @Override
    public RespData<Boolean> finishToDoItem(FinishToDoItemVO finishToDoItemVo) {

        String url = properties.concatURL(properties.getHost(), URL_FINISH_UPESN_TODO);

        ResponseEntity<ResponseData> responseEntity = restTemplate.getRestTemplate().postForEntity(url, finishToDoItemVo, ResponseData.class);

        return Objects.requireNonNull(responseEntity.getBody()).getRespData();
    }

    @Override
    public RespData<Boolean> deleteToDoItem(FinishToDoItemVO finishToDoItemVo) {

        String url = properties.concatURL(properties.getHost(), URL_DELETE_UPESN_TODO);

        ResponseEntity<ResponseData> responseEntity = restTemplate.getRestTemplate().postForEntity(url, finishToDoItemVo, ResponseData.class);

        return Objects.requireNonNull(responseEntity.getBody()).getRespData();
    }
}
-----------------------------------------------------------------------------------------------------------------------------------------------------------------
小知识点：构建多个项目(application.yml文件中指出)
configure(allprojects) { project ->
    group 'com.yonyoucloud.ec'
}


