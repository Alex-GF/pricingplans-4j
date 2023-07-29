package io.github.isagroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.isagroup.PricingPlanAwareTests.TestConfiguration.PricingContextImpl;
import io.github.isagroup.annotations.PricingPlanAware;
import io.github.isagroup.annotations.PricingPlanAwareAspect;
import io.github.isagroup.exceptions.PricingPlanEvaluationException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Import({ io.github.isagroup.annotations.PricingPlanAwareAspect.class })
public class PricingPlanAwareTests {

    private static final String JWT_SECRET_TEST = "secret";
    private static final Integer JWT_EXPIRATION_TEST = 86400;
    private static final String JWT_SUBJECT_TEST = "admin1";

    @Configuration
    public static class TestConfiguration {

        @Component
        public class PricingContextImpl extends PricingContext {

            private String configFilePath = "pricing/models.yml";
            private String jwtSecret = JWT_SECRET_TEST;
            private int jwtExpiration = JWT_EXPIRATION_TEST;

            @Override
            public String getConfigFilePath() {
                return this.configFilePath;
            };

            @Override
            public String getJwtSecret() {
                return this.jwtSecret;
            };

            @Override
            public int getJwtExpiration() {
                return this.jwtExpiration;
            };

            public void setConfigFilePath(String configFilePath) {
                this.configFilePath = configFilePath;
            }

            public void setJwtSecret(String jwtSecret) {
                this.jwtSecret = jwtSecret;
            }

            public void setJwtExpiration(int jwtExpiration) {
                this.jwtExpiration = jwtExpiration;
            }

            @Override
            public Map<String, Object> getUserContext() {
                Map<String, Object> userContext = new HashMap<>();

                userContext.put("username", JWT_SUBJECT_TEST);
                userContext.put("pets", 2);
                userContext.put("haveVetSelection", true);
                userContext.put("haveCalendar", true);
                userContext.put("havePetsDashboard", true);
                userContext.put("haveOnlineConsultations", true);

                return userContext;
            }

            @Override
            public String getUserPlan() {
                return "ADVANCED";
            }

            @Override
            public Object getUserAuthorities() {
                Map<String, String> userAuthorities = new HashMap<>();
                userAuthorities.put("role", "admin");
                userAuthorities.put("username", "admin1");
                userAuthorities.put("password", "4dm1n");

                return userAuthorities;
            }

        }

    }

    @Autowired
    private PricingPlanAwareAspect pricingPlanAwareAspect;

    @Autowired
    private PricingContextImpl pricingContextImpl;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Test
    void simpleAnnotationUseCaseTest() throws Throwable {

        Mockito.when(joinPoint.proceed()).thenReturn("Result");

        // Obtener el valor del parámetro featureId que deseas probar
        String featureId = "maxPets";

        pricingPlanAwareAspect.validatePricingPlan(joinPoint, new PricingPlanAware() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return PricingPlanAware.class;
            }

            @Override
            public String featureId() {
                return featureId;
            }
        });
    }

    @Test
    void nonExistentFeatureEvaluationTest() throws Throwable {

        Mockito.when(joinPoint.proceed()).thenReturn("Result");

        // Obtener el valor del parámetro featureId que deseas probar
        String featureId = "nonExistentFeature";

        PricingPlanEvaluationException exception = assertThrows(PricingPlanEvaluationException.class, () -> {
            pricingPlanAwareAspect.validatePricingPlan(joinPoint, new PricingPlanAware() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return PricingPlanAware.class;
                }

                @Override
                public String featureId() {
                    return featureId;
                }
            });
        });

        assertEquals("The feature " + featureId + " does not exist in the current pricing configuration", exception.getMessage());
    }

    @Test
    void nonExistentConfigurationFileTest() throws Throwable {

        pricingContextImpl.setConfigFilePath("nonExistentFile.yml");

        Mockito.when(joinPoint.proceed()).thenReturn("Result");

        // Obtener el valor del parámetro featureId que deseas probar
        String featureId = "nonExistentFeature";

        PricingPlanEvaluationException exception = assertThrows(PricingPlanEvaluationException.class, () -> {
            pricingPlanAwareAspect.validatePricingPlan(joinPoint, new PricingPlanAware() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return PricingPlanAware.class;
                }

                @Override
                public String featureId() {
                    return featureId;
                }
            });
        });

        assertEquals("Error while parsing YAML file", exception.getMessage());
    }

}
