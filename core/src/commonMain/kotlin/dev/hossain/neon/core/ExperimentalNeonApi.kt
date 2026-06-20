package dev.hossain.neon.core

@RequiresOptIn(
    message =
        "This API is part of the experimental Neon surface and may change " +
            "or be removed in future releases without a deprecation cycle.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
)
public annotation class ExperimentalNeonApi
