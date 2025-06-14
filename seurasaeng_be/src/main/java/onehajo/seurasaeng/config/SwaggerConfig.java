package onehajo.seurasaeng.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;


@OpenAPIDefinition(
        info = @Info(
                title = "My API",
                description = "This is the API documentation for ",
                version = "v1.0.0"
        )
)
@Configuration
public class SwaggerConfig {
}