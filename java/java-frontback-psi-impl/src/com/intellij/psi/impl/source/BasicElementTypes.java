// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.impl.source;

import com.intellij.java.syntax.element.SyntaxElementTypes;
import com.intellij.psi.JavaDocTokenType;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.tree.ParentAwareTokenSet;
import com.intellij.psi.tree.TokenSet;

/**
 * The BasicElementTypes interface represents a collection of basic element types used in frontback java modules.
 * {@link TokenSet} is used for set, which contains `basic` types, which are not used in hierarchy.
 * for other sets {@link ParentAwareTokenSet} is used.
 * This set of sets must be consistent with {@link com.intellij.psi.impl.source.tree.ElementType}.
 * It's checked with {@link com.intellij.java.parser.FrontBackElementTypeTest#testJavaTokenSet()}
 *
 * @see ParentAwareTokenSet
 * @see SyntaxElementTypes
 */
@SuppressWarnings("unused")
public interface BasicElementTypes extends JavaTokenType, JavaDocTokenType, BasicJavaElementType, BasicJavaDocElementType {

  TokenSet BASIC_JAVA_PLAIN_COMMENT_BIT_SET = TokenSet.create(END_OF_LINE_COMMENT, C_STYLE_COMMENT);
  ParentAwareTokenSet BASIC_JAVA_COMMENT_BIT_SET =
    ParentAwareTokenSet.orSet(ParentAwareTokenSet.create(BASIC_JAVA_PLAIN_COMMENT_BIT_SET), ParentAwareTokenSet.create(BASIC_DOC_COMMENT));
  ParentAwareTokenSet BASIC_JAVA_COMMENT_OR_WHITESPACE_BIT_SET =
    ParentAwareTokenSet.orSet(ParentAwareTokenSet.create(WHITE_SPACE), BASIC_JAVA_COMMENT_BIT_SET);
  TokenSet BASIC_KEYWORD_BIT_SET = TokenSet.create(
    ABSTRACT_KEYWORD, ASSERT_KEYWORD, BOOLEAN_KEYWORD, BREAK_KEYWORD, BYTE_KEYWORD, CASE_KEYWORD, CATCH_KEYWORD, CHAR_KEYWORD,
    CLASS_KEYWORD, CONST_KEYWORD, CONTINUE_KEYWORD, DEFAULT_KEYWORD, DO_KEYWORD, DOUBLE_KEYWORD, ELSE_KEYWORD, ENUM_KEYWORD,
    EXTENDS_KEYWORD, FINAL_KEYWORD, FINALLY_KEYWORD, FLOAT_KEYWORD, FOR_KEYWORD, GOTO_KEYWORD, IF_KEYWORD, IMPLEMENTS_KEYWORD,
    IMPORT_KEYWORD, INSTANCEOF_KEYWORD, INT_KEYWORD, INTERFACE_KEYWORD, LONG_KEYWORD, NATIVE_KEYWORD, NEW_KEYWORD, PACKAGE_KEYWORD,
    PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD, RETURN_KEYWORD, SHORT_KEYWORD, SUPER_KEYWORD, STATIC_KEYWORD, STRICTFP_KEYWORD,
    SWITCH_KEYWORD, SYNCHRONIZED_KEYWORD, THIS_KEYWORD, THROW_KEYWORD, THROWS_KEYWORD, TRANSIENT_KEYWORD, TRY_KEYWORD, VOID_KEYWORD,
    VOLATILE_KEYWORD, WHILE_KEYWORD,
    OPEN_KEYWORD, MODULE_KEYWORD, REQUIRES_KEYWORD, EXPORTS_KEYWORD, OPENS_KEYWORD, USES_KEYWORD, PROVIDES_KEYWORD,
    TRANSITIVE_KEYWORD, TO_KEYWORD, WITH_KEYWORD, VAR_KEYWORD, YIELD_KEYWORD, RECORD_KEYWORD, SEALED_KEYWORD, PERMITS_KEYWORD,
    NON_SEALED_KEYWORD, WHEN_KEYWORD, VALUE_KEYWORD
  );

  TokenSet BASIC_LITERAL_BIT_SET = TokenSet.create(TRUE_KEYWORD, FALSE_KEYWORD, NULL_KEYWORD);

