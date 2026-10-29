package com.fiap.workshop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.support.RecordingIdentityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "springdoc.swagger-ui.enabled=false")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RecordingIdentityConfig.class})
class SwaggerUiToggleTest {

	@Autowired
	MockMvc mvc;

	@Test
	void swaggerUiCanBeSwitchedOffWhileTheApiDocsStayAvailable() throws Exception {
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
		mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}
}
