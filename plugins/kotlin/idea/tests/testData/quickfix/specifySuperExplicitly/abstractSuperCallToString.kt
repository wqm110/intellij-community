// "Generate 'toString()'" "true"
// WITH_STDLIB

interface I

abstract class A {
    abstract override fun toString(): String
}

class B : A(), I {

    override fun toString(): String {
        return super.<caret>toString()
    }
}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.AbstractSuperCallFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.fixes.UpdateToCorrectMethodFix