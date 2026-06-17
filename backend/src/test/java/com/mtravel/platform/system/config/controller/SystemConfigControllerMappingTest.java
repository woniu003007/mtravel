package com.mtravel.platform.system.config.controller;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.assertj.core.api.Assertions.assertThat;

class SystemConfigControllerMappingTest {

    @Test
    void systemConfigShouldOnlyExposeGetAndPostMappings() {
        Set<RequestMethod> methods = Arrays.stream(SystemConfigController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)
                        || method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(DeleteMapping.class))
                .flatMap(SystemConfigControllerMappingTest::mappingMethods)
                .collect(Collectors.toSet());

        assertThat(methods).contains(RequestMethod.GET, RequestMethod.POST);
        assertThat(methods).doesNotContain(RequestMethod.PUT, RequestMethod.DELETE);
    }

    private static java.util.stream.Stream<RequestMethod> mappingMethods(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return java.util.stream.Stream.of(RequestMethod.GET);
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            return java.util.stream.Stream.of(RequestMethod.POST);
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            return java.util.stream.Stream.of(RequestMethod.PUT);
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return java.util.stream.Stream.of(RequestMethod.DELETE);
        }
        return java.util.stream.Stream.empty();
    }
}
