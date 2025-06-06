// "Opt in for 'MyOptIn' on 'SamI'" "true"
// PRIORITY: HIGH
// RUNTIME_WITH_SCRIPT_RUNTIME
// ACTION: Opt in for 'MyOptIn' in containing file 'samWithAnnotation.kts'
// ACTION: Opt in for 'MyOptIn' in module 'light_idea_test_case'
// ACTION: Opt in for 'MyOptIn' on 'SamI'
// ACTION: Opt in for 'MyOptIn' on statement

@RequiresOptIn
annotation class MyOptIn

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Bar

@MyOptIn
fun foo() {
}

fun interface SamI {
    fun run()
}

@Bar
SamI {
    foo<caret>()
}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.OptInFixes$UseOptInAnnotationFix