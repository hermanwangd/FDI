/*
 * Synthetic Spring fixture for SF-BL-002 Task 1 route-aware contracts.
 *
 * This source is a deterministic parse input for the HTTP behavior extractor
 * and the Spring route-handler index. It is intentionally kept under
 * src/test/resources and is never compiled or executed.
 */
package scenarioforward.sfbl002.routeaware;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/owners")
public class RouteFixtureController {

	@GetMapping
	List<Owner> listOwners(@RequestParam(name = "page", defaultValue = "1") int page) {
		return List.of();
	}

	@GetMapping("/{ownerId}")
	Owner showOwner(@PathVariable("ownerId") int ownerId) {
		return new Owner(ownerId, "Franklin");
	}

	@PostMapping
	ResponseEntity<Owner> createOwner(@RequestBody Owner owner) {
		if (owner.name() == null || owner.name().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		return ResponseEntity.ok(new Owner(1, owner.name()));
	}

	@GetMapping("/{ownerId}/pets/{petId}")
	Pet showPet(@PathVariable("ownerId") int ownerId, @PathVariable("petId") int petId) {
		return new Pet(petId, "Leo");
	}

	record Owner(int id, String name) {
	}

	record Pet(int id, String name) {
	}
}

/**
 * Synthetic ambiguity probe: the same method and route are declared on two
 * controllers, so an exact route-to-handler binding must stay AMBIGUOUS and
 * never fall back to a guessed handler.
 */
@RestController
class AmbiguousFixtureController {

	@GetMapping("/owners/ambiguous")
	String firstAmbiguousMapping() {
		return "first";
	}
}

@RestController
class AmbiguousFixtureAltController {

	@GetMapping("/owners/ambiguous")
	String secondAmbiguousMapping() {
		return "second";
	}
}
