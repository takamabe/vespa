# Copyright 2018 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
find_package(GTest REQUIRED)
vespa_add_executable(searchsummary_attribute_combiner_test_app TEST
    SOURCES
    attribute_combiner_test.cpp
    DEPENDS
    searchsummary
    GTest::GTest
)
vespa_add_test(NAME searchsummary_attribute_combiner_test_app COMMAND searchsummary_attribute_combiner_test_app)
