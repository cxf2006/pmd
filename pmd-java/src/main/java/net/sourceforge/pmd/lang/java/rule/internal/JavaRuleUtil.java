/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.rule.internal;

import static net.sourceforge.pmd.lang.java.types.JPrimitiveType.PrimitiveTypeKind.LONG;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamField;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.lang.ast.NodeStream;
import net.sourceforge.pmd.lang.java.ast.ASTArgumentList;
import net.sourceforge.pmd.lang.java.ast.ASTAssignableExpr.ASTNamedReferenceExpr;
import net.sourceforge.pmd.lang.java.ast.ASTAssignableExpr.AccessType;
import net.sourceforge.pmd.lang.java.ast.ASTAssignmentExpression;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceType;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTForStatement;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameter;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameters;
import net.sourceforge.pmd.lang.java.ast.ASTIfStatement;
import net.sourceforge.pmd.lang.java.ast.ASTList;
import net.sourceforge.pmd.lang.java.ast.ASTLocalVariableDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodOrConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTStatement;
import net.sourceforge.pmd.lang.java.ast.ASTUnaryExpression;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.java.ast.AccessNode.Visibility;
import net.sourceforge.pmd.lang.java.ast.JModifier;
import net.sourceforge.pmd.lang.java.ast.TypeNode;
import net.sourceforge.pmd.lang.java.types.TypeTestUtil;
import net.sourceforge.pmd.util.CollectionUtil;

/**
 * Utilities for rules to query the AST.
 */
public final class JavaRuleUtil {

    private JavaRuleUtil() {
        // utility class
    }


    /**
     * Returns true if the formal parameters of the method or constructor
     * match the given types exactly. Note that for varargs methods, the
     * last param must have an array type (but it is not checked to be varargs).
     * This will return false if we're not sure.
     *
     * @param node  Method or ctor
     * @param types List of types to match (may be empty)
     *
     * @throws NullPointerException If any of the classes is null, or the node is null
     * @see TypeTestUtil#isExactlyA(Class, TypeNode)
     */
    public static boolean hasParameters(ASTMethodOrConstructorDeclaration node, Class<?>... types) {
        ASTFormalParameters formals = node.getFormalParameters();
        if (formals.size() != types.length) {
            return false;
        }
        for (int i = 0; i < formals.size(); i++) {
            ASTFormalParameter fi = formals.get(i);
            if (!TypeTestUtil.isExactlyA(types[i], fi)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the {@code throws} declaration of the method or constructor
     * matches the given types exactly.
     *
     * @param node  Method or ctor
     * @param types List of exception types to match (may be empty)
     *
     * @throws NullPointerException If any of the classes is null, or the node is null
     * @see TypeTestUtil#isExactlyA(Class, TypeNode)
     */
    @SafeVarargs
    public static boolean hasExceptionList(ASTMethodOrConstructorDeclaration node, Class<? extends Throwable>... types) {
        @NonNull List<ASTClassOrInterfaceType> formals = ASTList.orEmpty(node.getThrowsList());
        if (formals.size() != types.length) {
            return false;
        }
        for (int i = 0; i < formals.size(); i++) {
            ASTClassOrInterfaceType fi = formals.get(i);
            if (!TypeTestUtil.isExactlyA(types[i], fi)) {
                return false;
            }
        }
        return true;
    }

    /**
     * True if the variable is never used. Note that the visibility of
     * the variable must be less than {@link Visibility#V_PRIVATE} for
     * us to be sure of it.
     */
    public static boolean isNeverUsed(ASTVariableDeclaratorId varId) {
        return CollectionUtil.none(varId.getUsages(), JavaRuleUtil::isReadUsage);
    }

    private static boolean isReadUsage(ASTNamedReferenceExpr expr) {
        return expr.getAccessType() == AccessType.READ
            // foo(x++)
            || expr.getParent() instanceof ASTUnaryExpression
            && expr.getParent().getParent() instanceof ASTArgumentList;
    }

    /**
     * True if the variable is incremented or decremented via a compound
     * assignment operator, or a unary increment/decrement expression.
     */
    public static boolean isVarAccessReadAndWrite(ASTNamedReferenceExpr expr) {
        return expr.getAccessType() == AccessType.WRITE
            && (!(expr.getParent() instanceof ASTAssignmentExpression)
            || ((ASTAssignmentExpression) expr.getParent()).getOperator().isCompound());
    }

    /**
     * True if the variable is incremented or decremented via a compound
     * assignment operator, or a unary increment/decrement expression.
     */
    public static boolean isInIfCondition(ASTExpression expr) {
        ASTExpression toplevel = getTopLevelExpr(expr);
        return toplevel.getIndexInParent() == 0 && toplevel.getParent() instanceof ASTIfStatement;
    }

    public static @Nullable ASTIfStatement getIfStmtIfExprInCondition(ASTExpression expr) {
        ASTExpression toplevel = getTopLevelExpr(expr);
        if (toplevel.getIndexInParent() == 0 && toplevel.getParent() instanceof ASTIfStatement) {
            return (ASTIfStatement) toplevel.getParent();
        }
        return null;
    }

    /**
     * Will cut through argument lists, except those of enum constants & explicit invocation nodes.
     */
    private static @NonNull ASTExpression getTopLevelExpr(ASTExpression expr) {
        return (ASTExpression) expr.ancestorsOrSelf()
                                   .takeWhile(it -> it instanceof ASTExpression
                                       || it instanceof ASTArgumentList && it.getParent() instanceof ASTExpression)
                                   .last();
    }

    public static NodeStream<ASTVariableDeclaratorId> getLoopVariables(ASTForStatement loop) {
        @Nullable ASTStatement init = loop.getInit();

        if (init instanceof ASTLocalVariableDeclaration) {
            return ((ASTLocalVariableDeclaration) init).getVarIds();
        }

        return NodeStream.empty();
    }

    // TODO at least UnusedPrivateMethod has some serialization-related logic.

    /**
     * Whether some variable declared by the given node is a serialPersistentFields
     * (serialization-specific field).
     */
    public static boolean isSerialPersistentFields(final ASTFieldDeclaration field) {
        return field.hasModifiers(JModifier.FINAL, JModifier.STATIC, JModifier.PRIVATE)
            && field.getVarIds().any(
            it -> "serialPersistentFields".equals(it.getName())
                && TypeTestUtil.isA(ObjectStreamField[].class, it)
        );
    }

    /**
     * Whether some variable declared by the given node is a serialVersionUID
     * (serialization-specific field).
     */
    public static boolean isSerialVersionUID(ASTFieldDeclaration field) {
        return field.hasModifiers(JModifier.FINAL, JModifier.STATIC)
            && field.getVarIds().any(
            it -> "serialVersionUID".equals(it.getName())
                && it.getTypeMirror().isPrimitive(LONG)
        );
    }

    /**
     * True if the method is a {@code readObject} method defined for serialization.
     */
    public static boolean isSerializationReadObject(ASTMethodDeclaration node) {
        return node.getVisibility() == Visibility.V_PRIVATE
            && "readObject".equals(node.getName())
            && hasExceptionList(node, InvalidObjectException.class)
            && hasParameters(node, ObjectInputStream.class);
    }
}
