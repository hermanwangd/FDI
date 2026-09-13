/*
 * Synthetic MockMvc fixture for SF-BL-002 Task 1 route-aware contracts.
 *
 * This source is a deterministic parse input for the HTTP behavior extractor.
 * It covers mapping composition, query strings, path variables, an unsupported
 * dynamic route expression, an ambiguous route, positive behavior, and a
 * rejection with same-test negative evidence. It is intentionally kept under
 * src/test/resources and is never compiled or executed.
 */
package scenarioforward.sfbl002.routeaware;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteFixtureController.class)
class RouteFixtureTests {

	private static final int TEST_OWNER_ID = 1;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void testListOwnersWithQuery() throws Exception {
		this.mockMvc.perform(get("/owners").param("page", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray());
	}

	@Test
	void testShowOwnerWithPathVariable() throws Exception {
		this.mockMvc.perform(get("/owners/{ownerId}", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TEST_OWNER_ID));
	}

	@Test
	void testCreateOwnerPositive() throws Exception {
		this.mockMvc
			.perform(post("/owners").contentType("application/json")
				.content("{\"id\":0,\"name\":\"Franklin\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Franklin"));
	}

	@Test
	void testCreateOwnerRejectsBlankName() throws Exception {
		this.mockMvc
			.perform(post("/owners").contentType("application/json")
				.content("{\"id\":0,\"name\":\"\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void testShowPetComposedVariables() throws Exception {
		this.mockMvc.perform(get("/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, 2))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Leo"));
	}

	@Test
	void testDynamicRouteExpressionStaysUnresolved() throws Exception {
		int petId = 2;
		this.mockMvc.perform(get("/pets/" + petId)).andExpect(status().isOk());
	}

	@Test
	void testAmbiguousRoute() throws Exception {
		this.mockMvc.perform(get("/owners/ambiguous")).andExpect(status().isOk());
	}
}
