package io.github.privacyops.analyzer.access;

import io.github.privacyops.access.AccessControlProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class AccessControlProviderLoader {

    public List<AccessControlProvider> load() {

        List<AccessControlProvider> providers =
                new ArrayList<>();

        /*
         * PrivacyOps 기본 Provider
         */
        providers.add(
                new SpringSecurityAccessControlProvider()
        );

        /*
         * 외부 SPI Provider 탐색
         *
         * classpath에 존재하는 JAR 중
         * META-INF/services/
         * io.github.privacyops.access.AccessControlProvider
         * 파일을 가진 Provider를 자동 탐색한다.
         */
        ServiceLoader<AccessControlProvider> loader =
                ServiceLoader.load(
                        AccessControlProvider.class
                );

        for (AccessControlProvider provider :
                loader) {

            /*
             * 동일 ID Provider 중복 방지
             */
            boolean alreadyRegistered =
                    providers.stream()
                            .anyMatch(
                                    existing ->
                                            existing.id()
                                                    .equals(
                                                            provider.id()
                                                    )
                            );

            if (!alreadyRegistered) {

                providers.add(
                        provider
                );
            }
        }

        return List.copyOf(
                providers
        );
    }
}