  TokenSet BASIC_OPERATION_BIT_SET = TokenSet.create(
    EQ, GT, LT, EXCL, TILDE, QUEST, COLON, PLUS, MINUS, ASTERISK, DIV, AND, OR, XOR,
    PERC, EQEQ, LE, GE, NE, ANDAND, OROR, PLUSPLUS, MINUSMINUS, LTLT, GTGT, GTGTGT,
    PLUSEQ, MINUSEQ, ASTERISKEQ, DIVEQ, ANDEQ, OREQ, XOREQ, PERCEQ, LTLTEQ, GTGTEQ, GTGTGTEQ);

  TokenSet BASIC_MODIFIER_BIT_SET = TokenSet.create(
    PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD, STATIC_KEYWORD, ABSTRACT_KEYWORD, FINAL_KEYWORD, NATIVE_KEYWORD,
    SYNCHRONIZED_KEYWORD, STRICTFP_KEYWORD, TRANSIENT_KEYWORD, VOLATILE_KEYWORD, DEFAULT_KEYWORD, SEALED_KEYWORD, NON_SEALED_KEYWORD,
    VALUE_KEYWORD);

  TokenSet BASIC_PRIMITIVE_TYPE_BIT_SET = TokenSet.create(
    BOOLEAN_KEYWORD, BYTE_KEYWORD, SHORT_KEYWORD, INT_KEYWORD, LONG_KEYWORD, CHAR_KEYWORD, FLOAT_KEYWORD, DOUBLE_KEYWORD, VOID_KEYWORD);

  ParentAwareTokenSet BASIC_EXPRESSION_BIT_SET = ParentAwareTokenSet.create(
    BASIC_REFERENCE_EXPRESSION, BASIC_LITERAL_EXPRESSION, BASIC_THIS_EXPRESSION, BASIC_SUPER_EXPRESSION,
    BASIC_PARENTH_EXPRESSION, BASIC_METHOD_CALL_EXPRESSION,
    BASIC_TYPE_CAST_EXPRESSION, BASIC_PREFIX_EXPRESSION, BASIC_POSTFIX_EXPRESSION, BASIC_BINARY_EXPRESSION,
    BASIC_POLYADIC_EXPRESSION, BASIC_CONDITIONAL_EXPRESSION,
    BASIC_ASSIGNMENT_EXPRESSION, BASIC_NEW_EXPRESSION, BASIC_ARRAY_ACCESS_EXPRESSION,
    BASIC_ARRAY_INITIALIZER_EXPRESSION, BASIC_INSTANCE_OF_EXPRESSION,
    BASIC_CLASS_OBJECT_ACCESS_EXPRESSION, BASIC_METHOD_REF_EXPRESSION, BASIC_LAMBDA_EXPRESSION, BASIC_SWITCH_EXPRESSION,
    BASIC_EMPTY_EXPRESSION,
    BASIC_TEMPLATE_EXPRESSION);

  ParentAwareTokenSet BASIC_ANNOTATION_MEMBER_VALUE_BIT_SET =
    ParentAwareTokenSet.orSet(BASIC_EXPRESSION_BIT_SET, ParentAwareTokenSet.create(BASIC_ANNOTATION,
                                                                                   BASIC_ANNOTATION_ARRAY_INITIALIZER));

  ParentAwareTokenSet BASIC_ARRAY_DIMENSION_BIT_SET = ParentAwareTokenSet.create(
    BASIC_REFERENCE_EXPRESSION, BASIC_LITERAL_EXPRESSION, BASIC_THIS_EXPRESSION, BASIC_SUPER_EXPRESSION,
    BASIC_PARENTH_EXPRESSION, BASIC_METHOD_CALL_EXPRESSION,
    BASIC_TYPE_CAST_EXPRESSION, BASIC_PREFIX_EXPRESSION, BASIC_POSTFIX_EXPRESSION, BASIC_BINARY_EXPRESSION,
    BASIC_POLYADIC_EXPRESSION, BASIC_CONDITIONAL_EXPRESSION,
    BASIC_ASSIGNMENT_EXPRESSION, BASIC_NEW_EXPRESSION, BASIC_ARRAY_ACCESS_EXPRESSION, BASIC_INSTANCE_OF_EXPRESSION,
    BASIC_CLASS_OBJECT_ACCESS_EXPRESSION,
    BASIC_EMPTY_EXPRESSION);

