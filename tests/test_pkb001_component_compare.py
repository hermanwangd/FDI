from collections.abc import Sequence
from copy import deepcopy

import pytest

from tooling.validation.pkb001_component_compare import compare_components


def component(path="src/OwnerController.java", containing_type="OwnerController", symbol="OwnerController.process"):
    return {
        "source_path": path,
        "containing_type": containing_type,
        "qualified_symbol": symbol,
    }


def metric(matched, expected, proposed):
    return {
        "matched": matched,
        "expected": expected,
        "proposed": proposed,
        "recall": matched / expected if expected else 1.0,
        "precision": matched / proposed if proposed else 1.0,
    }


def test_class_does_not_substitute_for_method_at_exact_level():
    proposed = [component(symbol="OwnerController") | {"role": "PRIMARY"}]
    expected = [component(symbol="OwnerController.processFindForm")]

    result = compare_components(proposed, expected)

    assert result == {
        "path": metric(1, 1, 1),
        "type": metric(1, 1, 1),
        "exact_symbol": metric(0, 1, 1),
        "expected_realization_chain_coverage": 0.0,
        "extra_proposed_components": [component(symbol="OwnerController")],
        "missing_expected_components": [
            component(symbol="OwnerController.processFindForm")
        ],
        "supporting_expected_symbols_cited": {"count": 0, "symbols": []},
    }


def test_supporting_evidence_does_not_increase_exact_match_or_chain_coverage():
    expected_method = component(symbol="OwnerController.processFindForm")

    result = compare_components([], [expected_method], supporting=[expected_method])

    assert result["exact_symbol"] == metric(0, 1, 0)
    assert result["expected_realization_chain_coverage"] == 0.0
    assert result["supporting_expected_symbols_cited"] == {
        "count": 1,
        "symbols": ["OwnerController.processFindForm"],
    }


def test_exact_method_match_populates_every_level_without_missing_or_extra():
    method = component(symbol="OwnerController.processFindForm")

    result = compare_components([method | {"role": "PRIMARY"}], [method])

    assert result["path"] == metric(1, 1, 1)
    assert result["type"] == metric(1, 1, 1)
    assert result["exact_symbol"] == metric(1, 1, 1)
    assert result["expected_realization_chain_coverage"] == 1.0
    assert result["extra_proposed_components"] == []
    assert result["missing_expected_components"] == []


def test_exact_symbol_match_is_independent_of_path_and_type():
    proposed = component("src/a/OwnerController.java", "FirstOwner")
    expected = component("src/b/OwnerController.java", "SecondOwner")

    result = compare_components([proposed], [expected])

    assert result["path"] == metric(0, 1, 1)
    assert result["type"] == metric(0, 1, 1)
    assert result["exact_symbol"] == metric(1, 1, 1)
    assert result["expected_realization_chain_coverage"] == 1.0
    assert result["extra_proposed_components"] == [proposed]
    assert result["missing_expected_components"] == [expected]


def test_supporting_symbol_citation_is_independent_of_path_and_type():
    expected = component("src/expected.py", "Expected", "Shared.run")
    supporting = component("src/support.py", "Support", "Shared.run")

    result = compare_components([], [expected], [supporting])

    assert result["exact_symbol"] == metric(0, 1, 0)
    assert result["expected_realization_chain_coverage"] == 0.0
    assert result["supporting_expected_symbols_cited"] == {
        "count": 1,
        "symbols": ["Shared.run"],
    }


def test_multiple_components_compute_recall_and_precision_independently():
    shared = component("src/shared.py", "Shared", "Shared.run")
    proposed = [shared, component("src/extra.py", "Shared", "Extra.run")]
    expected = [shared, component("src/missing.py", "Missing", "Missing.run")]

    result = compare_components(proposed, expected)

    assert result["path"] == metric(1, 2, 2)
    assert result["type"] == metric(1, 2, 1)
    assert result["exact_symbol"] == metric(1, 2, 2)
    assert result["expected_realization_chain_coverage"] == 0.5


@pytest.mark.parametrize(
    "collection",
    [
        None,
        "not rows",
        b"not rows",
        {"source_path": "src/file.py"},
        ["not a dict"],
        [{}],
        [component(path=" ")],
        [component(path="/absolute/file.py")],
        [component(path="C:/absolute/file.py")],
        [component(path="src\\file.py")],
        [component(path="src/../file.py")],
        [component(path="src/./file.py")],
        [component(path="src//file.py")],
        [component(path="src/file.py/")],
        [component(containing_type="")],
        [component(symbol=1)],
    ],
)
def test_invalid_missing_or_noncanonical_input_fails_closed(collection):
    with pytest.raises(ValueError):
        compare_components(collection, [])


@pytest.mark.parametrize("argument", ["proposed", "expected", "supporting"])
def test_duplicate_composite_identity_within_each_collection_fails_closed(argument):
    row = component()
    values = {"proposed": [], "expected": [], "supporting": []}
    values[argument] = [row, dict(row)]

    with pytest.raises(ValueError):
        compare_components(**values)


class ChangingSequence(Sequence):
    def __init__(self, rows):
        self.rows = rows
        self.iterations = 0

    def __len__(self):
        return len(self.rows)

    def __getitem__(self, index):
        return self.rows[index]

    def __iter__(self):
        self.iterations += 1
        if self.iterations > 1:
            raise AssertionError("collection was iterated more than once")
        return iter(tuple(self.rows))


class OneShotIterable:
    def __init__(self, rows):
        self.rows = rows
        self.iterations = 0

    def __iter__(self):
        self.iterations += 1
        if self.iterations > 1:
            raise AssertionError("one-shot iterable was consumed more than once")
        return iter(tuple(self.rows))


def test_sequence_is_snapshotted_once_before_validation_and_comparison():
    row = component()
    proposed = ChangingSequence([row])

    result = compare_components(proposed, [row])

    assert result["exact_symbol"]["matched"] == 1
    assert proposed.iterations == 1


def test_generator_input_is_accepted_and_snapshotted():
    row = component()

    result = compare_components((item for item in [row]), (item for item in [row]))

    assert result["exact_symbol"] == metric(1, 1, 1)


def test_one_shot_iterable_is_consumed_exactly_once():
    row = component()
    proposed = OneShotIterable([row])

    result = compare_components(proposed, [row])

    assert result["exact_symbol"]["matched"] == 1
    assert proposed.iterations == 1


def test_calls_and_input_order_are_deterministic_and_do_not_mutate_inputs():
    first = component("src/z.py", "Z", "Z.run") | {"role": "PRIMARY"}
    second = component("src/a.py", "A", "A.run")
    proposed = [first, second]
    expected = [component("src/m.py", "M", "M.run"), second]
    supporting = [component("src/m.py", "M", "M.run")]
    original = deepcopy((proposed, expected, supporting))

    one = compare_components(proposed, expected, supporting)
    two = compare_components(list(reversed(proposed)), list(reversed(expected)), supporting)
    three = compare_components(proposed, expected, supporting)

    assert one == two == three
    assert (proposed, expected, supporting) == original