  ParentAwareTokenSet BASIC_JAVA_STATEMENT_BIT_SET = ParentAwareTokenSet.create(
    BASIC_EMPTY_STATEMENT, BASIC_BLOCK_STATEMENT, BASIC_EXPRESSION_STATEMENT, BASIC_EXPRESSION_LIST_STATEMENT,
    BASIC_DECLARATION_STATEMENT, BASIC_IF_STATEMENT,
    BASIC_WHILE_STATEMENT, BASIC_FOR_STATEMENT, BASIC_FOREACH_STATEMENT, BASIC_DO_WHILE_STATEMENT,
    BASIC_SWITCH_STATEMENT, BASIC_SWITCH_LABEL_STATEMENT, BASIC_BREAK_STATEMENT,
    BASIC_CONTINUE_STATEMENT, BASIC_RETURN_STATEMENT, BASIC_THROW_STATEMENT, BASIC_SYNCHRONIZED_STATEMENT,
    BASIC_TRY_STATEMENT, BASIC_LABELED_STATEMENT, BASIC_ASSERT_STATEMENT,
    BASIC_YIELD_STATEMENT);

  ParentAwareTokenSet BASIC_JAVA_PATTERN_BIT_SET =
    ParentAwareTokenSet.create(BASIC_TYPE_TEST_PATTERN, BASIC_DECONSTRUCTION_PATTERN, BASIC_UNNAMED_PATTERN);

  ParentAwareTokenSet BASIC_JAVA_CASE_LABEL_ELEMENT_BIT_SET =
    ParentAwareTokenSet.orSet(BASIC_JAVA_PATTERN_BIT_SET, BASIC_EXPRESSION_BIT_SET, ParentAwareTokenSet.create(
      BASIC_DEFAULT_CASE_LABEL_ELEMENT));

  ParentAwareTokenSet BASIC_JAVA_MODULE_STATEMENT_BIT_SET = ParentAwareTokenSet.create(
    BASIC_REQUIRES_STATEMENT, BASIC_EXPORTS_STATEMENT, BASIC_OPENS_STATEMENT, BASIC_USES_STATEMENT,
    BASIC_PROVIDES_STATEMENT);

  ParentAwareTokenSet BASIC_IMPORT_STATEMENT_BASE_BIT_SET = ParentAwareTokenSet.create(BASIC_IMPORT_STATEMENT,
                                                                                       BASIC_IMPORT_STATIC_STATEMENT,
                                                                                       BASIC_IMPORT_MODULE_STATEMENT);
  TokenSet BASIC_CLASS_KEYWORD_BIT_SET =
    TokenSet.create(CLASS_KEYWORD, INTERFACE_KEYWORD, ENUM_KEYWORD, RECORD_KEYWORD);
  ParentAwareTokenSet BASIC_MEMBER_BIT_SET = ParentAwareTokenSet.create(BASIC_CLASS, BASIC_FIELD, BASIC_ENUM_CONSTANT,
                                                                        BASIC_METHOD, BASIC_ANNOTATION_METHOD);
  ParentAwareTokenSet BASIC_FULL_MEMBER_BIT_SET = ParentAwareTokenSet.orSet(BASIC_MEMBER_BIT_SET, ParentAwareTokenSet.create(
    BASIC_CLASS_INITIALIZER));

  TokenSet BASIC_INTEGER_LITERALS = TokenSet.create(INTEGER_LITERAL, LONG_LITERAL);
  TokenSet BASIC_REAL_LITERALS = TokenSet.create(FLOAT_LITERAL, DOUBLE_LITERAL);
  TokenSet BASIC_STRING_LITERALS = TokenSet.create(STRING_LITERAL, TEXT_BLOCK_LITERAL);
  TokenSet BASIC_TEXT_LITERALS = TokenSet.create(STRING_LITERAL, TEXT_BLOCK_LITERAL, CHARACTER_LITERAL);

  TokenSet BASIC_STRING_TEMPLATE_FRAGMENTS =
    TokenSet.create(STRING_TEMPLATE_BEGIN, STRING_TEMPLATE_MID, STRING_TEMPLATE_END,
                    TEXT_BLOCK_TEMPLATE_BEGIN, TEXT_BLOCK_TEMPLATE_MID, TEXT_BLOCK_TEMPLATE_END);

  TokenSet BASIC_ALL_LITERALS =
    TokenSet.orSet(BASIC_INTEGER_LITERALS, BASIC_REAL_LITERALS, BASIC_TEXT_LITERALS, BASIC_LITERAL_BIT_SET);
}
